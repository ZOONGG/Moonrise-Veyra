package dev.veyra.client.module.bind;

/** Allows state notifications only for transitions caused directly by a bind. */
public final class BindNotificationTracker {
    public boolean transition(boolean enabled, ModuleTransitionSource source) {
        ModuleTransitionSource safeSource = source == null
                ? ModuleTransitionSource.AUTO : source;
        return safeSource.shouldNotify();
    }
}
