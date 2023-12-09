package scc.media;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.blob.models.BlobItem;

import java.util.ArrayList;
import java.util.List;

public interface MediaInterface {

    public String upload(String key, byte[] contents);

    public byte[] download(String id);

    public List<String> list();


}
