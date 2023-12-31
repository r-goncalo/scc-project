package scc.serverless;

import com.microsoft.azure.functions.annotation.*;

import redis.clients.jedis.Jedis;
import scc.cache.RedisCache;
import com.microsoft.azure.functions.*;

import static scc.cache.RedisCache.LOG_KEY;


/**
 * Azure Functions with Timer Trigger.
 */
public class UpdateMostRecentUsers {

	private static final String CONNECTION_URL = System.getenv("COSMOSDB_URL");
	private static final String DB_KEY = System.getenv("COSMOSDB_KEY");
	private static final String DB_NAME = System.getenv("COSMOSDB_DATABASE");

	private static final int MAX_RECENT_USERS_IN_CACHE = 5;
	private static final String MOST_RECENT_USERS_REDIS_KEY = "mostRecentUsers";

    @FunctionName("updateMostRecentUsers")
    public void updateMostRecentUsersOnUserAddition(@CosmosDBTrigger(name = "cosmosTest",
    										databaseName = "scc24db60519", // = to be defined in system variables
    										collectionName = "users",
    										preferredLocations="West Europe",
    										createLeaseCollectionIfNotExists = true,
    										connectionStringSetting = "AzureCosmosDBConnection")  // = to be defined in system variables
        							String[] users,
        							final ExecutionContext context ) {


		try (Jedis jedis = RedisCache.getCachePool().getResource()) {

			jedis.append(LOG_KEY, "FUNCTIONS : UpdateMostRecentUsers\n");

			for( String u : users)
				jedis.lpush(MOST_RECENT_USERS_REDIS_KEY, u);

			jedis.ltrim(MOST_RECENT_USERS_REDIS_KEY, 0, MAX_RECENT_USERS_IN_CACHE - 1);

		} catch (Exception e){


			RedisCache.writeLogLine("    exception in updating most recent users " + e.getClass() + " " + e.getMessage() );

		}
    }

}
