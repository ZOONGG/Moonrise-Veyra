package dev.veyra.client.module.setting.impl;

import com.google.gson.JsonObject;
import dev.veyra.client.clickgui.Component;
import dev.veyra.client.clickgui.components.ModuleComponent;
import dev.veyra.client.module.setting.Setting;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class DoubleSliderSetting extends Setting {
    private final String name;
    private double valMax, valMin;
    private final double max;
    private final double min;
    private final double interval;

    private final double defaultValMin, defaultValMax;

    public DoubleSliderSetting(String settingName, double defaultValueMin, double defaultValueMax, double min, double max, double intervals) {
        super(settingName);
        this.name = settingName;
        this.valMin = defaultValueMin;
        this.valMax = defaultValueMax;
        this.min = min;
        this.max = max;
        this.interval = intervals;
        this.defaultValMin = valMin;
        this.defaultValMax = valMax;
    }

    public String getName() {
        return this.name;
    }

    @Override
    public void resetToDefaults() {
        this.setValueMin(defaultValMin);
        this.setValueMax(defaultValMax);
    }

    @Override
    public JsonObject getConfigAsJson() {
        JsonObject data = new JsonObject();
        data.addProperty("type", getSettingType());
        data.addProperty("valueMin", getInputMin());
        data.addProperty("valueMax", getInputMax());
        return data;
    }

    @Override
    public String getSettingType() {
        return "doubleslider";
    }

    @Override
    public void applyConfigFromJson(JsonObject data) {
        if(!data.get("type").getAsString().equals(getSettingType()))
            return;

        setRange(data.get("valueMin").getAsDouble(),
                data.get("valueMax").getAsDouble());
    }

    @Override
    public Component createComponent(ModuleComponent moduleComponent) {
        return null;
    }

    public double getInputMin() {
        return round(this.valMin, 2);
    }
    public double getInputMax() {
        return round(this.valMax, 2);
    }

    public double getMin() {
        return this.min;
    }

    public double getMax() {
        return this.max;
    }

    public void setValueMin(double n) {
        if (!Double.isFinite(n)) n = defaultValMin;
        n = correct(n, this.min, this.valMax);
        n = (double)Math.round(n * (1.0D / this.interval)) / (1.0D / this.interval);
        this.valMin = n;
    }

    public void setValueMax(double n) {
        if (!Double.isFinite(n)) n = defaultValMax;
        n = correct(n, this.valMin, this.max);
        n = (double)Math.round(n * (1.0D / this.interval)) / (1.0D / this.interval);
        this.valMax = n;
    }

    public void setRange(double requestedMin, double requestedMax) {
        if (!Double.isFinite(requestedMin)) requestedMin = defaultValMin;
        if (!Double.isFinite(requestedMax)) requestedMax = defaultValMax;
        double lower = Math.min(requestedMin, requestedMax);
        double upper = Math.max(requestedMin, requestedMax);
        this.valMin = quantize(correct(lower, min, max));
        this.valMax = quantize(correct(upper, min, max));
    }

    private double quantize(double value) {
        return (double) Math.round(value * (1.0D / interval)) / (1.0D / interval);
    }

    public static double correct(double val, double min, double max) {
        val = Math.max(min, val);
        val = Math.min(max, val);
        return val;
    }

    public static double round(double val, int p) {
        if (p < 0) {
            return 0.0D;
        } else {
            BigDecimal bd = new BigDecimal(val);
            bd = bd.setScale(p, RoundingMode.HALF_UP);
            return bd.doubleValue();
        }
    }
}
