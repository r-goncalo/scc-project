package scc.srv;


import com.azure.cosmos.util.CosmosPagedIterable;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import redis.clients.jedis.Jedis;
import scc.cache.RedisCache;
import scc.data.User;
import scc.data.UserDAO;
import scc.db.CosmosDBLayer;


import java.util.*;

/**
 * Resource for managing users
 */
@Path("/user") //this means the path will be /rest/media
public class UserResource {


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



        try (Jedis jedis = RedisCache.getCachePool().getResource()) {

            ObjectMapper mapper = new ObjectMapper();

            jedis.set("user:"+id, mapper.writeValueAsString(u));
            String res = jedis.get("user:"+id);

            // How to convert string to object
            UserDAO uread = mapper.readValue(res, UserDAO.class);
            System.out.println("GET value = " + res);

            Long cnt = jedis.lpush("MostRecentUsers", mapper.writeValueAsString(u));

            if (cnt > 5)
                jedis.ltrim("MostRecentUsers", 0, 4);

            List<String> lst = jedis.lrange("MostRecentUsers", 0, -1);
            System.out.println("MostRecentUsers");

            for( String s : lst)
                System.out.println(s);

            cnt = jedis.incr("NumUsers");
            System.out.println( "Num users : " + cnt);

        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return id;

    }

    /**
     * Note: This method is not needed
     *
     * Return the contents of an image. Throw an appropriate error message if
     * id does not exist.
     * @return
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public CosmosPagedIterable<UserDAO> download(@PathParam("id") String id) {

        Locale.setDefault(Locale.US);
        CosmosDBLayer db = CosmosDBLayer.getInstance();

        try (Jedis jedis = RedisCache.getCachePool().getResource()) {

            ObjectMapper mapper = new ObjectMapper();

            String res = jedis.get("user:"+id);

            // How to convert string to object
            UserDAO uread = mapper.readValue(res, UserDAO.class);
            System.out.println("GET value = " + res);
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }


        return db.getUserById(id);
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
