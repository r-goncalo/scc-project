package scc.srv;


import com.azure.cosmos.util.CosmosPagedIterable;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import redis.clients.jedis.Jedis;
import scc.cache.RedisCache;
import scc.data.HouseDao;
import scc.data.Session;
import scc.data.User;
import scc.data.UserDAO;
import scc.db.CosmosDBLayer;
import scc.utils.Hash;


import java.util.*;

/**
 * Resource for managing users
 */
@Path("/user") //this means the path will be /rest/user
public class UserResource {

    private static final int MAX_USERS_IN_CACHE = 5;

    public UserResource (){}

    /**
     *
     * @param user
     *
     * @return the generated id of the user
     */
    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public static User newUser(User user){

        LogResource.writeLine("\nUSER : CREATE USER : name: " + user.getName() + ", pwd = " + user.getPwd());

        String id = "0:" + System.currentTimeMillis();
        user.setId(id);
        LogResource.writeLine("    Generated id: " + id);

        Locale.setDefault(Locale.US);
        CosmosDBLayer db = CosmosDBLayer.getInstance();

        UserDAO u = new UserDAO(user);
        u.setPwd(Hash.of(user.getPwd()));
        db.putUser(u); //puts user in database

        //Note: maybe redis stuff should be in separate methods or even class

        //we'll save the user in cache
        try (Jedis jedis = RedisCache.getCachePool().getResource()) {

            ObjectMapper mapper = new ObjectMapper();

            jedis.set("user:"+id, mapper.writeValueAsString(u)); //the index will be "user" + <that user's id>

            Long cnt = jedis.lpush("MostRecentUsers", mapper.writeValueAsString(u));

            if (cnt > MAX_USERS_IN_CACHE)
                jedis.ltrim("MostRecentUsers", 0, MAX_USERS_IN_CACHE - 1);

            jedis.incr("NumUsers");

        } catch (Exception e) {
            LogResource.writeLine("    error when putting in cache: " + e.getClass() + ": " + e.getMessage());

        }

        LogResource.writeLine("    user created with success");
        return user;

    }

    /**
     * Note: This method is not needed
     * Note: missing password implementation
     *
     * Return the user. Throw an appropriate error message if
     * id does not exist.
     *
     * @return the getted user, null if it does not exist
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public User getUser(@CookieParam("scc:session") Cookie session, @PathParam("id") String id, @QueryParam("pwd") String pwd) {

        LogResource.writeLine("\nUSER : GET USER : id = " + id + ", pwd = " + pwd);

        User user = verifyUser(session, id, pwd); //this will cause an exception in case of failing

        if(user == null)
            user = getUser(id);

        return user;


    }

    /**
     *
     * @param session
     * @param id, id of user client is claiming to be
     * @param pwd
     *
     * @return the user if it was getted during verification, null otherwise (session verification)
     *
     * @throws ForbiddenException if the session and pwd failed
     */
    public User verifyUser(Cookie session, String id, String pwd) throws ForbiddenException{

        try {

            boolean isSessionValid = RedisCache.isSessionOfUser(session, id);

            if(!isSessionValid) {

                LogResource.writeLine("    session not authorized");
                throw new ForbiddenException("    session not authorized");
            }

            return null;

        } catch(NotAuthorizedException e){

            LogResource.writeLine("    " + e.getMessage());

        }

        //there is no session, using pwd
        if(pwd == null) {
            LogResource.writeLine("    No registered password");
            throw new ForbiddenException("    session not authorized (wrong session or wrong pwd)");
        }

        User user = getUser(id);

        if(!user.getPwd().equals(Hash.of(pwd))) {

            LogResource.writeLine("    wrong password");
            throw new ForbiddenException("    wrong password");


        }

        return user;

    }

    /**
     *
     * @param id of the user to get
     *
     * @return the user
     *
     * @throws NotFoundException when can't retrieve user from cache or database
     */
    public User getUser(String id) throws NotFoundException{

        Locale.setDefault(Locale.US);
        CosmosDBLayer db = CosmosDBLayer.getInstance();

        ObjectMapper mapper = new ObjectMapper();

        try (Jedis jedis = RedisCache.getCachePool().getResource()) {

            String res = jedis.get("user:"+id);

            if(res != null) {

                LogResource.writeLine("    cache hit getting user with id: " + id);

                // How to convert string to object
                UserDAO uread = mapper.readValue(res, UserDAO.class);

                return uread.toUser();

            }

        } catch (Exception e) {
            LogResource.writeLine("    error when getting from cache: " + e.getClass() + ": " + e.getMessage());

        }

        Iterator<UserDAO> dbUser = db.getUserById(id).iterator();

        if(!dbUser.hasNext()) {
            LogResource.writeLine("    user not found in database");
            throw new NotFoundException("User does not exist");
        }

        return dbUser.next().toUser();

    }

    @DELETE
    @Path("/{id}")
    public void deleteUser(@CookieParam("scc:session") Cookie session, @PathParam("id") String id, @QueryParam("pwd") String pwd) {

        LogResource.writeLine("\nUSER : DELETE USER : id = " + id + ", pwd = " + pwd);

        Locale.setDefault(Locale.US);
        CosmosDBLayer db = CosmosDBLayer.getInstance();

        User user = verifyUser(session, id, pwd); //this will cause an exception in case of failing


        db.delUserById(id);

        RedisCache.getCachePool().getResource().del("user:" + id);


    }

    /**
     * Lists the ids of images stored.
     */
    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> list() {

        LogResource.writeLine("USER : GET USERS");

        List<String> toReturn = new ArrayList<>();


        CosmosPagedIterable<UserDAO> users =  CosmosDBLayer.getInstance().getUsers();


        for( UserDAO user : users){

            toReturn.add(user.getName());

        }

        LogResource.writeLine("   Number of users: " + toReturn.size());

        return toReturn;

    }


    /**
     *
     * @param user a user with the relevant information (id, pwd)
     *
     * @return a session id cookie
     */
    @POST
    @Path("/auth")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response auth(User user){


        LogResource.writeLine("\nUSER : AUTH: id = " + user.getId() + ", pwd = " + user.getPwd());

        User userInDb = getUser(user.getId());

        if(Hash.of(user.getPwd()).equals(userInDb.getPwd())){

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

            LogResource.writeLine("    Authenticated with success: (cookie = " + cookie.getValue() + ", userId = " + user.getId());
            return Response.ok().cookie(cookie).build();

        }

        throw new NotAuthorizedException("Incorrect login");



    }





}
