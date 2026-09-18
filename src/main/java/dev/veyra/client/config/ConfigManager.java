package dev.veyra.client.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import dev.veyra.client.main.Veyra;
import dev.veyra.client.module.Module;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class ConfigManager {
    private final File configDirectory = new File(System.getProperty("user.home") + File.separator + ".weave" + File.separator + "veyra");

    private Config config;
    private final ArrayList<Config> configs = new ArrayList<>();

    public ConfigManager() {
        if(!configDirectory.isDirectory()){
            configDirectory.mkdirs();
        }

        discoverConfigs();
        File defaultFile = new File(configDirectory, "config.rcfg");
        boolean hasSavedDefault = defaultFile.isFile()
                && defaultFile.length() > 0L
                && !isOutdated(defaultFile);
        this.config = new Config(defaultFile);

        if (hasSavedDefault) {
            setConfig(this.config);
        } else {
            save();
        }

    }

    @SuppressWarnings("unused")
    public static boolean isOutdated(File file) {
        JsonParser jsonParser = new JsonParser();
        try (FileReader reader = new FileReader(file))
        {
            Object obj = jsonParser.parse(reader);
            JsonObject data = (JsonObject) obj;
            return false;
        } catch (JsonSyntaxException | ClassCastException | IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    public void discoverConfigs(){
        configs.clear();
        if(configDirectory.listFiles() == null || !(Objects.requireNonNull(configDirectory.listFiles()).length > 0))
            return;

        for(File file : Objects.requireNonNull(configDirectory.listFiles())){
            if(file.getName().endsWith(".rcfg")){
                if(!isOutdated(file)){
                    configs.add(new Config(
                            new File(file.getPath())
                    ));
                }
            }
        }
    }

    public Config getConfig(){
        return config;
    }

    public void save(){
        JsonObject data = new JsonObject();
        data.addProperty("usedFor", 0);
        data.addProperty("lastEditTime", System.currentTimeMillis());

        JsonObject modules = new JsonObject();
        for(Module module : Veyra.moduleManager.getModules()){
            modules.add(module.getName(), module.getConfigAsJson());
        }
        data.add("modules", modules);

        config.save(data);
    }

    public void setConfig(Config config){
        JsonObject root = config.getData();
        if (root == null || !root.has("modules") || !root.get("modules").isJsonObject()) {
            return;
        }
        this.config = config;
        JsonObject data = root.getAsJsonObject("modules");
        List<Module> knownModules = new ArrayList<>(Veyra.moduleManager.getModules());
        for(Module module : knownModules){
            JsonObject moduleData = ConfigMigration.moduleData(data, module.getName());
            if(moduleData != null){
                module.applyConfigFromJson(moduleData);
            } else {
                module.resetToConfigDefaults();
            }
        }
    }

    public void loadConfigByName(String replace) {
        discoverConfigs();
        for(Config config: configs) {
            if(config.getName().equals(replace))
                setConfig(config);
        }
    }
}
