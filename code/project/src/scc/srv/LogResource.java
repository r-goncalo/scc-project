package scc.srv;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * temporary class to store (public, unrestrained, unsecure) logs
 */
@Path("/log")
public class LogResource {

    private static String LOG = "";

    public static void writeLine(String line){
        LOG += line + "\n";
    }

    public static void reset() {
        LOG = "";
        writeLine("Log reset");
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getLog() {
        return LOG;
    }

    @POST
    public void cleanLog() {
        LOG = "";
    }

}