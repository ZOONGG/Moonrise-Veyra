package dev.veyra.client.compat;

import java.util.Locale;

/** Resolves the final active legacy color from a scoreboard-team prefix. */
final class BwhTeamColorResolver {
    private static final String LEGACY_COLORS = "0123456789abcdef";

    private BwhTeamColorResolver() {
    }

    static String resolve(String teamPrefix) {
        if (teamPrefix == null) return null;
        String normalized = teamPrefix.toLowerCase(Locale.ROOT);
        String color = null;
        for (int index = 0; index + 1 < normalized.length(); index++) {
            char marker = normalized.charAt(index);
            char code = normalized.charAt(index + 1);
            if ((marker == '\u00a7' || marker == '&')
                    && LEGACY_COLORS.indexOf(code) >= 0) {
                color = "\u00a7" + code;
                index++;
            }
        }
        return color;
    }
}
