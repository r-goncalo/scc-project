package scc.srv;


import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import scc.data.User;
import scc.data.UserDAO;
import scc.db.CosmosDBLayer;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.ServiceUnavailableException;


import java.util.*;

/**
 * Resource for managing users
 */
@Path("/user") //this means the path will be /rest/media
public class UserResource {

    Map<String, UserDAO> userCasheMap = new HashMap<String, UserDAO>();

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public static String newUser(User user){

        Locale.setDefault(Locale.US);
        CosmosDBLayer db = CosmosDBLayer.getInstance();
        String id = "0:" + System.currentTimeMillis();
        UserDAO u = new UserDAO(user);
        db.putUser(u);
        return id;

    }

    /**
     * Lists the ids of images stored.
     */
    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> list() {

        List<String> toReturn = new ArrayList<>();


        Iterable<UserDAO> users =  CosmosDBLayer.getInstance().getUsers();

        for( UserDAO user : users){

            toReturn.add(user.getName());

        }

        return toReturn;

    }


}
