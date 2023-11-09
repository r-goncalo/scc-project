package scc.srv;

import com.azure.cosmos.util.CosmosPagedIterable;
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


@Path("/house")
public class HouseResource {


    private static final long MAX_USERS_IN_CACHE = 5;

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public static House newHouse(House house){

        LogResource.writeLine("HOUSE : CREATE HOUSE : name = " + house.getName());

        Locale.setDefault(Locale.US);
        CosmosDBLayer db = CosmosDBLayer.getInstance();
        String id = "0:" + System.currentTimeMillis();

        LogResource.writeLine("   id = " + id);

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

        } catch(Exception e) {
            e.printStackTrace();
        }

        return house;
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public House getHouse(@PathParam("id") String id) {

        LogResource.writeLine("HOUSE : CREATE HOUSE : id = " + id);

        Locale.setDefault(Locale.US);
        CosmosDBLayer db = CosmosDBLayer.getInstance();

        ObjectMapper mapper = new ObjectMapper();

        try (Jedis jedis = RedisCache.getCachePool().getResource()) {

            String res = jedis.get("user:"+id);

            if(res != null) {

                LogResource.writeLine("    cache hit");

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
    public List<String> list() {

        LogResource.writeLine("HOUSE : LIST HOUSES");

        List<String> toReturn = new ArrayList<>();


        Iterable<HouseDao> houses =  CosmosDBLayer.getInstance().getAllHouses();

        for( HouseDao house : houses){
            toReturn.add(house.getName());
        }
        return toReturn;
    }

    @DELETE
    @Path("/house/{id}")
    public void deleteHouse(@PathParam("id") String id, @QueryParam("pwd") String pwd) {

        Locale.setDefault(Locale.US);
        CosmosDBLayer db = CosmosDBLayer.getInstance();

        //delete from database
        CosmosPagedIterable<HouseDao> dbHouse = db.getHouseById(id);

        HouseDao houseDao = dbHouse.iterator().next();

        db.delUserById(houseDao.getId());


        //delete from cache
        try (Jedis jedis = RedisCache.getCachePool().getResource()) {
            jedis.del("house:" + id);
        } catch (Exception e){
            e.printStackTrace();
        }


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
    //    @GET
    //    @Path("/location/{location}/{start}/{end}")
    //    @Produces(MediaType.APPLICATION_JSON)
    //    public List<House> getHousesByLocationAndTime(@PathParam("location") String location, @PathParam("start") String start, @PathParam("end") String end) {
    //
    //    }
}