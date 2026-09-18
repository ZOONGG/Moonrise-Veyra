package dev.veyra.client.module.modules.render;

import java.util.Locale;

/** Recognizes moments when Hypixel has placed the player at their own base. */
final class BedwarsBaseAnchorTrigger {
    private BedwarsBaseAnchorTrigger() {
    }

    static boolean matches(String message) {
        if (message == null) return false;
        String normalized = message.trim().toLowerCase(Locale.ROOT);
        return normalized.contains("protect your bed and destroy the enemy beds")
                || normalized.contains("you have respawned");
    }
}
