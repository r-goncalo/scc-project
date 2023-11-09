package scc.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.Cookie;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import scc.data.Session;

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

	public Session putSession(Session session){

		ObjectMapper mapper = new ObjectMapper();
		getCachePool().getResource().set("session=" + session.getUid(), mapper.writeValueAsString(session.getUser());

	}

	public Session getSession(String uid){


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

			s = getCachePool().getSession(session.getValue());

		} catch (CacheException e) {
			return null;
		}
		if (s == null || s.getUser() == null || s.getUser().length() == 0)
			throw new NotAuthorizedException("No valid session initialized");
		if (!s.getUser().equals(id) && !s.getUser().equals("admin"))
			throw new NotAuthorizedException("Invalid user : " + s.getUser());
		return s;

	}
}
