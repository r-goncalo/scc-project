package scc.srv;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import com.fasterxml.jackson.core.io.JsonStringEncoder;
import jakarta.ws.rs.*;
import scc.media.BlobStorage;
import scc.utils.Hash;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.MediaType;

/**
 * Resource for managing media files, such as images.
 */
@Path("/media") //this means the path will be /rest/media
public class MediaResource
{

	private static final String CONTAINER_NAME = "media";
	String STORAGE_CONNECT_STRING = System.getenv("BlobStoreConnection");

	public MediaResource() {}

	/**
	 * Post a new image.The id of the image is its hash.
	 */
	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_OCTET_STREAM)
	@Produces(MediaType.APPLICATION_JSON)
	public String upload(byte[] contents) throws IOException {

		String key = Hash.of(contents);

		LogResource.writeLine("MEDIA : UPLOAD : id(hash) = " + key);

		try {

			BlobStorage.getInstance().upload(key, contents);

			LogResource.writeLine("    File uploaded");

		} catch( Exception e) {

			LogResource.writeLine("   Error uploading: " + e.getMessage());

			throw e;
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
	public byte[] download(@PathParam("id") String id) throws IOException {

		LogResource.writeLine("MEDIA : DOWNLOAD : id = " + id);

		byte[] arr = BlobStorage.getInstance().download(id);

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

		LogResource.writeLine("MEDIA : LIST");

		return  BlobStorage.getInstance().list();

	}
}
