package dev.veyra.client.combat.block;

import java.util.List;

public record AutoBlockStep(List<AutoBlockAction> actions) {
    private static final AutoBlockStep EMPTY = new AutoBlockStep(List.of());

    public AutoBlockStep {
        actions = List.copyOf(actions);
    }

    public static AutoBlockStep empty() {
        return EMPTY;
    }

    public static AutoBlockStep of(AutoBlockAction... actions) {
        return actions.length == 0 ? EMPTY : new AutoBlockStep(List.of(actions));
    }

    public boolean contains(AutoBlockAction action) {
        return actions.contains(action);
    }

    public boolean isEmpty() {
        return actions.isEmpty();
    }
}
