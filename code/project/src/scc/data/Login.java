package scc.data;

public class Login {
    //id
    private String user;
    //pwd
    private String pwd;
    public Login(String user, String pwd){
        this.user = user;
        this.pwd = pwd;
    }
    public Login(){

    }

    //getters

    public String getUser() {
        return user;
    }

    public String getPwd() {
        return pwd;
    }
}
