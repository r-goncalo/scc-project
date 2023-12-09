package scc.media;

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

        Files.write(Path.of(VOLUME_PATH), contents);

    }

    @Override
    public byte[] download(String id) throws IOException {

        return Files.readAllBytes(Path.of(VOLUME_PATH));

    }

    @Override
    public List<String> list() {

        File folder = new File("your/path");
        File[] listOfFiles = folder.listFiles();

        List<String> toReturn = new ArrayList<>(listOfFiles.length);

        for(File file : listOfFiles){

            toReturn.add(file.getName());

        }

        return toReturn;
        
    }
}
