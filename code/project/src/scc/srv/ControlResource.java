package scc.srv;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import scc.db.CosmosDBLayer;
import scc.cache.RedisCache;

/**
 * Class with control endpoints.
 */
@Path("/ctrl")
public class ControlResource
{

	/**
	 * This methods just prints a string. It may be useful to check if the current
	 * version is running on Azure.
	 */
	@Path("/version")
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String hello() {
		return "v: 0001";
	}

	//delete everything in cache and cosmosdb
	@Path("/reset")
	@DELETE
	public Response reset() {
		CosmosDBLayer.getInstance().reset();
		RedisCache.reset();
		LogResource.reset();
		return Response.ok().build();
	}


}