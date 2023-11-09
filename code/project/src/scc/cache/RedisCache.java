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

	private static final String RedisHostname = System.getenv("REDIS_KEY");
	private static final String RedisKey = System.getenv("REDIS_URL");
	
	private static JedisPool instance;
	
	public synchronized static JedisPool getCachePool() {

		if( instance != null)
			return instance;

		final JedisPoolConfig poolConfig = new JedisPoolConfig();
		poolConfig.setMaxTotal(128);
		poolConfig.setMaxIdle(128);
		poolConfig.setMinIdle(16);
		poolConfig.setTestOnBorrow(true);
		poolConfig.setTestOnReturn(true);
		poolConfig.setTestWhileIdle(true);
		poolConfig.setNumTestsPerEvictionRun(3);
		poolConfig.setBlockWhenExhausted(true);
		instance = new JedisPool(poolConfig, RedisHostname, 6380, 1000, RedisKey, true);

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
	 * @param session
	 * @param id
	 *
	 * @return
	 *
	 * @throws NotActiveException
	 */
	public static boolean isSessionOfUser(Cookie session, String id) throws NotAuthorizedException {

		if(session == null || session.getValue() == null)
			throw new NotAuthorizedException("No session initialized");

		String sessionUserId;

		sessionUserId = getCachePool().getResource().get(session.getValue());

		if (sessionUserId == null)

			throw new NotAuthorizedException("No valid session initialized");

		if (!sessionUserId.equals(id))
			return false;

		return true;

	}
}
