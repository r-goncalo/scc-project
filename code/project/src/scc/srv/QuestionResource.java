package scc.srv;

import com.azure.cosmos.util.CosmosPagedIterable;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.ServiceUnavailableException;
import jakarta.ws.rs.core.MediaType;
import scc.data.HouseDao;
import scc.data.Question;
import scc.data.QuestionDao;
import scc.db.CosmosDBLayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Path("/house/{houseId}/question")
public class QuestionResource {
    //A question can only
    //be answered by the user that owns the house, and there can be only one answer for each
    //question.

    //add new question
    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public static Question newQuestion(@PathParam("houseId") String houseId, Question question) {
        Locale.setDefault(Locale.US);
        CosmosDBLayer db = CosmosDBLayer.getInstance();

        QuestionDao q = new QuestionDao(question);
        CosmosPagedIterable<HouseDao> h = db.getHouseById(houseId);

        //check if user exists
        if (db.getUserById(question.getUserId()).iterator().hasNext() == false)
            throw new NotFoundException("User not found");

        //check if house exists
        if(h.iterator().hasNext() == false)
            throw new NotFoundException("House not found");

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
    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public static List<Question> getQuestions(@PathParam("houseId") String houseId) {
        Locale.setDefault(Locale.US);
        CosmosDBLayer db = CosmosDBLayer.getInstance();

        CosmosPagedIterable<QuestionDao> questions = db.getQuestionsForHouse(houseId);

        List<Question> ret = new ArrayList<>();

        for (QuestionDao q : questions)
            ret.add(new Question(q));

        return ret;
    }


}
