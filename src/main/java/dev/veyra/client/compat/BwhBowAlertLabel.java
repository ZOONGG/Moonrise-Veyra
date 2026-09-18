package dev.veyra.client.compat;

import java.util.Locale;

/** Formats the three BedWars bow tiers from their visible enchantments. */
final class BwhBowAlertLabel {
    private BwhBowAlertLabel() {
    }

    static String fromLevels(int power, int punch) {
        return fromEvidence(power, punch, 0, null);
    }

    static String fromEvidence(int power, int punch, int enchantmentCount, String itemData) {
        String data = itemData == null ? "" : itemData.toLowerCase(Locale.ROOT);
        if (punch > 0 || data.contains("punch") || enchantmentCount >= 2) return "Punch Bow";
        if (power > 0 || data.contains("power") || enchantmentCount >= 1) return "Power Bow";
        return "Bow";
    }
}
