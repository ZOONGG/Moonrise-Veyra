package dev.veyra.client.compat;

/** Builds an overlay label from the name actually visible in the current game. */
final class BwhVisibleOverlayName {
    private BwhVisibleOverlayName() {
    }

    static String resolve(String visibleName, String teamPrefix, String formattedTabName) {
        if (visibleName == null || visibleName.isBlank()) return null;
        String color = BwhTeamColorResolver.resolve(teamPrefix);
        if (color == null) color = BwhTeamColorResolver.resolve(formattedTabName);
        if (color == null) color = "§f";
        return color + visibleName;
    }
}
