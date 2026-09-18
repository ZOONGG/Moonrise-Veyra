package dev.veyra.client.module.setting.impl;

import com.google.gson.JsonObject;
import dev.veyra.client.clickgui.Component;
import dev.veyra.client.clickgui.components.ModuleComponent;
import dev.veyra.client.module.setting.Setting;

import java.awt.Color;

/** A single persisted HSV color picker value exposed to modules as opaque RGB. */
public final class ColorSetting extends Setting {
    private final int defaultColor;
    private float hue;
    private float saturation;
    private float brightness;

    public ColorSetting(String name, int defaultColor) {
        super(name);
        this.defaultColor = 0xFF000000 | (defaultColor & 0x00FFFFFF);
        setColor(this.defaultColor);
    }

    public int getColor() {
        return 0xFF000000 | (Color.HSBtoRGB(hue, saturation, brightness) & 0x00FFFFFF);
    }

    public String getHex() {
        return String.format("#%06X", getColor() & 0x00FFFFFF);
    }

    public float getHue() {
        return hue;
    }

    public float getSaturation() {
        return saturation;
    }

    public float getBrightness() {
        return brightness;
    }

    public void setHue(float value) {
        hue = clamp(value);
    }

    public void setSaturationAndBrightness(float saturation, float brightness) {
        this.saturation = clamp(saturation);
        this.brightness = clamp(brightness);
    }

    public void setColor(int color) {
        float[] hsb = Color.RGBtoHSB(
                color >> 16 & 255,
                color >> 8 & 255,
                color & 255,
                null);
        hue = hsb[0];
        saturation = hsb[1];
        brightness = hsb[2];
    }

    @Override
    public void resetToDefaults() {
        setColor(defaultColor);
    }

    @Override
    public JsonObject getConfigAsJson() {
        JsonObject data = new JsonObject();
        data.addProperty("type", getSettingType());
        data.addProperty("value", getColor());
        return data;
    }

    @Override
    public String getSettingType() {
        return "color";
    }

    @Override
    public void applyConfigFromJson(JsonObject data) {
        if (data == null || !data.has("type") || !getSettingType().equals(data.get("type").getAsString())) return;
        if (data.has("value")) setColor(data.get("value").getAsInt());
    }

    @Override
    public Component createComponent(ModuleComponent moduleComponent) {
        return null;
    }

    private static float clamp(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}
