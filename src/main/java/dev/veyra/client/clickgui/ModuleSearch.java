package dev.veyra.client.clickgui;

import java.util.Locale;

/** Search normalization, including text entered while the Russian layout is active. */
public final class ModuleSearch {
    private static final String RU_LAYOUT = "йцукенгшщзхъфывапролджэячсмитьбю";
    private static final String EN_LAYOUT = "qwertyuiop[]asdfghjkl;'zxcvbnm,.";

    private ModuleSearch() {
    }

    public static boolean matches(String source, String query) {
        String haystack = normalize(source);
        String needle = normalize(query);
        if (needle.isEmpty()) return true;
        if (containsWithOptionalSpaces(haystack, needle)) return true;
        String layoutCorrected = normalize(toEnglishLayout(query));
        return !layoutCorrected.equals(needle)
                && containsWithOptionalSpaces(haystack, layoutCorrected);
    }

    static String toEnglishLayout(String value) {
        if (value == null || value.isEmpty()) return "";
        String lower = value.toLowerCase(Locale.ROOT);
        StringBuilder corrected = new StringBuilder(lower.length());
        for (int index = 0; index < lower.length(); index++) {
            char character = lower.charAt(index);
            int mapped = RU_LAYOUT.indexOf(character);
            corrected.append(mapped >= 0 ? EN_LAYOUT.charAt(mapped) : character);
        }
        return corrected.toString();
    }

    private static boolean containsWithOptionalSpaces(String haystack, String needle) {
        return haystack.contains(needle)
                || haystack.replace(" ", "").contains(needle.replace(" ", ""));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim()
                .replaceAll("\\s+", " ");
    }
}
