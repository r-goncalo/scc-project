package scc.srv;


import com.azure.cosmos.util.CosmosPagedIterable;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import redis.clients.jedis.Jedis;
import scc.cache.RedisCache;
import scc.data.*;
import scc.db.CosmosDBLayer;
import scc.utils.Hash;


import java.util.*;

/**
 * Resource for managing users
 */
@Path("/user") //this means the path will be /rest/user
public class UserResource {

    private static final int MAX_RECENT_USERS_IN_CACHE = 5;
    private static final String MOST_RECENT_USERS_REDIS_KEY = "mostRecentUsers";
    private static final String USERS_REDIS_KEY = "users";
    private static final String NUM_RECENT_USERS = "numRecentUsers";
    private static final String NUM_USERS = "numUsers";


    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public static User newUser(User user){

        LogResource.writeLine("\nUSER : CREATE USER : name: " + user.getName() + ", pwd = " + user.getPwd());

        
        CosmosDBLayer db = CosmosDBLayer.getInstance();

        //check if user already exists
        if(db.getUserById(user.getId()).iterator().hasNext())
            throw new WebApplicationException("User already exists", Response.Status.CONFLICT);

        //chek if user id is not "Deleted User"
        if(user.getId().equals("Deleted User"))
            throw new WebApplicationException("User id cannot be \"Deleted User\"", Response.Status.CONFLICT);

        UserDAO u = new UserDAO(user);
        u.setPwd(Hash.of(user.getPwd()));
        db.putUser(u); //puts user in database

        //Note: maybe redis stuff should be in separate methods or even class
        String id = u.getId();
        //we'll save the user in cache
        try (Jedis jedis = RedisCache.getCachePool().getResource()) {

            ObjectMapper mapper = new ObjectMapper();
            User userRedis = u.toUser();
            jedis.set("user:"+id, mapper.writeValueAsString(userRedis)); //the index will be "user" + <that user's id>

            Long cnt = jedis.lpush(MOST_RECENT_USERS_REDIS_KEY, mapper.writeValueAsString(userRedis)); //puts user in list and returns the size of the list

            if (cnt > MAX_RECENT_USERS_IN_CACHE)
                jedis.ltrim(MOST_RECENT_USERS_REDIS_KEY, 0, MAX_RECENT_USERS_IN_CACHE - 1);
            if(cnt < MAX_RECENT_USERS_IN_CACHE)
                jedis.incr(NUM_RECENT_USERS);

            
            jedis.lpush(USERS_REDIS_KEY, mapper.writeValueAsString(userRedis));
            jedis.incr(NUM_USERS);

        } catch (Exception e) {
            LogResource.writeLine("    error when putting in cache: " + e.getClass() + ": " + e.getMessage());

        }

        LogResource.writeLine("    user created with success");
        return user;

    }

    //update user (not implemented)
    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public static User updateUser(@PathParam("id") String id, @CookieParam("scc:session") Cookie session, User user){
        LogResource.writeLine("\nUSER : UPDATE USER : id = " + id + ", name = " + user.getName() + ", pwd = " + user.getPwd());
        
        CosmosDBLayer db = CosmosDBLayer.getInstance();

        //check if user is logged in
        boolean isLoggedIn = RedisCache.isSessionOfUser(session, id);
        if(isLoggedIn == false)
            throw new WebApplicationException("User not logged in", Response.Status.UNAUTHORIZED);

        //check if user exists
        if(db.getUserById(id).iterator().hasNext() == false)
            throw new NotFoundException("User not found");

        //check if id is the same as user's id
        if(!id.equals(user.getId()))
            throw new WebApplicationException("User id does not match", Response.Status.CONFLICT);


        //update user
        User ret = db.updateUser(new UserDAO(user)).toUser();

        //update redis
        try (Jedis jedis = RedisCache.getCachePool().getResource()) {
            ObjectMapper mapper = new ObjectMapper();
            jedis.set("user:" + id, mapper.writeValueAsString(ret));
        } catch (Exception e) {
            LogResource.writeLine("    error when putting in cache: " + e.getClass() + ": " + e.getMessage());
        }

        return ret;
    }


    //delete user given id and pwd or session
    @DELETE
    @Path("/{id}")
    public static Response deleteUser(@PathParam("id") String id, @CookieParam("scc:session") Cookie session) {
        LogResource.writeLine("\nUSER : DELETE USER : id = " + id);
        CosmosDBLayer db = CosmosDBLayer.getInstance();

        User user = getUser(id);
        //check if user is logged in
        boolean isLoggedIn = RedisCache.isSessionOfUser(session, id);
        if(isLoggedIn == false)
            throw new WebApplicationException("User not logged in", Response.Status.UNAUTHORIZED);

        //check if user exists
        if(user == null)
            throw new NotFoundException("User not found");



        //update user's houses with owner id "Deleted User"
        CosmosPagedIterable<HouseDao> houses = db.getHousesForUser(id);//todo posso usar jedis nisto?
        for (HouseDao house : houses) {
            db.updateHouseOwner(house.getId(), "Deleted User");
        }

        //delete user
        db.delUserById(id);

        //delete from redis
        try (Jedis jedis = RedisCache.getCachePool().getResource()) {
            jedis.del("user:"+id);
            ObjectMapper mapper = new ObjectMapper();

            String userString = mapper.writeValueAsString(user);
            // delete from most recent users redis key
            long cnt = jedis.lrem(MOST_RECENT_USERS_REDIS_KEY, 0, userString);
            jedis.decrBy(NUM_RECENT_USERS, cnt);

            //delete from users redis key
            cnt = jedis.lrem(USERS_REDIS_KEY, 0, userString);
            jedis.decrBy(NUM_USERS,cnt);

        } catch (Exception e) {
            LogResource.writeLine("    error when deleting from cache: " + e.getClass() + ": " + e.getMessage());
        }

        return Response.noContent().build();
    }


    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public static List<User> listUsers() {

        List<User> toReturn = new ArrayList<>();

        //get users from redis
        try (Jedis jedis = RedisCache.getCachePool().getResource()) {
            ObjectMapper mapper = new ObjectMapper();
            List<String> usersJson = jedis.lrange(MOST_RECENT_USERS_REDIS_KEY, 0, -1);
            for (String userJson : usersJson) {
                toReturn.add(mapper.readValue(userJson, User.class));
            }
        } catch (Exception e) {
            LogResource.writeLine("    error when getting from cache: " + e.getClass() + ": " + e.getMessage());
        }
        if (toReturn.isEmpty()) {
            Iterable<UserDAO> users = CosmosDBLayer.getInstance().getUsers();


            for (UserDAO user : users) {

                toReturn.add(user.toUser());

            }
        }

        return toReturn;

    }

    // list user's houses
    @GET
    @Path("/{id}/houses")
    @Produces(MediaType.APPLICATION_JSON)
    public static List<House> listHouses(@PathParam("id") String id) {
        CosmosDBLayer db = CosmosDBLayer.getInstance();

        //check if user exists
        if(db.getUserById(id).iterator().hasNext() == false)
            throw new NotFoundException("User not found");

        CosmosPagedIterable<HouseDao> houses = db.getHousesForUser(id);

        List<House> toReturn = new ArrayList<>();
        for (HouseDao house : houses) {
            toReturn.add(house.toHouse());
        }
        return toReturn;
    }


    /**
     *
     * @param userLogin a user with the relevant information (id, pwd)
     *
     * @return a session id cookie
     */
    @POST
    @Path("/auth")
    @Consumes(MediaType.APPLICATION_JSON)
    public static Response auth(Login userLogin) throws InternalServerErrorException, NotAuthorizedException {

        LogResource.writeLine("\nUSER : AUTH: id = " + userLogin.getUser() + ", pwd = " + userLogin.getPwd());

        User userInDb = getUser(userLogin.getUser());

        if(Hash.of(userLogin.getPwd()).equals(userInDb.getPwd())){
            String uid = UUID.randomUUID().toString();
            NewCookie cookie = new NewCookie.Builder("scc:session")
                    .value(uid)
                    .path("/")
                    .comment("sessionid")
                    .maxAge(3600)
                    .secure(false)
                    .httpOnly(true)
                    .build();

            try {
                RedisCache.putSession(new Session(uid, userInDb.getId()));
            }catch(Exception e){
                LogResource.writeLine("    Error saving session in cache: " + e.getClass() + ": " + e.getMessage());
                throw new InternalServerErrorException("Error saving session");
            }

            LogResource.writeLine("    Authenticated with success: (cookie = " + cookie.getValue() + ", userId = " + userLogin.getUser()+" )");
            return Response.ok().cookie(cookie).build();

        }

        LogResource.writeLine("    wrong password");
        throw new NotAuthorizedException("Incorrect login");
    }

    private static User getUser(String id) {
        User user = null;

        try (Jedis jedis = RedisCache.getCachePool().getResource()) {

            String userJson = jedis.get("user:" + id);

            if (userJson != null) {

                ObjectMapper mapper = new ObjectMapper();
                user = mapper.readValue(userJson, User.class);

            }

        } catch (Exception e) {
            LogResource.writeLine("    error when getting from cache: " + e.getClass() + ": " + e.getMessage());
        }

        if (user == null) {

            CosmosPagedIterable<UserDAO> users = CosmosDBLayer.getInstance().getUserById(id);

            if (users.iterator().hasNext()) {

                user = users.iterator().next().toUser();

                try (Jedis jedis = RedisCache.getCachePool().getResource()) {
                    ObjectMapper mapper = new ObjectMapper();
                    jedis.set("user:" + id, mapper.writeValueAsString(user));

                } catch (Exception e) {
                    LogResource.writeLine("    error when putting in cache: " + e.getClass() + ": " + e.getMessage());
                }

            }

        }
        //check if user is null
        if(user == null)
            throw new NotFoundException("User not found");

        return user;
    }


}
