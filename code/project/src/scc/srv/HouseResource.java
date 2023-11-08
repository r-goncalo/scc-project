package scc.srv;

import com.azure.cosmos.util.CosmosPagedIterable;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import redis.clients.jedis.Jedis;
import scc.cache.RedisCache;
import scc.data.House;
import scc.data.HouseDao;
import scc.db.CosmosDBLayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


@Path("/house") //this means the path will be /rest/user
public class HouseResource {


    private static final long MAX_USERS_IN_CACHE = 5;

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public static House newHouse(House house){

        Locale.setDefault(Locale.US);
        CosmosDBLayer db = CosmosDBLayer.getInstance();
        String id = "0:" + System.currentTimeMillis();

        HouseDao h = new HouseDao(house);
        db.putHouse(h);


        //we'll save the user in cache
        try (Jedis jedis = RedisCache.getCachePool().getResource()) {

            ObjectMapper mapper = new ObjectMapper();

            jedis.set("house:"+id, mapper.writeValueAsString(h)); //the index will be "user" + <that user's id>

            Long cnt = jedis.lpush("MostRecentHouses", mapper.writeValueAsString(h));

            if (cnt > MAX_USERS_IN_CACHE)
                jedis.ltrim("MostRecentHouses", 0, MAX_USERS_IN_CACHE - 1);

            jedis.incr("NumHouses");

        } catch (JsonMappingException e) {
            e.printStackTrace();

        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return house;
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


        } catch (JsonMappingException e) {
            e.printStackTrace();


        } catch (JsonProcessingException e) {
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
    public List<String> list() {

        List<String> toReturn = new ArrayList<>();


        Iterable<HouseDao> houses =  CosmosDBLayer.getInstance().getHouses();

        for( HouseDao house : houses){
            toReturn.add(house.getName());
        }
        return toReturn;
    }


}
