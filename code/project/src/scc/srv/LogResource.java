package scc.srv;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.ArrayList;

/**
 * Class with control endpoints.
 */
@Path("/log")
public class LogResource {


    private static String LOG = "";


    public static void writeLine(String line){

        LOG += "\n" + line;

    }

    /**
     * This methods just prints a string. It may be useful to check if the current
     * version is running on Azure.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getLog() {
        return LOG;
    }



}
