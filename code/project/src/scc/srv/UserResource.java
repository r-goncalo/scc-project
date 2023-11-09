package scc.srv;


import com.azure.cosmos.util.CosmosPagedIterable;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import redis.clients.jedis.Jedis;
import scc.cache.RedisCache;
import scc.data.HouseDao;
import scc.data.User;
import scc.data.UserDAO;
import scc.db.CosmosDBLayer;


import java.util.*;

/**
 * Resource for managing users
 */
@Path("/user") //this means the path will be /rest/user
public class UserResource {

    private static final int MAX_USERS_IN_CACHE = 5;


    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public static String newUser(User user){

        Locale.setDefault(Locale.US);
        CosmosDBLayer db = CosmosDBLayer.getInstance();
        String id = "0:" + System.currentTimeMillis();
        UserDAO u = new UserDAO(user);
        db.putUser(u); //puts user in database


        //we'll save the user in cache
        try (Jedis jedis = RedisCache.getCachePool().getResource()) {

            ObjectMapper mapper = new ObjectMapper();

            jedis.set("user:"+id, mapper.writeValueAsString(u)); //the index will be "user" + <that user's id>

            Long cnt = jedis.lpush("MostRecentUsers", mapper.writeValueAsString(u));

            if (cnt > MAX_USERS_IN_CACHE)
                jedis.ltrim("MostRecentUsers", 0, MAX_USERS_IN_CACHE - 1);

            jedis.incr("NumUsers");

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
     * Return the user. Throw an appropriate error message if
     * id does not exist.
     * @return
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public User getUser(@PathParam("id") String id) {

        Locale.setDefault(Locale.US);
        CosmosDBLayer db = CosmosDBLayer.getInstance();

        ObjectMapper mapper = new ObjectMapper();

        try (Jedis jedis = RedisCache.getCachePool().getResource()) {

            String res = jedis.get("user:"+id);

            if(res != null) {

                // How to convert string to object
                UserDAO uread = mapper.readValue(res, UserDAO.class);

                return uread.toUser();

            }


        } catch (JsonMappingException e) {
            e.printStackTrace();


        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        CosmosPagedIterable<UserDAO> dbUser = db.getUserById(id);

        UserDAO userDao = dbUser.iterator().next();

        return userDao.toUser();
    }


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

    // list user's houses
    @GET
    @Path("/{id}/houses")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> listHouses(@PathParam("id") String id) {
        Locale.setDefault(Locale.US);
        CosmosDBLayer db = CosmosDBLayer.getInstance();
        CosmosPagedIterable<HouseDao> houses = db.getHousesForUser(id);

        List<String> toReturn = new ArrayList<>();
        for (HouseDao house : houses) {
            toReturn.add(house.getName());
        }
        return toReturn;
    }




}
