package scc.data;

/**
 * represents a session
 */
public class Session {

    private String uid; //the id of the session (cookie)
    private String userId; //a user object with relevant information (id, ...)

    public Session(String uid, String userId) {

        this.uid = uid;
        this.userId = userId;

    }

    public String getUid() {return uid;}
    public String getUserId(){return userId;}

}
