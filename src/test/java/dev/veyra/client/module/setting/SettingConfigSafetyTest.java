package dev.veyra.client.module.setting;

import com.google.gson.JsonObject;
import dev.veyra.client.module.setting.impl.ComboSetting;
import dev.veyra.client.module.setting.impl.DoubleSliderSetting;
import dev.veyra.client.module.setting.impl.SliderSetting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SettingConfigSafetyTest {
    @Test
    void slidersReplaceNonFiniteValuesAndClampRanges() {
        SliderSetting single = new SliderSetting("Single", 5.0D, 0.0D, 10.0D, 1.0D);
        single.setValue(Double.NaN);
        assertEquals(5.0D, single.getInput());
        single.setValue(100.0D);
        assertEquals(10.0D, single.getInput());

        DoubleSliderSetting range = new DoubleSliderSetting(
                "Range", 2.0D, 8.0D, 0.0D, 10.0D, 1.0D);
        range.setValueMin(Double.NaN);
        range.setValueMax(Double.POSITIVE_INFINITY);
        assertEquals(2.0D, range.getInputMin());
        assertEquals(8.0D, range.getInputMax());
    }

    @Test
    void comboPersistsStableEnumNameAndDefaultsInvalidValue() {
        ComboSetting<Mode> setting = new ComboSetting<>("Mode", Mode.FIRST);
        setting.setMode(Mode.SECOND);
        assertEquals("SECOND", setting.getConfigAsJson().get("value").getAsString());

        JsonObject invalid = new JsonObject();
        invalid.addProperty("type", "mode");
        invalid.addProperty("value", "removed-value");
        setting.applyConfigFromJson(invalid);
        assertEquals(Mode.FIRST, setting.getMode());
    }

    @Test
    void rangeConfigLoadsIndependentlyOfConstructorDefaults() {
        DoubleSliderSetting range = new DoubleSliderSetting(
                "Range", 75.0D, 125.0D, 0.0D, 500.0D, 5.0D);
        JsonObject data = new JsonObject();
        data.addProperty("type", "doubleslider");
        data.addProperty("valueMin", 0.0D);
        data.addProperty("valueMax", 50.0D);

        range.applyConfigFromJson(data);

        assertEquals(0.0D, range.getInputMin());
        assertEquals(50.0D, range.getInputMax());
    }

    private enum Mode {
        FIRST,
        SECOND
    }
}
