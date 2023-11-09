package scc.serverless;

import com.microsoft.azure.functions.annotation.*;

import redis.clients.jedis.Jedis;
import scc.cache.RedisCache;

import com.microsoft.azure.functions.*;

/**
 * Azure Functions with Blob Trigger.
 */
public class BlobStoreFunction
{
	private static final String CONTAINER_NAME = "media";
	String STORAGE_CONNECT_STRING = System.getenv("BlobStoreConnection");

	@FunctionName("blobtest")
	public void setLastBlobInfo(@BlobTrigger(name = "blobtest", 
									dataType = "binary", 
									path = "images/{name}", 
									connection = "BlobStoreConnection") 
								byte[] content,
								@BindingName("name") String blobname, 
								final ExecutionContext context) {
		try (Jedis jedis = RedisCache.getCachePool().getResource()) {
			jedis.incr("cnt:blob");
			jedis.set("serverless::blob::name",
					"Blob name : " + blobname + " ; size = " + (content == null ? "0" : content.length));
		}
	}

}
