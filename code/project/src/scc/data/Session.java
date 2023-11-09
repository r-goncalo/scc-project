package scc.data;

/**
 * represents a session
 */
public class Session {

    private String uid; //the id of the session (cookie)
    private User user; //a user object with relevant information (id, ...)

    public Session(String uid, User user) {

        this.uid = uid;
        this.user = user;

    }

    public String getUid() {return uid;}
    public User getUser(){return user;}

}
