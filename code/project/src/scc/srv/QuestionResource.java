package scc.srv;

import com.azure.cosmos.util.CosmosPagedIterable;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import redis.clients.jedis.Jedis;
import scc.cache.RedisCache;
import scc.data.HouseDao;
import scc.data.Question;
import scc.data.QuestionDao;
import scc.data.UserDAO;
import scc.db.CosmosDBLayer;

import java.util.*;

@Path("/house/{houseId}/question")
public class QuestionResource {
    public static final String MOST_RECENT_QUESTION = "mostRecentQuestion";
    public static final String NUM_RECENT_QUESTION = "numRecentQuestion";
    public static final int NUM_MAX_RECENTE_QUESTIONS = 5;
    public static final String NUM_QUESTION = "numQuestion";
    public static final String QUESTION_REDIS_KEY = "question";
    //A question can only
    //be answered by the user that owns the house, and there can be only one answer for each
    //question.

    //add new question
    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public static Question newQuestion(@CookieParam("scc:session") Cookie session, @PathParam("houseId") String houseId, Question question) {
        Locale.setDefault(Locale.US);
        CosmosDBLayer db = CosmosDBLayer.getInstance();

        question.setId(UUID.randomUUID().toString());
        QuestionDao q = new QuestionDao(question);

        CosmosPagedIterable<HouseDao> h = db.getHouseById(houseId);

        //check if user exists
        Iterator<UserDAO> userIter = db.getUserById(question.getUserId()).iterator();
        if (userIter.hasNext() == false)
            throw new NotFoundException("User not found");

        //check if house exists
        if(h.iterator().hasNext() == false)
            throw new NotFoundException("House not found");

        //check if user is logged in
        boolean isLoggedIn = RedisCache.isSessionOfUser(session, userIter.next().getId());
        if(isLoggedIn == false)
            throw new WebApplicationException("User not logged in", Response.Status.UNAUTHORIZED);


        if (q.getReplyToId() != null) {
            //check if question to reply to exists
            if (db.getQuestionByIdAndHouse(houseId,q.getReplyToId()).iterator().hasNext() == false)
                throw new NotFoundException("Question to reply to not found");

            //check if questions has already been answered
            if (db.getQuestionByReplyToIdAndHouse(houseId,q.getReplyToId()).iterator().hasNext() == true)
                throw new NotFoundException("Question has already been answered");

            //check if userid is the same as house owner
            if (h.iterator().next().getOwnerId().equals(question.getUserId()) == false)
                throw new NotFoundException("Only the owner of the house can reply to a question");

        }

        //store in cache
        try (Jedis jedis = RedisCache.getCachePool().getResource()) {

            ObjectMapper mapper = new ObjectMapper();

            jedis.set("question:"+question.getId(), mapper.writeValueAsString(question));

            Long cnt = jedis.lpush(MOST_RECENT_QUESTION, mapper.writeValueAsString(question));

            if (cnt > NUM_MAX_RECENTE_QUESTIONS)
                jedis.ltrim(MOST_RECENT_QUESTION, 0, NUM_MAX_RECENTE_QUESTIONS - 1);
            if (cnt < NUM_MAX_RECENTE_QUESTIONS)
                jedis.incr(NUM_RECENT_QUESTION);

            jedis.lpush(QUESTION_REDIS_KEY, mapper.writeValueAsString(question));
            jedis.incr(NUM_QUESTION);

        } catch(Exception e) {
            e.printStackTrace();
        }


        db.putQuestion(q);
        return question;
    }

    //get question
    @GET
    @Path("/{questionID}")
    @Produces(MediaType.APPLICATION_JSON)
    public static Question getQuestion(@PathParam("questionID") String questionID,
                                       @PathParam("houseId") String houseId) {
        Locale.setDefault(Locale.US);
        CosmosDBLayer db = CosmosDBLayer.getInstance();

        QuestionDao q = db.getQuestionByIdAndHouse(houseId, questionID).iterator().next();

        if (q == null)
            throw new NotFoundException();

        return new Question(q);
    }

    //list questions for a given house
    //noanswer=true&st=0&len=20
    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public static List<Question> listQuestions(@PathParam("houseId") String houseId,
                                               @QueryParam("noanswer") String noanswer,
                                               @QueryParam("st") String start,
                                               @QueryParam("len") String length) {
        Locale.setDefault(Locale.US);
        CosmosDBLayer db = CosmosDBLayer.getInstance();
        int startInt = 0;
        int lenInt = Integer.MAX_VALUE/2;

        if (start != null ){
            startInt = Integer.parseInt(start);
        }

        if (length != null){
            lenInt = Integer.parseInt(length);

        }

        if (lenInt == 0){
            return new ArrayList<>();
        }


        //get from cache
        try (Jedis jedis = RedisCache.getCachePool().getResource()) {

            ObjectMapper mapper = new ObjectMapper();

            List<String> questions = jedis.lrange(QUESTION_REDIS_KEY, startInt, startInt + lenInt - 1);

            List<Question> ret = new ArrayList<>();

            if(noanswer.equals("1")) {
                for (String q : questions) {
                    Question question = mapper.readValue(q, Question.class);
                    if(question.getReplyToId() == null)
                        ret.add(question);
                }
            }else {
                for (String q : questions) {
                    Question question = mapper.readValue(q, Question.class);
                    ret.add(question);
                }
            }

            return ret;

        } catch(Exception e) {
            e.printStackTrace();
        }


        //get from cosmos


        CosmosPagedIterable<QuestionDao> questions = db.getQuestionsForHouse(houseId);

        List<Question> toReturn = new ArrayList<>();

        if (noanswer.equals("1")) {
            for (QuestionDao q : questions) {
                if (q.getReplyToId() == null)
                    toReturn.add(new Question(q));
            }
        } else {
            for (QuestionDao q : questions)
                toReturn.add(new Question(q));
        }

        if(startInt > toReturn.size())
            return new ArrayList<>();

        List<Question> ret;
        ret = toReturn.subList(startInt, Math.min(startInt + lenInt, toReturn.size()));

        return ret;

    }


}