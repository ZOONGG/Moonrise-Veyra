package dev.veyra.client.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class Config {
    public final File file;
    public final long creationDate;

    public Config(File pathToFile){
        long creationDate1;
        this.file = pathToFile;

        if(!file.exists()){
            creationDate1 = System.currentTimeMillis();
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }  else {
            try {
                creationDate1 = getData().get("creationTime").getAsLong();
            } catch (NullPointerException e){
                creationDate1 = 0L;
            }
        }
        this.creationDate = creationDate1;
    }

    public String getName(){
        return file.getName().replace(".rcfg", "");
    }

    public JsonObject getData(){
        JsonParser jsonParser = new JsonParser();
        try (FileReader reader = new FileReader(file))
        {
            Object obj = jsonParser.parse(reader);
            return (JsonObject) obj;
        } catch (JsonSyntaxException | ClassCastException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public synchronized void save(JsonObject data){
        data.addProperty("creationTime", creationDate);
        Path destination = file.toPath();
        Path temporary = null;
        try {
            temporary = Files.createTempFile(destination.getParent(), file.getName(), ".tmp");
            Files.writeString(temporary, data.toString(), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, destination,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                }
            }
        }
    }
}
