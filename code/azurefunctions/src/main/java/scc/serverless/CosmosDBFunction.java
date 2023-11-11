package scc.serverless;

import com.microsoft.azure.functions.annotation.*;

import redis.clients.jedis.Jedis;
import scc.cache.RedisCache;
import com.microsoft.azure.functions.*;



/**
 * Azure Functions with Timer Trigger.
 */
public class CosmosDBFunction {

	private static final String CONNECTION_URL = System.getenv("COSMOSDB_URL");
	private static final String DB_KEY = System.getenv("COSMOSDB_KEY");
	private static final String DB_NAME = System.getenv("COSMOSDB_DATABASE");

	private static final String MOST_RECENT_USERS_REDIS_KEY = "mostRecentUsers";

    @FunctionName("cosmosDBtest")
    public void updateMostRecentUsers(@CosmosDBTrigger(name = "cosmosTest",
    										databaseName = "DB_NAME", // = to be defined in system variables
    										collectionName = "users",
    										preferredLocations="West Europe",
    										createLeaseCollectionIfNotExists = true,
    										connectionStringSetting = "AzureCosmosDBConnection")  // = to be defined in system variables
        							String[] users,
        							final ExecutionContext context ) {

		try (Jedis jedis = RedisCache.getCachePool().getResource()) {
			jedis.incr("cnt:cosmos");

			for( String u : users) {
				jedis.lpush(MOST_RECENT_USERS_REDIS_KEY, u);
			}
			jedis.ltrim(MOST_RECENT_USERS_REDIS_KEY, 0, 9);

		}
    }

}
