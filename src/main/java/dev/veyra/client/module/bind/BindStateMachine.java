package dev.veyra.client.module.bind;

public final class BindStateMachine {
    private boolean previouslyDown;

    public BindAction update(BindMode mode, boolean down) {
        if (down == previouslyDown) return BindAction.NONE;
        previouslyDown = down;

        if (!down) {
            return mode == BindMode.HOLD ? BindAction.DISABLE : BindAction.NONE;
        }

        return switch (mode) {
            case TOGGLE -> BindAction.TOGGLE;
            case HOLD -> BindAction.ENABLE;
            case PRESS -> BindAction.PRESS;
        };
    }

    public BindAction reset(BindMode mode) {
        boolean wasDown = previouslyDown;
        previouslyDown = false;
        return wasDown && mode == BindMode.HOLD ? BindAction.DISABLE : BindAction.NONE;
    }
}
