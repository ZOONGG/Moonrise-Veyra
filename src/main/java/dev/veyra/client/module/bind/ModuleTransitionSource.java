package dev.veyra.client.module.bind;

public enum ModuleTransitionSource {
    KEYBIND,
    GUI,
    CONFIG,
    AUTO;

    public boolean shouldNotify() {
        return this == KEYBIND;
    }
}
