package scc.serverless;

import com.azure.cosmos.util.CosmosPagedIterable;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.CosmosDBTrigger;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.TimerTrigger;
import redis.clients.jedis.Jedis;
import scc.cache.RedisCache;
import scc.data.HouseDao;
import scc.data.User;
import scc.data.UserDAO;
import scc.db.CosmosDBLayer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static scc.cache.RedisCache.LOG_KEY;

public class ThreatUserDeletion {

    @FunctionName("houses-deleted-users")
    public void housesWithDeletedUsers( @TimerTrigger(name = "periodicSetTime",
    								schedule = "30 */1 * * * *")
    				String timerInfo,
    				ExecutionContext context) {

		RedisCache.writeLogLine("Executing houses with deleted users at: " + new SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z").format(new Date()));

		CosmosDBLayer db = CosmosDBLayer.getInstance();

		CosmosPagedIterable<HouseDao> housesDB = db.getAllHouses();
		CosmosPagedIterable<UserDAO> usersDB = db.getUsers();

		Set<String> deletedUsersIDs = new HashSet<>();
		Set<String> nonDeletedUsersIDs = new HashSet<>();

		for(HouseDao house : housesDB){

			if(house.getOwnerId() != null) {

				String ownerId = house.getOwnerId();

				boolean ownerExists = (!deletedUsersIDs.contains(ownerId) && nonDeletedUsersIDs.contains(ownerId)) || findIdInUsers(ownerId, usersDB);

				if (!ownerExists) {

					deletedUsersIDs.add(house.getOwnerId());

					house.setOwnerId(null);

					db.updateHouse(house);

				}else
					nonDeletedUsersIDs.add(ownerId);
			}

		}

    }

	private boolean findIdInUsers(String userId, Iterable<UserDAO> users){

		for(UserDAO user : users)
			if(user.getId().equals(userId))
				return true;

		return false;

	}

}
