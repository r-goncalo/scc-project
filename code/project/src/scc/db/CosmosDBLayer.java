package scc.db;

import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import com.azure.cosmos.util.CosmosPagedIterable;

import scc.data.*;

import java.util.Date;

import scc.srv.LogResource;

public class CosmosDBLayer {

	private static final String CONNECTION_URL = System.getenv("COSMOSDB_URL");
	private static final String DB_KEY = System.getenv("COSMOSDB_KEY");
	private static final String DB_NAME = System.getenv("COSMOSDB_DATABASE");

	private static CosmosDBLayer instance;

	public static synchronized CosmosDBLayer getInstance() {
		if( instance != null)
			return instance;

		LogResource.writeLine("    Creating Cosmos Database Client...");

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

			LogResource.writeLine("        error creating cosmos database client: " + e.getMessage());

		}

		LogResource.writeLine("    Ended creation of Cosmos Database Client...");

		return instance;

	}

	private CosmosClient client;
	private CosmosDatabase db;
	private CosmosContainer users;
	private CosmosContainer houses;
	private CosmosContainer rentals;
	private CosmosContainer questions;

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

	}

	public void close() {
		client.close();
	}

	/*
	 //////////////////// USERS ///////////////
	 */

	public CosmosItemResponse<Object> delUserById(String id) {
		init();
		PartitionKey key = new PartitionKey( id);
		return users.deleteItem(id, key, new CosmosItemRequestOptions());
	}

	public CosmosItemResponse<Object> delUser(UserDAO user) {
		init();
		return users.deleteItem(user, new CosmosItemRequestOptions());
	}

	public CosmosItemResponse<UserDAO> putUser(UserDAO user) {
		init();
		return users.createItem(user);
	}

	public CosmosPagedIterable<UserDAO> getUserById( String id) {
		init();
		return users.queryItems("SELECT * FROM users WHERE users.id=\"" + id + "\"", new CosmosQueryRequestOptions(), UserDAO.class);
	}

	public CosmosPagedIterable<UserDAO> getUsers() {
		init();
		return users.queryItems("SELECT * FROM users ", new CosmosQueryRequestOptions(), UserDAO.class);
	}

	/*
	//////////////////// HOUSES ///////////////
	*/

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
	/*
	////////////////// RENTALS ////////////////
	 */

	//make a function to get all the rentals in a given date for a given house id
	public CosmosPagedIterable<RentalDao> getHouseRentalForDate(Date date, String houseId) {
		init();
		return rentals.queryItems("SELECT * FROM rentals WHERE rentals.houseId=\"" + houseId + "\" AND rentals.day=\"" + date.toString() + "\"", new CosmosQueryRequestOptions(), RentalDao.class);
	}

	// list of rentals that will have a discounted price in the following two months
	public CosmosPagedIterable<RentalDao> getRentalsWithDiscount() {
		init();
		//SELECT * FROM rentals WHERE rentals.day BETWEEN CURRENT_DATE and CURRENT_DATE + INTERVAL 2 MONTH and rentals.price = (select house.discount from house where house.id = rentals.houseId)
		return rentals.queryItems("SELECT * FROM rentals WHERE rentals.day BETWEEN CURRENT_DATE and CURRENT_DATE + INTERVAL 2 MONTH and rentals.price = (select house.discount from house where house.id = rentals.houseId)", new CosmosQueryRequestOptions(), RentalDao.class);

	}


	//return CosmosItemResponse<RentalDao> with all the rentals in a given period from start to end.
	// Use between in the sql query
	public CosmosPagedIterable<RentalDao> getRentalsByPeriod(String start, String end) {
		init();
		return rentals.queryItems("SELECT * FROM rentals WHERE rentals.day BETWEEN \"" + start + "\" AND \"" + end + "\"", new CosmosQueryRequestOptions(), RentalDao.class);
	}


	//do the same functions of users and houses but for rentals and questions
	public CosmosItemResponse<RentalDao> putRental(RentalDao rental) {
		init();
		return rentals.createItem(rental);
	}

	public CosmosPagedIterable<RentalDao> getRentalById(String id) {
		init();
		return rentals.queryItems("SELECT * FROM rentals WHERE rentals.id=\"" + id + "\"", new CosmosQueryRequestOptions(), RentalDao.class);
	}

	//getrentalsforhouse
	public CosmosPagedIterable<RentalDao> getRentalsForHouse(String id) {
		init();
		return rentals.queryItems("SELECT * FROM rentals WHERE rentals.houseId=\"" + id + "\"", new CosmosQueryRequestOptions(), RentalDao.class);
	}
	//get rental for user
	public CosmosPagedIterable<RentalDao> getRentalsForUser(String id) {
		init();
		return rentals.queryItems("SELECT * FROM rentals WHERE rentals.userId=\"" + id + "\"", new CosmosQueryRequestOptions(), RentalDao.class);
	}
	//get all rentals
	public CosmosPagedIterable<RentalDao> getRentals() {
		init();
		return rentals.queryItems("SELECT * FROM rentals ", new CosmosQueryRequestOptions(), RentalDao.class);
	}

	/*
	/////////////////// QUESTIONS /////////////
	 */

	//Questions
	public CosmosItemResponse<Object> putQuestion(Object question) {
		init();
		return questions.createItem(question);
	}

	public CosmosPagedIterable<Object> getQuestionById(String id) {
		init();
		return questions.queryItems("SELECT * FROM questions WHERE questions.id=\"" + id + "\"", new CosmosQueryRequestOptions(), Object.class);
	}

	public CosmosPagedIterable<Object> getQuestions() {
		init();
		return questions.queryItems("SELECT * FROM questions ", new CosmosQueryRequestOptions(), Object.class);
	}


}
