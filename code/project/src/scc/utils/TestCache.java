package scc.utils;

import java.util.List;
import java.util.Locale;

import redis.clients.jedis.Jedis;
import com.fasterxml.jackson.databind.ObjectMapper;
import scc.cache.RedisCache;
import scc.data.User;
import scc.data.UserDAO;

/**
 * Standalone program for accessing the database
 *
 */
public class TestCache {
	
	public static void main(String[] args) {
		System.setProperty(org.slf4j.simple.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "Error");

		try {
			ObjectMapper mapper = new ObjectMapper();

			Locale.setDefault(Locale.US);
			String id = "0:" + System.currentTimeMillis();
			UserDAO u = new UserDAO();
			u.setId(id);
			u.setName("SCC " + id);
			u.setPwd("super_secret");
			u.setPhotoId("0:34253455");
			u.setHouseIds(new String[0]);

			try (Jedis jedis = RedisCache.getCachePool().getResource()) {
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
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}


