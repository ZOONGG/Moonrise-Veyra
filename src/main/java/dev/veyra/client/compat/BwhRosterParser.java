package dev.veyra.client.compat;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** Parses Hypixel's response to /who without depending on BWH classes. */
public final class BwhRosterParser {
    private static final String PREFIX = "ONLINE:";
    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9_]{1,16}");

    private BwhRosterParser() {
    }

    public static Result parse(String message) {
        String plain = stripFormatting(message == null ? "" : message).trim();
        if (!plain.toUpperCase(Locale.ROOT).startsWith(PREFIX)) {
            return new Result(false, Set.of());
        }

        LinkedHashSet<String> players = new LinkedHashSet<>();
        String roster = plain.substring(PREFIX.length()).trim();
        if (!roster.isEmpty()) {
            for (String part : roster.split(",")) {
                String name = part.trim();
                if (USERNAME.matcher(name).matches()) players.add(name);
            }
        }
        return new Result(true, players);
    }

    private static String stripFormatting(String value) {
        return value.replaceAll("(?i)\\u00a7[0-9A-FK-OR]", "");
    }

    public record Result(boolean matched, Set<String> players) {
    }
}
