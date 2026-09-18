package dev.veyra.client.compat;

import java.util.Locale;

/** Formats a BedWars team name from its current scoreboard color. */
final class BwhBedwarsTeamLabel {
    private BwhBedwarsTeamLabel() {
    }

    static String fromPrefix(String prefix) {
        String color = BwhTeamColorResolver.resolve(prefix);
        if (color == null) return "§fTeam";
        String name = switch (color.charAt(1)) {
            case 'c' -> "Red";
            case '9' -> "Blue";
            case 'a' -> "Green";
            case 'e' -> "Yellow";
            case 'b' -> "Aqua";
            case 'f' -> "White";
            case 'd' -> "Pink";
            case '7', '8' -> "Gray";
            default -> "Team";
        };
        return color + name + " team";
    }

    static String teamKey(String registeredName, String prefix) {
        String color = BwhTeamColorResolver.resolve(prefix);
        if (color != null) return "color:" + color.toLowerCase(Locale.ROOT);
        String normalizedName = registeredName == null
                ? ""
                : registeredName.trim().toLowerCase(Locale.ROOT);
        return normalizedName.isEmpty() ? null : "team:" + normalizedName;
    }
}
