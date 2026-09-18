package dev.veyra.client.coordination.input;

import net.minecraft.client.settings.KeyBinding;
import net.weavemc.api.event.ShutdownEvent;
import net.weavemc.api.event.SubscribeEvent;
import net.weavemc.api.event.TickEvent;
import net.weavemc.api.event.WorldEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Applies logical leases to Minecraft keys and restores live physical input. */
public final class InputLeaseManager {
    private static final InputLeaseCoordinator COORDINATOR = new InputLeaseCoordinator();
    private static final Map<String, Set<Integer>> KEYS_BY_OWNER = new HashMap<>();

    public static synchronized void set(String owner, int keyCode, boolean pressed, int priority) {
        COORDINATOR.set(owner, keyCode, pressed, priority);
        KEYS_BY_OWNER.computeIfAbsent(owner, ignored -> new HashSet<>()).add(keyCode);
        apply(keyCode);
    }

    public static synchronized void release(String owner, int keyCode) {
        COORDINATOR.release(owner, keyCode);
        Set<Integer> keys = KEYS_BY_OWNER.get(owner);
        if (keys != null) {
            keys.remove(keyCode);
            if (keys.isEmpty()) KEYS_BY_OWNER.remove(owner);
        }
        apply(keyCode);
    }

    public static synchronized void releaseOwner(String owner) {
        releaseOwner(owner, true);
    }

    /**
     * Releases every logical key owned by a module. When physical input is
     * suppressed, the released keys remain logically up until vanilla handles
     * them again. This is required while a GUI is open: Shift must stay an
     * inventory modifier and must not leak into the player's sneak binding.
     */
    public static synchronized void releaseOwner(String owner, boolean restorePhysicalState) {
        Set<Integer> keys = KEYS_BY_OWNER.remove(owner);
        COORDINATOR.releaseOwner(owner);
        if (keys != null) {
            keys.forEach(keyCode -> apply(keyCode, restorePhysicalState));
        }
    }

    public static synchronized boolean owns(String owner, int keyCode) {
        Set<Integer> keys = KEYS_BY_OWNER.get(owner);
        return keys != null && keys.contains(keyCode);
    }

    @SubscribeEvent
    public void onTick(TickEvent.Pre event) {
        refresh();
    }

    @SubscribeEvent
    public void onWorld(WorldEvent event) {
        clearAll();
    }

    @SubscribeEvent
    public void onShutdown(ShutdownEvent event) {
        clearAll();
    }

    private static synchronized void refresh() {
        Set<Integer> keys = new HashSet<>();
        KEYS_BY_OWNER.values().forEach(keys::addAll);
        keys.forEach(InputLeaseManager::apply);
    }

    private static synchronized void clearAll() {
        Set<Integer> keys = new HashSet<>();
        KEYS_BY_OWNER.values().forEach(keys::addAll);
        for (String owner : new HashSet<>(KEYS_BY_OWNER.keySet())) {
            COORDINATOR.releaseOwner(owner);
        }
        KEYS_BY_OWNER.clear();
        keys.forEach(InputLeaseManager::apply);
    }

    private static void apply(int keyCode) {
        apply(keyCode, true);
    }

    private static void apply(int keyCode, boolean restorePhysicalState) {
        KeyBinding.setKeyBindState(keyCode,
                COORDINATOR.resolve(keyCode,
                        restorePhysicalState && physicalState(keyCode)));
    }

    public static boolean physicalState(int keyCode) {
        if (keyCode < 0) {
            int mouseButton = keyCode + 100;
            return mouseButton >= 0 && mouseButton < Mouse.getButtonCount()
                    && Mouse.isButtonDown(mouseButton);
        }
        return keyCode >= 0 && keyCode < Keyboard.KEYBOARD_SIZE
                && Keyboard.isKeyDown(keyCode);
    }

    public InputLeaseManager() {
    }
}
