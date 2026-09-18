package dev.veyra.client.module.bind;

public enum BindMode {
    TOGGLE,
    HOLD,
    PRESS;

    public static BindMode fromConfig(String value, BindMode fallback) {
        BindMode safeFallback = fallback == null ? TOGGLE : fallback;
        if (value == null || value.isBlank()) return safeFallback;
        try {
            return valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return safeFallback;
        }
    }
}
