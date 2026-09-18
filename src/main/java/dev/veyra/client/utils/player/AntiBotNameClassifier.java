package dev.veyra.client.utils.player;

import java.util.Locale;

/** Pure name checks for common Hypixel NPC/shop labels. */
public final class AntiBotNameClassifier {
    private AntiBotNameClassifier() {
    }

    public static boolean isKnownNpc(String formattedName, String plainName) {
        String plain = normalize(plainName);
        if (plain.isEmpty()) plain = normalize(stripFormatting(formattedName));
        return plain.contains("[npc]")
                || plain.contains("item shop")
                || plain.contains("team upgrades")
                || plain.contains("solo upgrades")
                || plain.contains("shopkeeper");
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String stripFormatting(String value) {
        if (value == null || value.isEmpty()) return "";
        StringBuilder plain = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            if (value.charAt(index) == '\u00a7' && index + 1 < value.length()) {
                index++;
                continue;
            }
            plain.append(value.charAt(index));
        }
        return plain.toString();
    }
}
