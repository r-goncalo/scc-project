package scc.db;

import com.azure.cosmos.*;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import com.azure.cosmos.util.CosmosPagedIterable;
import scc.data.*;

import java.util.Date;

public class CosmosDBLayer {

	private static final String CONNECTION_URL = System.getenv("COSMOSDB_URL");
	private static final String DB_KEY = System.getenv("COSMOSDB_KEY");
	private static final String DB_NAME = System.getenv("COSMOSDB_DATABASE");

	private static CosmosDBLayer instance;

	public static synchronized CosmosDBLayer getInstance() {
		if( instance != null)
			return instance;


		try {

			CosmosClient client = new CosmosClientBuilder()
					.endpoint(CONNECTION_URL)
					.key(DB_KEY)
					//.directMode()
					.gatewayMode()
					// replace by .directMode() for better performance
					.consistencyLevel(ConsistencyLevel.SESSION)
					.connectionSharingAcrossClientsEnabled(true)
					.contentResponseOnWriteEnabled(true)
					.buildClient();
			instance = new CosmosDBLayer(client);

		} catch (Exception e){


		}


		return instance;

	}

	private CosmosClient client;
	private CosmosDatabase db;
	private CosmosContainer users;
	private CosmosContainer houses;
	private CosmosContainer rentals;
	private CosmosContainer questions;
	private CosmosContainer availabilities;

	public CosmosDBLayer(CosmosClient client) {
		this.client = client;
	}

	private synchronized void init() {

		if( db != null)
			return;

		db = client.getDatabase(DB_NAME);

		users = db.getContainer("users");
		houses = db.getContainer("houses");
		rentals = db.getContainer("rentals");
		questions = db.getContainer("questions");
		availabilities = db.getContainer("availabilities");

	}

	public void close() {
		client.close();
	}

	/*
	 //////////////////// USERS ///////////////
	 */


	public CosmosPagedIterable<UserDAO> getUsers() {
		init();
		return users.queryItems("SELECT * FROM users ", new CosmosQueryRequestOptions(), UserDAO.class);
	}

	/*
	//////////////////// HOUSES ///////////////
	*/

	public HouseDao updateHouse(HouseDao houseDao) {
		init();
		CosmosItemResponse<HouseDao> response = houses.upsertItem(houseDao);
		return response.getItem();
	}

	public CosmosItemResponse<HouseDao> putHouse(HouseDao h) {
		init();
		return houses.createItem(h);
	}

	public CosmosPagedIterable<HouseDao> getHouseById(String id) {
		init();
		return houses.queryItems("SELECT * FROM houses WHERE users.id=\"" + id + "\"", new CosmosQueryRequestOptions(), HouseDao.class);
	}

	public CosmosPagedIterable<HouseDao> getHouses() {
		init();
		return houses.queryItems("SELECT * FROM houses", new CosmosQueryRequestOptions(), HouseDao.class);

	}


	public CosmosItemResponse<Object> delHouseById(String id) {
		init();
		PartitionKey key = new PartitionKey(id);
		return houses.deleteItem(id, key, new CosmosItemRequestOptions());
	}

	//list all of a users house
	public CosmosPagedIterable<HouseDao> getHousesForUser(String id) {
		init();
		return houses.queryItems("SELECT * FROM houses WHERE houses.ownerId=\"" + id + "\"", new CosmosQueryRequestOptions(), HouseDao.class);
	}

	//make a function to get all the houses in a given location
	public CosmosPagedIterable<HouseDao> getHousesByLocation(String location) {
		init();
		return houses.queryItems("SELECT * FROM houses WHERE houses.location=\"" + location + "\"", new CosmosQueryRequestOptions(), HouseDao.class);
	}


	public CosmosPagedIterable<HouseDao> getHouseByLocationAndTime(String location, Date start, Date end) {
		init();
		// select * from house where house.location = location and house.id not in (select houseId from rental where rental.day between start and end)
		return houses.queryItems("SELECT * FROM houses WHERE houses.location=\"" + location + "\" AND houses.id NOT IN (SELECT rentals.houseId FROM rentals WHERE rentals.day BETWEEN \"" + start.toString() + "\" AND \"" + end.toString() + "\")", new CosmosQueryRequestOptions(), HouseDao.class);
	}


	public CosmosPagedIterable<HouseDao> getAllHouses() {
		init();
		return houses.queryItems("SELECT * FROM houses", new CosmosQueryRequestOptions(), HouseDao.class);

	}

}
