package scc.media;

import scc.srv.MediaResource;

public class BlobStorage {


    public static MediaInterface getInstance(){

        return new MediaBlob();

    }


}
