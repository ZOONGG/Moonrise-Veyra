package dev.veyra.client.module.setting.impl;

import com.google.gson.JsonObject;
import dev.veyra.client.clickgui.Component;
import dev.veyra.client.clickgui.components.ModuleComponent;
import dev.veyra.client.module.setting.Setting;

public class ComboSetting<T extends Enum<?>> extends Setting {
    private T[] options;
    private T currentOption;
    private final T defaultOption;

    public ComboSetting(String settingName, T defaultOption){
        super(settingName);

        this.currentOption = defaultOption;
        this.defaultOption = defaultOption;
        this.options = (T[]) defaultOption.getDeclaringClass().getEnumConstants();
    }

    @Override
    public void resetToDefaults() {
        this.currentOption = defaultOption;
    }

    @Override
    public JsonObject getConfigAsJson() {
        JsonObject data = new JsonObject();
        data.addProperty("type", getSettingType());
        data.addProperty("value", getMode().name());
        return data;
    }

    @Override
    public String getSettingType() {
        return "mode";
    }

    @Override
    public void applyConfigFromJson(JsonObject data) {
        if(!data.get("type").getAsString().equals(getSettingType()))
            return;

        String configuredValue = data.get("value").getAsString();
        for(T opt : options){
            if(opt.name().equalsIgnoreCase(configuredValue)) {
                setMode(opt);
                return;
            }
        }
        resetToDefaults();
    }

    @Override
    public Component createComponent(ModuleComponent moduleComponent) {
        return null;
    }

    public T getMode(){
        return this.currentOption;
    }

    public void setMode(T value){
        this.currentOption = value;
    }

    public void nextMode(){
        for(int i = 0; i < options.length; i++){
            if(options[i] == currentOption) {
                currentOption = options[(i + 1) % (options.length)];
                return;
            }
        }
    }
}
