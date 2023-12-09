package scc.media;

public class MediaStorage {


    public static MediaInterface getInstance(){

        return new VolumeSystem();
        //return new MediaBlob();

    }


}
