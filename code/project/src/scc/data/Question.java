package scc.data;

//Question: houses’ questions and replies. Each question must include the house it
//refers to, the user that posed the question and the text of the message.
// each question must have a replyToId meaning it's a reply to a question
//make the question class
public class Question {
    private String id;
    private String houseId;
    private String userId;
    private String text;
    private String replyToId;

    public Question(String id, String houseId, String userId, String text, String replyToId) {
        this.id = id;
        this.houseId = houseId;
        this.userId = userId;
        this.text = text;
        this.replyToId = replyToId;
    }

    public Question(QuestionDao question) {
        this(question.getId(), question.getHouseId(), question.getUserId(), question.getContent(), question.getReplyToId());
    }

    public Question(){

    }

    //getters
    public String getId() {
        return id;
    }

    public String getHouseId() {
        return houseId;
    }

    public String getUserId() {
        return userId;
    }

    public String getText() {
        return text;
    }

    public String getReplyToId() {
        return replyToId;
    }

    public void setId(String id) {
        this.id = id;
    }
}