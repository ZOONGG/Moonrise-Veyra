package dev.veyra.client.utils.player;

import java.util.Locale;

/** Pure scoreboard/name matching used by the in-game team detector. */
public final class TeamMatcher {
    private TeamMatcher() {
    }

    public static boolean sameRegisteredTeam(String selfTeam, String targetTeam) {
        String self = normalizeTeam(selfTeam);
        String target = normalizeTeam(targetTeam);
        return !self.isEmpty() && self.equals(target);
    }

    public static Character effectiveColor(String teamPrefix, String displayName, String playerName) {
        Character prefixColor = lastColor(teamPrefix, teamPrefix == null ? 0 : teamPrefix.length());
        if (prefixColor != null) return prefixColor;

        if (displayName == null || displayName.isEmpty()) return null;
        int nameAt = playerName == null || playerName.isEmpty()
                ? displayName.length()
                : displayName.toLowerCase(Locale.ROOT).lastIndexOf(playerName.toLowerCase(Locale.ROOT));
        return lastColor(displayName, nameAt < 0 ? displayName.length() : nameAt);
    }

    public static boolean sameEffectiveColor(
            String selfPrefix, String selfDisplay, String selfName,
            String targetPrefix, String targetDisplay, String targetName) {
        Character selfColor = effectiveColor(selfPrefix, selfDisplay, selfName);
        Character targetColor = effectiveColor(targetPrefix, targetDisplay, targetName);
        return selfColor != null && selfColor.equals(targetColor);
    }

    private static Character lastColor(String text, int before) {
        if (text == null) return null;
        for (int index = Math.min(before, text.length()) - 2; index >= 0; index--) {
            if (text.charAt(index) != '\u00a7') continue;
            char code = Character.toLowerCase(text.charAt(index + 1));
            if ((code >= '0' && code <= '9') || (code >= 'a' && code <= 'f')) return code;
        }
        return null;
    }

    private static String normalizeTeam(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
