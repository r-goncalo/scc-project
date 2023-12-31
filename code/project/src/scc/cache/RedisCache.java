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
	public static final boolean REDIS_ENABLED = true;

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

		getCachePool().getResource().set("session=" + session.getUid(), session.getUserId());

	}

	/**
	 *
	 * @param session, the cookie sent by the client
	 * @param id, the id of the user the client is claiming to be
	 *
	 * @return if user corresponding to session and id are the same, false otherwise
	 *
	 * @throws NotAuthorizedException if the session of the user is not initialized or there is no session of that user in cache
	 */
	public static boolean isSessionOfUser(Cookie session, String id) throws NotAuthorizedException {

		if(session == null || session.getValue() == null) {
			LogResource.writeLine("    cookie session invalid or null");
			throw new NotAuthorizedException("No session initialized");
		}

		String sessionUserId;

		sessionUserId = getCachePool().getResource().get("session=" + session.getValue());

		if (sessionUserId == null) {
			LogResource.writeLine("    no cookie session in cache registered with value: " + session.getValue());
			throw new NotAuthorizedException("No valid session initialized");

		}

		if (!sessionUserId.equals(id)) {
			LogResource.writeLine("    cookie session existent but with different id: " + id + " != " + sessionUserId);
			return false;
		}

		return true;

	}

	public static void reset() {

		getCachePool().getResource().flushAll();

	}
}
