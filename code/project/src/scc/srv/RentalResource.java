package scc.srv;

import com.azure.cosmos.util.CosmosPagedIterable;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import scc.data.HouseDao;
import scc.data.Rental;
import scc.data.RentalDao;
import scc.db.CosmosDBLayer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;



//todo restrições (caso não haja casa ou user, não pode criar rental. Caso já haja rental, não se pode criar outro)

@Path("/house/{houseId}/rental") //todo checkar se valores entre {} são iguais aos do @pathparam
public class RentalResource {

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public static Rental newRental( @PathParam("houseId") String houseId ,Rental rental){

        Locale.setDefault(Locale.US);
        CosmosDBLayer db = CosmosDBLayer.getInstance();

        //check if rental id already exists
        if(db.getrentalbyidandhouse(houseId,rental.getId()).iterator().hasNext())
            throw new WebApplicationException("Rental already exists", Response.Status.CONFLICT);

        //confirm rental's houseid
        if(!houseId.equals(rental.getHouseId())){
            throw new NotFoundException("House not found");
        }

        //todo confirm rental's date is not taken. use db.gethouserentalfordate
        if(db.getHouseRentalForDate(rental.getDay(),rental.getHouseId()).iterator().hasNext())
            throw new WebApplicationException("Rental date is already taken for the given house", Response.Status.CONFLICT);

        // confirm rental's user exists
        if(db.getUserById(rental.getUserId()).iterator().hasNext()==false)
            throw new WebApplicationException("User not found", Response.Status.NOT_FOUND);


        Iterator<HouseDao> h = db.getHouseById(rental.getHouseId()).iterator();
        //todo confirm rental's house exists
        if(h.hasNext() == false)
            throw new NotFoundException(); //todo house not found exception

        //todo assign rental price
        rental.setPrice(h.next().getNormalPrice()); //todo fazer para o discounted price


        RentalDao r = new RentalDao(rental);


        return db.putRental(r).getItem().toRental();
    }

    //get a specific rental for a given houseid
    @GET
    @Path("/{rentalId}")
    @Produces(MediaType.APPLICATION_JSON)
    public static Rental getRental(@PathParam("houseId") String houseId, @PathParam("rentalId") String rentalId) {

        Locale.setDefault(Locale.US);
        CosmosDBLayer db = CosmosDBLayer.getInstance();

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