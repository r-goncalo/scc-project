package scc.srv;

import com.azure.cosmos.implementation.ConflictException;
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

//The goal of this project is to understand how services available in cloud computing
//platforms can be used for creating applications that are scalable, fast, and highly available.
//The project will consist in the design and implementation of the backend for a house rental
//company (either short term or long term, as you choose) and the companion testing scripts.
//The system will manage the rental of houses. Users can make houses available for renting.
//Users can rent houses. Users can also pose questions about a house. A question can only
//be answered by the user that owns the house, and there can be only one answer for each
//question.
//For implementing its features, the system must maintain, at least, the following
//information (you can add more information, as it seems appropriate for your system):
//• User: information about users, including the nickname, name, (hash of the)
//password, photo;
//• Media: images and videos used in the system;
//• House: information about a house, including a name, a location, a description, at
//least one photo.
//• Associated with each house, it is also necessary to maintain the availability of the
//house (when it is available for renting) and price for each period (periods can be
//divided in days, weeks or months, as you prefer; for prices you can maintain the
//normal price and a promotion price).
//• Rental: Each rental should include information about the house being rented, the
//user renting the house, the period and price of the rental.
//• Question: houses’ questions and replies. Each question must include the house it
//refers to, the user that posed the question and the text of the message.
//The system must support, at least, the following basic operations using standard REST
//rules for defining endpoints:
//• User (/rest/user): create user, delete user, update user. After a user is deleted, houses
//and rentals from the user appear as been performed by a default “Deleted User”
//user.
//• Media (/rest/media): upload media, download media.
//• House (/rest/house): create a house, update a house, delete a house.
//• Rental (/rest/house/{id}/rental): base URL for rental information, supporting
//operations for creating, updating and listing rental information.
//• Question (/rest/house/{id}/question): create question or reply, list questions.
//Operations that create an entity with an associated media object (e.g., users and houses)
//can be implemented either by having clients executing two consecutive calls, the first for
//uploading the image and the second for the corresponding operation; or by executing a
//single operation that includes the media and the remaining data.

// Rental (/rest/house/{id}/rental): base URL for rental information, supporting
//operations for creating, updating and listing rental information

//todo restrições (caso não haja casa ou user, não pode criar rental. Caso já haja rental, não se pode criar outro)

@Path("/rest/house/{HouseId}/rental")
public class RentalResource {

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public static String newRental( @PathParam("HouseId") String houseId ,Rental rental){

        Locale.setDefault(Locale.US);
        CosmosDBLayer db = CosmosDBLayer.getInstance();
        String id = "0:" + System.currentTimeMillis();//todo mudar os ids

        //confirm rental's houseid
        if(!houseId.equals(rental.getHouseId())){
            //throw 404
            throw new NotFoundException("House not found");
        }

        //todo confirm rental's date is not taken. use db.gethouserentalfordate
        if(db.getHouseRentalForDate(rental.getDay(),rental.getHouseId()).iterator().hasNext())
            throw new WebApplicationException("Rental date is already taken for the given house", Response.Status.CONFLICT);

        // confirm rental's user exists
        if(!db.getUserById(rental.getUserId()).stream().iterator().hasNext())
            throw new NotFoundException(); //todo user not found exception

        Iterator<HouseDao> h = db.getHouseById(rental.getHouseId()).iterator();
        //todo confirm rental's house exists
        if(h.hasNext() == false)
            throw new NotFoundException(); //todo house not found exception

        //todo assign rental price
        rental.setPrice(h.next().getNormalPrice()); //todo fazer para o discounted price


        RentalDao r = new RentalDao(rental);
        db.putRental(r);

        return id;
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public static Rental getRental(@PathParam("id") String rentalID) {

        Locale.setDefault(Locale.US);
        CosmosDBLayer db = CosmosDBLayer.getInstance();

        RentalDao r = db.getRentalById(rentalID).iterator().next();

        if (r == null)
            throw new NotFoundException();

        return new Rental(r);
    }

    @GET
    @Path("/house/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public static List<Rental> getRentalsForHouse(@PathParam("id") String id) {

        Locale.setDefault(Locale.US);
        CosmosDBLayer db = CosmosDBLayer.getInstance();

        CosmosPagedIterable<RentalDao> rentals = db.getRentalsForHouse(id);

        List<Rental> ret = new ArrayList<>();

        for (RentalDao r : rentals)
            ret.add(new Rental(r));

        return ret;
    }

    @GET
    @Path("/user/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public static List<Rental> getRentalsForUser(@PathParam("id") String id) {

        Locale.setDefault(Locale.US);
        CosmosDBLayer db = CosmosDBLayer.getInstance();

        CosmosPagedIterable<RentalDao> rentals = db.getRentalsForUser(id);

        List<Rental> ret = new ArrayList<>();

        for (RentalDao r : rentals)
            ret.add(new Rental(r));
        return  ret;
    }

}