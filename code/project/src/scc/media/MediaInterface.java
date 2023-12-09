package scc.media;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.blob.models.BlobItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public interface MediaInterface {

    public void upload(String key, byte[] contents) throws IOException;

    public byte[] download(String id) throws IOException;

    public List<String> list() throws Exception;


}
