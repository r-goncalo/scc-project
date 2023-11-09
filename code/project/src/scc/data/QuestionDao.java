package scc.data;

//make the questiondao class like the question class with the rids and ts
public class QuestionDao{
    private String _rid;
    private String _ts;
    private String id;
    private String houseId;
    private String userId;
    private String content;
    private String replyToId;

    public QuestionDao(String id, String houseId, String userId, String content, String replyToId) {
        this.id = id;
        this.houseId = houseId;
        this.userId = userId;
        this.content = content;
        this.replyToId = replyToId;
    }

    public QuestionDao(Question question) {
        this(question.getId(), question.getHouseId(), question.getUserId(), question.getText(), question.getReplyToId());
    }

    public QuestionDao(){

    }

    //getters
    public String get_rid() {
        return _rid;
    }

    public String get_ts() {
        return _ts;
    }

    public String getId() {
        return id;
    }

    public String getHouseId() {
        return houseId;
    }

    public String getUserId() {
        return userId;
    }

    public String getContent() {
        return content;
    }

    public String getReplyToId() {
        return replyToId;
    }
}