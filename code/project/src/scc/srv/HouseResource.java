package scc.srv;

import com.azure.cosmos.util.CosmosPagedIterable;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import redis.clients.jedis.Jedis;
import scc.cache.RedisCache;
import scc.data.*;
import scc.db.CosmosDBLayer;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;


@Path("/house")
public class HouseResource {


    private static final long MAX_RECENTE_HOUSES_IN_CACHE = 5;
    public static final String HOUSES_REDIS_KEY = "houses";
    public static final String MOST_RECENT_HOUSES_REDIS_KEY = "MostRecentHouses";
    public static final String NUM_HOUSES = "NumHouses";
    public static final String NUM_RECENT_HOUSES = "NumRecentHouses";

    //new house given session cookie and house object
    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public static House newHouse(@CookieParam("session") Cookie session, House house){
        LogResource.writeLine("New house: " + house);

        CosmosDBLayer db = CosmosDBLayer.getInstance();

        // check if user ownerID exists
        if(db.getUserById(house.getOwnerId()).iterator().hasNext() == false)
            throw new WebApplicationException("User" +house.getOwnerId()+" not found", Response.Status.NOT_FOUND);

        //check if owner is logged in with session cookie use the verify user method
        boolean isOwnerLoggedIn = RedisCache.isSessionOfUser(session, house.getOwnerId());
        if(isOwnerLoggedIn == false)
            throw new WebApplicationException("Owner not logged in", Response.Status.UNAUTHORIZED);

        HouseDao h = new HouseDao(house);
        UUID uniqueID = UUID.randomUUID();
        h.setId(uniqueID.toString());
        db.putHouse(h);


        //we'll save the house in cache
        try (Jedis jedis = RedisCache.getCachePool().getResource()) {

            ObjectMapper mapper = new ObjectMapper();
            house = h.toHouse();
            jedis.set("house:"+house.getId(), mapper.writeValueAsString(house));

            Long cnt = jedis.lpush(MOST_RECENT_HOUSES_REDIS_KEY, mapper.writeValueAsString(house));

            if (cnt > MAX_RECENTE_HOUSES_IN_CACHE)
                jedis.ltrim(MOST_RECENT_HOUSES_REDIS_KEY, 0, MAX_RECENTE_HOUSES_IN_CACHE - 1);
            if (cnt < MAX_RECENTE_HOUSES_IN_CACHE)
                jedis.incr(NUM_RECENT_HOUSES);

            jedis.lpush(HOUSES_REDIS_KEY, mapper.writeValueAsString(h));
            jedis.incr(NUM_HOUSES);

        } catch(Exception e) {
            e.printStackTrace();
        }

        return house;
    }



    //update house
    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public static House updateHouse(@PathParam("id") String id, @CookieParam("session") Cookie session, House house){
        CosmosDBLayer db = CosmosDBLayer.getInstance();

        //check if house exists
        if(db.getHouseById(id).iterator().hasNext())
            throw new WebApplicationException("House already exists", Response.Status.CONFLICT);

        //check if id is the same as house's id
        if(!id.equals(house.getId()))
            throw new WebApplicationException("House id does not match", Response.Status.CONFLICT);

        //check if owner is logged in
        boolean isOwnerLoggedIn = RedisCache.isSessionOfUser(session, house.getOwnerId());
        if(isOwnerLoggedIn == false)
            throw new WebApplicationException("Owner not logged in", Response.Status.UNAUTHORIZED);

        //update house
        House ret = db.updateHouse(new HouseDao(house)).toHouse();

        //update on redis
        try (Jedis jedis = RedisCache.getCachePool().getResource()) {
            ObjectMapper mapper = new ObjectMapper();
            jedis.set("house:"+house.getId(), mapper.writeValueAsString(house));
        } catch (Exception e){
            e.printStackTrace();
        }

        return ret;
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public House getHouse(@PathParam("id") String id) {

        Locale.setDefault(Locale.US);
        CosmosDBLayer db = CosmosDBLayer.getInstance();

        ObjectMapper mapper = new ObjectMapper();

        try (Jedis jedis = RedisCache.getCachePool().getResource()) {

            String res = jedis.get("user:"+id);

            if(res != null) {

                // How to convert string to object
                HouseDao uread = mapper.readValue(res, HouseDao.class);

                return uread.toHouse();

            }


        } catch (Exception e) {
            e.printStackTrace();
        }

        CosmosPagedIterable<HouseDao> dbUser = db.getHouseById(id);

        HouseDao houseDao = dbUser.iterator().next();

        return houseDao.toHouse();
    }

    /**
     * Lists the ids of images stored.
     */
    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public List<House> listHouses() {
        CosmosDBLayer db = CosmosDBLayer.getInstance();
        List<House> toReturn = new ArrayList<>();

        //get all the houses from the redis
        try{
            ObjectMapper mapper = new ObjectMapper();
            try (Jedis jedis = RedisCache.getCachePool().getResource()) {
                List<String> lst = jedis.lrange(HOUSES_REDIS_KEY, 0, -1);
                for( String s : lst){
                    House h = mapper.readValue(s, House.class);
                    toReturn.add(h);
                }
            }
        } catch (Exception e){
            LogResource.writeLine("Error getting houses from cache");
            e.printStackTrace();
        }

        if(toReturn.isEmpty()) {

            Iterable<HouseDao> houses = CosmosDBLayer.getInstance().getAllHouses();

            for (HouseDao house : houses) {
                toReturn.add(house.toHouse());
            }
        }
        return toReturn;
    }

    @DELETE
    @Path("/house/{id}")
    public Response deleteHouse(@PathParam("id") String id, @CookieParam("session") Cookie session) {

        Locale.setDefault(Locale.US);
        CosmosDBLayer db = CosmosDBLayer.getInstance();


        //check if house exists
        Iterator<HouseDao> iterator = db.getHouseById(id).iterator();
        if(iterator.hasNext())
            throw new WebApplicationException("House already exists", Response.Status.CONFLICT);
        House house = iterator.next().toHouse();

        //check if owner is logged in
        String ownerId = iterator.next().getOwnerId();
        boolean isOwnerLoggedIn = RedisCache.isSessionOfUser(session, ownerId);
        if(isOwnerLoggedIn == false)
            throw new WebApplicationException("Owner not logged in", Response.Status.UNAUTHORIZED);

        //delete from database
        db.delHouseById(id);

        //delete from cache
        try (Jedis jedis = RedisCache.getCachePool().getResource()) {
            jedis.del("house:" + id);

            //delete from recent houses
            ObjectMapper mapper = new ObjectMapper();
            String houseString = mapper.writeValueAsString(house);
            Long cnt = jedis.lrem(MOST_RECENT_HOUSES_REDIS_KEY, 0, houseString);
            jedis.decrBy(NUM_RECENT_HOUSES, cnt);

            //delete from houses
            cnt = jedis.lrem(HOUSES_REDIS_KEY, 0, houseString);
            jedis.decrBy(NUM_HOUSES, cnt);

        } catch (Exception e){
            e.printStackTrace();
        }

        return Response.status(Response.Status.NO_CONTENT).build();
    }

    @GET
    @Path("/location/{location}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<House> getHousesByLocation(@PathParam("location") String location) {

        Locale.setDefault(Locale.US);
        CosmosDBLayer db = CosmosDBLayer.getInstance();

        CosmosPagedIterable<HouseDao> houses = db.getHousesByLocation(location);

        List<House> ret = new ArrayList<>();

        for (HouseDao h : houses)
            ret.add(new House(h));

        return ret;
    }

    //get all the houses in a given location and whithin a given time range from start to end
    @GET
    @Path("/location/{location}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<House> getHousesByLocationAndTime(@PathParam("location") String location, @QueryParam("start") String start, @QueryParam("end") String end) {

        Locale.setDefault(Locale.US);
        CosmosDBLayer db = CosmosDBLayer.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("MM-yyyy"); //todo check if this is the right format

        Date startDate = null;
        Date endDate = null;
        try {
            startDate = sdf.parse(start);
            endDate = sdf.parse(end);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        CosmosPagedIterable<HouseDao> houses = db.getHouseByLocationAndTime(location, startDate, endDate); //todo possivélmente mal

        List<House> ret = new ArrayList<>();

        for (HouseDao h : houses)
            ret.add(new House(h));

        return ret;
    }


    //post availability
    @POST
    @Path("/{id}/availability")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Availabity newAvailability(@CookieParam("scc:session") Cookie session, @PathParam("id") String houseId, Availabity availabity ) throws ParseException {
        CosmosDBLayer db = CosmosDBLayer.getInstance();

        //check if house exists
        Iterator<HouseDao> houseIter = db.getHouseById(houseId).iterator();
        if(houseIter.hasNext() == false)
            throw new WebApplicationException("House not found", Response.Status.NOT_FOUND);

        // check if owner is logged in
        String ownerid = houseIter.next().getOwnerId();
        boolean isOwnerLoggedIn = RedisCache.isSessionOfUser(session, ownerid);
        if(isOwnerLoggedIn == false)
            throw new WebApplicationException("Owner not logged in", Response.Status.UNAUTHORIZED);


        //first get list of availabilities for the house
        CosmosPagedIterable<AvailabityDao> availabilities = db.getAvailabilitiesForHouse(houseId);

        SimpleDateFormat dateFormat = new SimpleDateFormat("MM-yyyy");

        //todo rever bem isto porque pode star errado
        Date start = dateFormat.parse(availabity.getFromDate());
        Date end = dateFormat.parse(availabity.getToDate());

        for (AvailabityDao a : availabilities) {
            Date aStart = dateFormat.parse(a.getFromDate());
            Date aEnd = dateFormat.parse(a.getToDate());
            // check if start and end does not intersect aStart and aEnd
            if (start.compareTo(aStart) >= 0 && start.compareTo(aEnd) <= 0) {
                throw new WebApplicationException("Availability date is already taken for the given house", Response.Status.CONFLICT);
            }

        }


        // put availability in db
        AvailabityDao a = new AvailabityDao(availabity);
        UUID uniqueID = UUID.randomUUID();
        a.setId(uniqueID.toString());
        db.putAvailability(a);
        availabity.setId(uniqueID.toString());

        //put availability in cache
        try (Jedis jedis = RedisCache.getCachePool().getResource()) {
            ObjectMapper mapper = new ObjectMapper();
            jedis.set("availability:"+availabity.getId(), mapper.writeValueAsString(availabity));
        } catch (Exception e){
            e.printStackTrace();
        }

        return availabity;
    }

}
