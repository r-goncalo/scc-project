package scc.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.Cookie;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import scc.data.Session;
import scc.data.UserDAO;
import scc.srv.LogResource;

import java.io.NotActiveException;

public class RedisCache {

	private static final String REDIS_HOST_NAME = System.getenv("REDIS_URL");
	private static final String REDIS_KEY = System.getenv("REDIS_KEY");

	private static JedisPool instance;
	
	public synchronized static JedisPool getCachePool() {

		if( instance != null)
			return instance;

		LogResource.writeLine("    Creating Redis Client...");
		LogResource.writeLine("        Redis host: " + REDIS_HOST_NAME);
		LogResource.writeLine("        Redis key: " + REDIS_KEY);

		final JedisPoolConfig poolConfig = new JedisPoolConfig();
		poolConfig.setMaxTotal(128);
		poolConfig.setMaxIdle(128);
		poolConfig.setMinIdle(16);
		poolConfig.setTestOnBorrow(true);
		poolConfig.setTestOnReturn(true);
		poolConfig.setTestWhileIdle(true);
		poolConfig.setNumTestsPerEvictionRun(3);
		poolConfig.setBlockWhenExhausted(true);
		instance = new JedisPool(poolConfig, REDIS_HOST_NAME, 6380, 1000, REDIS_KEY, true);

		LogResource.writeLine("    Finished creating Redis Client...");

		return instance;
		
	}


	/*
	///////////////////// USERS ///////////////
	*/

	/*
	///////////////////// SESSION ///////////////
	*/

	public static void putSession(Session session){

		ObjectMapper mapper = new ObjectMapper();

		try {

			getCachePool().getResource().set("session=" + session.getUid(), mapper.writeValueAsString(session.getUser()));

		} catch (JsonProcessingException e) {

			LogResource.writeLine("   Error puting session in cache");

			e.printStackTrace();
		}

	}

	public static Session getSession(String uid) throws JsonProcessingException {

		Session session = null;

		try {

			ObjectMapper mapper = new ObjectMapper();
			String sessionString = getCachePool().getResource().get("session=" + uid);
			session = mapper.readValue(sessionString, Session.class);

		}catch(JsonProcessingException exception){

			LogResource.writeLine("   Error getting session from cache");

		}
		return session;

	}

	/**
	 *
	 * @param session
	 * @param id
	 *
	 * @return
	 *
	 * @throws NotActiveException
	 */
	public Session checkCookieUser(Cookie session, String id) throws NotActiveException {

		if(session == null || session.getValue() == null)
			throw new NotAuthorizedException("No session initialized");

		Session s;

		try {

			s = getSession(session.getValue());

		} catch (Exception e) {
			return null;
		}
		if (s == null || s.getUser() == null)

			throw new NotAuthorizedException("No valid session initialized");

		if (!s.getUser().equals(id) && !s.getUser().equals("admin"))
			throw new NotAuthorizedException("Invalid user : " + s.getUser());

		return s;

	}
}
