package scc.serverless;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.BindingName;
import com.microsoft.azure.functions.annotation.BlobTrigger;
import com.microsoft.azure.functions.annotation.FunctionName;
import redis.clients.jedis.Jedis;
import scc.cache.RedisCache;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;

import java.awt.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

/**
 * Azure Functions with Blob Trigger.
 */
public class BlobReplication
{
	private static final String CONTAINER_NAME = "media";
	String STORAGE_CONNECT_STRING = System.getenv("BlobStoreConnection");

	private static final String[] REGIONS = {"westeurope", "northeurope"};

	private static final String REGION = System.getenv("REGION_NAME");

	@FunctionName("propagateMediaCreation")
	public void propagateMediaCreation(@BlobTrigger(name = "blobtest",
									dataType = "binary", 
									path = CONTAINER_NAME + "/{name}",
									connection = "BlobStoreConnection") 
								byte[] content,
								@BindingName("name") String blobname,
								final ExecutionContext context) {

		RedisCache.writeLogLine("FUNCTIONS : PROPAGATING MEDIA FILE : ID = " + blobname);

		int maxTries = 5;
		for(String region : REGIONS){

			if(!region.equals(REGION)){


				Response r = null;
				do {

					RedisCache.writeLogLine("    propagating media file to: " + region);

					try {

						r = (ClientBuilder.newClient()).target("https://scc24app" + region + "60519.azurewebsites.net/rest/media")
								.request()
								.post(Entity.entity(content, MediaType.APPLICATION_OCTET_STREAM));

						RedisCache.writeLogLine("    propagating media file to: " + region + " response code: " + r.getStatusInfo().getStatusCode());


					}catch (Exception e){

						RedisCache.writeLogLine("    error propagating media file to: " + region + ": " + e.getClass() + " " + e.getMessage());

					}

				} while (r.getStatusInfo().getStatusCode() != 409 && (r.getStatusInfo().getStatusCode() < 200 || r.getStatusInfo().getStatusCode() >= 300 && maxTries-- > 0)); //while the response does not return "ok"
			}

		}

	}

}
