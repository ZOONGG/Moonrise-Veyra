package dev.veyra.client.module.modules.render;

/** Decides which vanilla/Lunar labels Veyra replaces. */
final class NametagRenderPolicy {
    private NametagRenderPolicy() {
    }

    static boolean shouldCancelBaseLabel(boolean playerEntity, boolean localPlayer) {
        return playerEntity && !localPlayer;
    }
}
