package scc.data;

public class Login {
    //id
    private String id;
    //pwd
    private String pwd;
    public Login(String id, String pwd){
        this.id = id;
        this.pwd = pwd;
    }
    public Login(){

    }

    //getters

    public String getId() {
        return id;
    }

    public String getPwd() {
        return pwd;
    }
}
