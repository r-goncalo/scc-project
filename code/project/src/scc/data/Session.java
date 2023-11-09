package scc.data;

/**
 * represents a session
 */
public class Session {

    String uid; //the id of the session (cookie)
    User user; //a user object with relevant information (id, ...)

    public Session(String uid, User user) {

        this.uid = uid;
        this.user = user;

    }

    String getUid() {return uid;}
    User getUser(){return user;}

}
