package dev.veyra.client.compat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses Hypixel's team-upgrade purchase message. */
final class BwhProtectionPurchaseParser {
    private static final Pattern PURCHASE = Pattern.compile(
            "(?:^|\\s)([A-Za-z0-9_]{1,16})\\s+purchased\\s+Reinforced Armor\\s+(I|II|III|IV)(?=\\s|[.!]|$)",
            Pattern.CASE_INSENSITIVE);

    private BwhProtectionPurchaseParser() {
    }

    static Result parse(String message) {
        String plain = (message == null ? "" : message)
                .replaceAll("(?i)\\u00a7[0-9A-FK-OR]", "").trim();
        Matcher matcher = PURCHASE.matcher(plain);
        if (!matcher.find()) return new Result(false, "", 0);
        return new Result(true, matcher.group(1),
                level(matcher.group(2).toUpperCase(java.util.Locale.ROOT)));
    }

    private static int level(String roman) {
        return switch (roman) {
            case "I" -> 1;
            case "II" -> 2;
            case "III" -> 3;
            case "IV" -> 4;
            default -> 0;
        };
    }

    record Result(boolean matched, String purchaser, int level) {
    }
}
