package scc.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisCache {

	private static final String RedisHostname = System.getenv("REDIS_URL");
	private static final String RedisKey = System.getenv("REDIS_KEY");
	
	private static JedisPool instance;

	public static final String LOG_KEY = "functions:log";
	
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

	/**
	 *
	 * this method should only be used if there isn't an already existent use of jedis
	 *
	 * @param line
	 */
	public synchronized static void writeLogLine(String line){

		try (Jedis jedis = RedisCache.getCachePool().getResource()) {

			jedis.append(LOG_KEY, line + "\n"); //the index will be "user" + <that user's id>

		}

	}

}
