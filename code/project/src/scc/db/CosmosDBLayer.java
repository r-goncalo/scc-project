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
		instance = new CosmosDBLayer( client);
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
		rentals = db.getContainer("rentals");//todo letra minuscula
		questions = db.getContainer("questions");
		availabilities = db.getContainer("availabilities");
	}

	//make a function to get all the rentals in a given date for a given house id
	public CosmosPagedIterable<RentalDao> getHouseRentalForDate(String date, String houseId) {
		init();
		return rentals.queryItems("SELECT * FROM rentals WHERE rentals.houseId=\"" + houseId + "\" AND rentals.date=\"" + date + "\"", new CosmosQueryRequestOptions(), RentalDao.class);
	}

	//update the owner if of a given house
	public CosmosItemResponse<HouseDao> updateHouseOwner(String houseId, String ownerId) {
		init();
		CosmosPagedIterable<HouseDao> house = houses.queryItems("SELECT * FROM houses WHERE houses.id=\"" + houseId + "\"", new CosmosQueryRequestOptions(), HouseDao.class);

		HouseDao h = house.iterator().next();
		h.setOwnerId(ownerId);
		return houses.upsertItem(h);
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

	public CosmosPagedIterable<RentalDao> getrentalbyidandhouse(String houseId, String rentalid) {
		init();
		return rentals.queryItems("SELECT * FROM rentals WHERE rentals.id=\"" + rentalid + "\" AND rentals.houseId=\"" + houseId + "\"", new CosmosQueryRequestOptions(), RentalDao.class);
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

	//Questions
	public CosmosItemResponse<QuestionDao> putQuestion(QuestionDao question) {
		init();
		return questions.createItem(question);
	}

	public CosmosPagedIterable<QuestionDao> getQuestionById(String id) {
		init();
		return questions.queryItems("SELECT * FROM questions WHERE questions.id=\"" + id + "\"", new CosmosQueryRequestOptions(), QuestionDao.class);
	}

	public CosmosPagedIterable<QuestionDao> getQuestions() {
		init();
		return questions.queryItems("SELECT * FROM questions ", new CosmosQueryRequestOptions(), QuestionDao.class);
	}

	public CosmosItemResponse<Object> delUserById(String id) {
		init();
		PartitionKey key = new PartitionKey( id);
		return users.deleteItem(id, key, new CosmosItemRequestOptions());
	}

	public CosmosItemResponse<Object> delHouseById(String id) {
		init();
		PartitionKey key = new PartitionKey(id);
		return houses.deleteItem(id, key, new CosmosItemRequestOptions());
	}

	public CosmosItemResponse<UserDAO> putUser(UserDAO user) {
		init();
		return users.createItem(user);
	}
	
	public CosmosPagedIterable<UserDAO> getUserById( String id) {
		init();
		return users.queryItems("SELECT * FROM users u WHERE u.id='" + id + "'", new CosmosQueryRequestOptions(), UserDAO.class);

	}

	public CosmosPagedIterable<UserDAO> getUsers() {
		init();
		return users.queryItems("SELECT * FROM users ", new CosmosQueryRequestOptions(), UserDAO.class);
	}

	public void close() {
		client.close();
	}


	public CosmosItemResponse<HouseDao> putHouse(HouseDao h) {
		init();
		return houses.createItem(h);
	}

	public CosmosPagedIterable<HouseDao> getHouseById(String id) {
		init();
		return houses.queryItems("SELECT * FROM houses WHERE houses.id=\"" + id + "\"", new CosmosQueryRequestOptions(), HouseDao.class);
	}

	//list all of a users house
	public CosmosPagedIterable<HouseDao> getHousesForUser(String id) {
		init();
		return houses.queryItems("SELECT * FROM houses WHERE houses.ownerId=\"" + id + "\"", new CosmosQueryRequestOptions(), HouseDao.class);
	}

	public CosmosPagedIterable<HouseDao> getAllHouses() {
		init();
		return houses.queryItems("SELECT * FROM houses", new CosmosQueryRequestOptions(), HouseDao.class);

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

	// list of rentals that will have a discounted price in the following two months
	public CosmosPagedIterable<RentalDao> getRentalsWithDiscount() {
		init();
		return rentals.queryItems("SELECT * FROM rentals WHERE rentals.day BETWEEN CURRENT_DATE and CURRENT_DATE + INTERVAL 2 MONTH and rentals.price = (select house.discount from house where house.id = rentals.houseId)", new CosmosQueryRequestOptions(), RentalDao.class);
	}

	//given a question id check if there exists a question with the same replytoid
	public CosmosPagedIterable<QuestionDao> getQuestionByReplyToIdAndHouse( String houseId, String questionId) {
		init();
		return questions.queryItems("SELECT * FROM questions WHERE questions.replyToId=\"" + questionId + "\" AND questions.houseId=\"" + houseId + "\"", new CosmosQueryRequestOptions(), QuestionDao.class);
		}

	//getQuestionByIdAndHouse
	public CosmosPagedIterable<QuestionDao> getQuestionByIdAndHouse(String houseId, String questionID) {
		init();
		return questions.queryItems("SELECT * FROM questions WHERE questions.id=\"" + questionID + "\" AND questions.houseId=\"" + houseId + "\"", new CosmosQueryRequestOptions(), QuestionDao.class);
	}

	public CosmosPagedIterable<QuestionDao> getQuestionsForHouse(String houseId) {
		return questions.queryItems("SELECT * FROM questions WHERE questions.houseId=\"" + houseId + "\"", new CosmosQueryRequestOptions(), QuestionDao.class);
	}

	public UserDAO updateUser(UserDAO userDAO) {
		init();
		CosmosItemResponse<UserDAO> response = users.upsertItem(userDAO);
		return response.getItem();
	}

	public HouseDao updateHouse(HouseDao houseDao) {
		init();
		CosmosItemResponse<HouseDao> response = houses.upsertItem(houseDao);
		return response.getItem();
	}

	public CosmosPagedIterable<AvailabityDao> getAvailabilitiesForHouse(String houseId) {
		return availabilities.queryItems("SELECT * FROM availabilities WHERE availabilities.houseId=\"" + houseId + "\"", new CosmosQueryRequestOptions(), AvailabityDao.class);
	}

	public void putAvailability(AvailabityDao a) {
		init();
		availabilities.createItem(a);
	}

	public CosmosPagedIterable<Availabity> getAvailabilityForDate(String houseId, String date) {
		init();
		return availabilities.queryItems("SELECT * FROM availabilities WHERE availabilities.houseId=\"" + houseId + "\" AND availabilities.fromDate <= \"" + date + "\" AND availabilities.toDate >= \"" + date + "\"", new CosmosQueryRequestOptions(), Availabity.class);
	}
}
