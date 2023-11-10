package scc.srv;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import scc.utils.Hash;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.ServiceUnavailableException;
import jakarta.ws.rs.core.MediaType;

/**
 * Resource for managing media files, such as images.
 */
@Path("/media") //this means the path will be /rest/media
public class MediaResource
{

	private static final String CONTAINER_NAME = "media";
	String STORAGE_CONNECT_STRING = System.getenv("BlobStoreConnection");


	/**
	 * Post a new image.The id of the image is its hash.
	 */
	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_OCTET_STREAM)
	@Produces(MediaType.APPLICATION_JSON)
	public String upload(byte[] contents) {

		String key = Hash.of(contents);

		try {
			BinaryData data = BinaryData.fromFile(java.nio.file.Path.of(key));

			// Get container client
			BlobContainerClient containerClient = new BlobContainerClientBuilder()
					.connectionString(STORAGE_CONNECT_STRING)
					.containerName(CONTAINER_NAME)
					.buildClient();

			// Get client to blob
			BlobClient blob = containerClient.getBlobClient( key);

			// Upload contents from BinaryData (check documentation for other alternatives)
			blob.upload(data);
			// todo check if it already exists


			System.out.println( "File updloaded : " + key);

		} catch( Exception e) {
			e.printStackTrace();
		}

		return key;


	}

	/**
	 * Return the contents of an image. Throw an appropriate error message if
	 * id does not exist.
	 */
	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public byte[] download(@PathParam("id") String id) {

		// Get container client
		BlobContainerClient containerClient = new BlobContainerClientBuilder()
				.connectionString(STORAGE_CONNECT_STRING)
				.containerName(CONTAINER_NAME)
				.buildClient();

		// Get client to blob
		BlobClient blob = containerClient.getBlobClient( id);

		// Download contents to BinaryData (check documentation for other alternatives)
		BinaryData data = blob.downloadContent();

		byte[] arr = data.toBytes();

		System.out.println( "Blob size : " + arr.length);

		return arr;
	}

	/**
	 * Lists the ids of images stored.
	 */
	@GET
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	public List<String> list() {

		List<String> toReturn = new ArrayList<>();

		BlobContainerClient containerClient = new BlobContainerClientBuilder()
				.connectionString(STORAGE_CONNECT_STRING)
				.containerName(CONTAINER_NAME)
				.buildClient();


		Iterable<BlobItem> blobs =  containerClient.listBlobs();

		for( BlobItem blob : blobs){

			toReturn.add(blob.getName());

		}

		return toReturn;

	}
}
