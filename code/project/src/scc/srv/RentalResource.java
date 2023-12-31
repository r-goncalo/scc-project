package scc.srv;

import com.azure.cosmos.util.CosmosPagedIterable;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import redis.clients.jedis.Jedis;
import scc.cache.RedisCache;
import scc.data.AvailabityDao;
import scc.data.HouseDao;
import scc.data.Rental;
import scc.data.RentalDao;
import scc.db.CosmosDBLayer;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;


//todo restrições (caso não haja casa ou user, não pode criar rental. Caso já haja rental, não se pode criar outro)

@Path("/house/{houseId}/rental") //todo checkar se valores entre {} são iguais aos do @pathparam
public class RentalResource {

    private static final long MAX_RECENTE_RENTALS_IN_CACHE = 5;
    private static final String MOST_RECENT_RENTALS_REDIS_KEY = "MostRecentRentals";
    private static final String NUM_RECENT_RENTALS = "NumRecentRentals";
    public static final String RENTALS_REDIS_KEY = "Rentals";
    public static final String NUM_RENTALS = "NumRentals";

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public static Rental newRental(@CookieParam("scc:session") Cookie session, @PathParam("houseId") String houseId , Rental rental) {
        LogResource.writeLine("    new rental:"+rental.toString());
        Locale.setDefault(Locale.US);
        CosmosDBLayer db = CosmosDBLayer.getInstance();

        //confirm rental's houseid
        if(!houseId.equals(rental.getHouseId())){
            LogResource.writeLine("    houseid does not match");
            throw new NotFoundException("House not found");
        }

        // confirm rental's house exists
        Iterator<HouseDao> h = db.getHouseById(rental.getHouseId()).iterator();
        if(h.hasNext() == false) {
            LogResource.writeLine("    house not found");
            throw new WebApplicationException("House not found", Response.Status.NOT_FOUND);
        }

        // confirm rental's date is not taken. use db.gethouserentalfordate
        if(db.getHouseRentalForDate(rental.getPeriod(),rental.getHouseId()).iterator().hasNext()) { //todo possivelmente mal
            LogResource.writeLine("    rental date is already taken for the given house");
            throw new WebApplicationException("Rental date is already taken for the given house", Response.Status.CONFLICT);
        }
        // confirm rental's user exists
        if(db.getUserById(rental.getRenterId()).iterator().hasNext()==false) {
            LogResource.writeLine("    user not found");
            throw new WebApplicationException("User not found", Response.Status.NOT_FOUND);
        }

        // check if renter is not the house owner
        if(db.getHouseById(rental.getHouseId()).iterator().next().getOwnerId().equals(rental.getRenterId())) {
            LogResource.writeLine("    renter cannot be the owner of the house");
            throw new WebApplicationException("Renter cannot be the owner of the house", Response.Status.CONFLICT);
        }


        //check if user is logged in
        boolean isOwnerLoggedIn = RedisCache.isSessionOfUser(session, rental.getRenterId());
        if(isOwnerLoggedIn == false) {
            LogResource.writeLine("    renter not logged in");
            throw new WebApplicationException("Renter not logged in", Response.Status.UNAUTHORIZED);
        }


        // find if rental date is inside any availability period
        CosmosPagedIterable<AvailabityDao> availabilities = db.getAvailabilitiesForHouse(rental.getHouseId());
        //get house
        CosmosPagedIterable<HouseDao> house = db.getHouseById(rental.getHouseId());
        boolean isInsideAvailability = false;
        double price = 0;
        boolean isDiscounted = false;
        for (AvailabityDao a : availabilities) {
            String startDate = a.getFromDate(),
                    endDate = a.getToDate();
            if (rental.getPeriod().compareTo(startDate) >= 0 && rental.getPeriod().compareTo(endDate) <= 0) {
                isInsideAvailability = true;
                String discountMonth= house.iterator().next().getDiscountMonth();
                if (discountMonth != null)
                    isDiscounted = discountMonth.equals(rental.getPeriod());
                price = a.getCost() * (isDiscounted ? a.getDiscount() : 1);
                break;
            }
        }
        if(!isInsideAvailability) {
            LogResource.writeLine("    rental date is not inside any availability period");
            throw new WebApplicationException("Rental date is not inside any availability period", Response.Status.CONFLICT);
        }

        //set values
        rental.setId(UUID.randomUUID().toString());
        rental.setPrice(price); //todo fazer para o discounted price
        RentalDao r = new RentalDao(rental);

        //put in db
        db.putRental(r);

        //put in cache
        try (Jedis jedis = RedisCache.getCachePool().getResource()) {

            ObjectMapper mapper = new ObjectMapper();

            jedis.set("rental:"+rental.getId(), mapper.writeValueAsString(rental));

            Long cnt = jedis.lpush(MOST_RECENT_RENTALS_REDIS_KEY, mapper.writeValueAsString(rental));

            if (cnt > MAX_RECENTE_RENTALS_IN_CACHE)
                jedis.ltrim(MOST_RECENT_RENTALS_REDIS_KEY, 0, MAX_RECENTE_RENTALS_IN_CACHE - 1);
            if (cnt < MAX_RECENTE_RENTALS_IN_CACHE)
                jedis.incr(NUM_RECENT_RENTALS);

            jedis.lpush(RENTALS_REDIS_KEY, mapper.writeValueAsString(rental));
            jedis.incr(NUM_RENTALS);

        } catch(Exception e) {
            e.printStackTrace();
        }



        return rental;
    }

    //update rental
    @PUT
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public static Rental updateRental(@PathParam("rentalId") String rentalId, @PathParam("houseID") String houseId,@CookieParam("scc:session") Cookie session, Rental rental ) throws ParseException {
        CosmosDBLayer db = CosmosDBLayer.getInstance();

        //check if rentalids match
        if(!rental.getId().equals(rentalId))
            throw new NotFoundException("Rental not found");

        //check if rental exists
        if(db.getrentalbyidandhouse(houseId, rental.getId()).iterator().hasNext() == false)
            throw new NotFoundException("Rental not found");

        //check if rental Date is not taken
        if(db.getHouseRentalForDate(rental.getPeriod(),rental.getHouseId()).iterator().hasNext()) { //todo possivelmente mal
            LogResource.writeLine("    rental date is already taken for the given house");
            throw new WebApplicationException("Rental date is already taken for the given house", Response.Status.CONFLICT);
        }
        //check if house id exists
        if(db.getHouseById(houseId).iterator().hasNext() == false)
            throw new NotFoundException("House not found");

        //check if user is logged in
        boolean isOwnerLoggedIn = RedisCache.isSessionOfUser(session, rental.getRenterId());
        if(isOwnerLoggedIn == false)
            throw new WebApplicationException("Renter not logged in", Response.Status.UNAUTHORIZED);

        //check if rental date is valid
        CosmosPagedIterable<AvailabityDao> availabilities = db.getAvailabilitiesForHouse(rental.getHouseId());

        SimpleDateFormat sdf = new SimpleDateFormat("MM-yyyy");
        Date rentalDate = sdf.parse(rental.getPeriod());

        boolean isInsideAvailability = false;
        double price = 0;
        for (AvailabityDao a : availabilities) {
            //check if is between any availability period
            String startDate = a.getFromDate(),
                    endDate = a.getToDate();
            if (rental.getPeriod().compareTo(startDate) >= 0 && rental.getPeriod().compareTo(endDate) <= 0) {
                isInsideAvailability = true;
                price = a.getCost();
                break;
            }
        }
        if(!isInsideAvailability) {
            LogResource.writeLine("    rental date is not inside any availability period");
            throw new WebApplicationException("Rental date is not inside any availability period", Response.Status.CONFLICT);
        }

        //update rental
        rental.setPrice(price);
        RentalDao r = new RentalDao(rental);
        db.putRental(r);

        //update cache
        try (Jedis jedis = RedisCache.getCachePool().getResource()) {
            ObjectMapper mapper = new ObjectMapper();
            jedis.set("rental:"+rental.getId(), mapper.writeValueAsString(rental));
        } catch (Exception e){
            e.printStackTrace();
        }

        return rental;
    }

    //get a specific rental for a given houseid
    @GET
    @Path("/{rentalId}")
    @Produces(MediaType.APPLICATION_JSON)
    public static Rental getRental(@CookieParam("scc:session") Cookie session ,@PathParam("houseId") String houseId, @PathParam("rentalId") String rentalId) {

        Locale.setDefault(Locale.US);
        CosmosDBLayer db = CosmosDBLayer.getInstance();

        //check if user is logged in
        boolean isOwnerLoggedIn = RedisCache.isSessionOfUser(session, rentalId);
        if(isOwnerLoggedIn == false)
            throw new WebApplicationException("Renter not logged in", Response.Status.UNAUTHORIZED);

        CosmosPagedIterable<RentalDao> rentals = db.getrentalbyidandhouse(houseId, rentalId);

        if(rentals.iterator().hasNext() == false)
            throw new NotFoundException("Rental not found");

        RentalDao r = rentals.iterator().next();

        return new Rental(r);
    }

    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public static List<Rental> getHouseRentals(@PathParam("houseId") String houseId) {

        Locale.setDefault(Locale.US);
        CosmosDBLayer db = CosmosDBLayer.getInstance();

        CosmosPagedIterable<RentalDao> rentals = db.getRentalsForHouse(houseId);

        List<Rental> ret = new ArrayList<>();

        for (RentalDao r : rentals)
            ret.add(new Rental(r));

        return ret;
    }

}