package scc.media;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import scc.srv.LogResource;
import scc.utils.Hash;

import java.util.ArrayList;
import java.util.List;

public class MediaBlob implements MediaInterface {

    private static final String CONTAINER_NAME = "media";
    String STORAGE_CONNECT_STRING = System.getenv("BlobStoreConnection");

    public MediaBlob() {}


    public void upload(String key, byte[] contents) {

            BinaryData data = BinaryData.fromBytes(contents);

            // Get container client
            BlobContainerClient containerClient = new BlobContainerClientBuilder()
                    .connectionString(STORAGE_CONNECT_STRING)
                    .containerName(CONTAINER_NAME)
                    .buildClient();

            // Get client to blob
            BlobClient blob = containerClient.getBlobClient( key);

            // Upload contents from BinaryData (check documentation for other alternatives)
            blob.upload(data);
    }


    public byte[] download(String id) {


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

        return arr;
    }

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
