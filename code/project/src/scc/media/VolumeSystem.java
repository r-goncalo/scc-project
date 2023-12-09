package scc.media;

import scc.srv.LogResource;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class VolumeSystem implements MediaInterface{

    private static final String VOLUME_PATH = System.getenv("VOLUME_PATH");


    @Override
    public void upload(String key, byte[] contents) throws IOException {

        String path = VOLUME_PATH + "/" +  key;
        LogResource.writeLine("    writing file in path: " + path);
        Files.write(Path.of(path), contents);

    }

    @Override
    public byte[] download(String id) throws IOException {

        String path = VOLUME_PATH  + "/" +   id;
        LogResource.writeLine("    reading file in path: " + path);

        return Files.readAllBytes(Path.of(path));

    }

    @Override
    public List<String> list() throws Exception {

        LogResource.writeLine("    reading files in path: " + VOLUME_PATH);

        File folder = new File(VOLUME_PATH);

        File[] listOfFiles = folder.listFiles();

        if(listOfFiles == null)
            throw new Exception("Is not path of folder");

        List<String> toReturn = new ArrayList<>(listOfFiles.length);

        for(File file : listOfFiles){

            toReturn.add(file.getName());

        }

        return toReturn;

    }
}
