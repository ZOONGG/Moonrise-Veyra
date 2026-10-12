package dev.veyra.client.module.modules.player;

import dev.veyra.client.module.Module;

import dev.veyra.client.module.setting.impl.DescriptionSetting;


import dev.veyra.client.player.clutch.VoidClutchEngine;

import dev.veyra.weave.events.PacketEvent;
import dev.veyra.weave.events.EventDirection;
import net.minecraft.client.gui.GuiGameOver;
import net.minecraft.network.play.server.S06PacketUpdateHealth;
import net.minecraft.network.play.server.S07PacketRespawn;
import net.minecraft.network.play.server.S40PacketDisconnect;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.weavemc.api.event.*;

/** Event subscription exists only while a bind-triggered sequence is running. */
public final class VoidClutch extends Module {
    private EntityPlayerSP player;
    private WorldClient world;
    private final dev.veyra.client.player.clutch.ClutchInput input = new dev.veyra.client.player.clutch.ClutchInput();
    private long clientTick;
    private int lastPlayerTicks;
    private volatile boolean lifecycleInvalidated;
    private final VoidClutchEngine engine = new VoidClutchEngine(new VoidClutchEngine.Actions() {
        public boolean valid() {
            if (player != null) {
                if (player.ticksExisted < lastPlayerTicks) lifecycleInvalidated = true;
                lastPlayerTicks = player.ticksExisted;
            }
            return !lifecycleInvalidated && !(mc.currentScreen instanceof GuiGameOver) && mc.currentScreen == null && mc.inGameHasFocus && !mc.gameSettings.smoothCamera
                    && mc.thePlayer != null && mc.theWorld != null && mc.playerController != null
                    && mc.getNetHandler() != null && !mc.thePlayer.isDead && mc.thePlayer.getHealth() > 0
                    && (player == null || player == mc.thePlayer && world == mc.theWorld);
        }
        public int pearlSlot() { return find(Items.ender_pearl); }
        public int fireballSlot() { return find(Items.fire_charge); }
        public void missing(String item) {
            mc.thePlayer.addChatMessage(new ChatComponentText("VoidClutch: " + item + " not found"));
        }
        public void capture() {
            player = mc.thePlayer;
            world = mc.theWorld;
            lastPlayerTicks = player.ticksExisted;
        }
        public void beginAim(java.util.function.LongSupplier delta) { dev.veyra.client.player.clutch.ClutchInput.beginRawAim(delta); }
        public int upwardMouseCounts() {
            return dev.veyra.client.player.clutch.NativeMouseDelta.fullUpwardCounts(
                    player.rotationPitch, mc.gameSettings.mouseSensitivity, mc.gameSettings.invertMouse);
        }
        public void clearAim() { dev.veyra.client.player.clutch.ClutchInput.clearAim(); }
        public void select(int slot) { input.tapHotbar(mc, slot); }
        public void use(int slot, boolean pearl) {
            ItemStack stack = player.inventory.getStackInSlot(slot);
            Item expected = pearl ? Items.ender_pearl : Items.fire_charge;
            if (stack == null || stack.stackSize <= 0 || stack.getItem() != expected)
                throw new IllegalStateException("Required hotbar item changed");
            if (player.inventory.currentItem != slot) throw new IllegalStateException("Selected hotbar slot changed");
            input.use(mc);
        }
        public void cleanup() {
            input.clearSlot();
            player = null; world = null;
        }
        public void traceMouse(long tick, long elapsed, int delta) {
            System.out.printf("%d | %d | AimInput | %d | %.3f | %d | mouse-input%n", elapsed / 1_000_000, clientTick, -delta, mc.thePlayer == null ? 0 : mc.thePlayer.rotationPitch, mc.thePlayer == null ? -1 : mc.thePlayer.inventory.currentItem + 1);
        }
        public void trace(long tick, long elapsed, String state, String action) {

                System.out.printf("%d | %d | %s | %d | %.3f | %d | %s%n", elapsed / 1_000_000, clientTick, state, 0, mc.thePlayer == null ? 0 : mc.thePlayer.rotationPitch, mc.thePlayer == null ? -1 : mc.thePlayer.inventory.currentItem + 1, action);
        }
    });

    public VoidClutch() {
        super("VoidClutch", ModuleCategory.Player, 0);
        registerSetting(new DescriptionSetting("Bind only: full upward mouse aim / 50 ms, Pearl slot / 50 ms / RMB, 30 ms / Fireball slot / 30 ms / RMB."));
    }
    @Override public boolean isBindOnly() { return true; }
    @Override public boolean supportsHoldBind() { return false; }
    @Override public boolean supportsPressBind() { return true; }
    @Override public void onBindPressed() {
        if (engine.isActive()) return;
        lifecycleInvalidated = false;
        EventBus.subscribe(this);
        try { engine.start(); }
        catch (RuntimeException error) { reportFailure(); }
        finally { if (!engine.isActive()) EventBus.unsubscribe(this); }
    }
    // Network callbacks only invalidate; input cleanup stays on the client thread.
    @SubscribeEvent public void onPacket(PacketEvent event) {
        if (event.getDirection() != EventDirection.INCOMING) return;
        if (event.getPacket() instanceof S07PacketRespawn
                || event.getPacket() instanceof S40PacketDisconnect
                || event.getPacket() instanceof S06PacketUpdateHealth health && health.getHealth() <= 0)
            lifecycleInvalidated = true;
    }
    public void afterTimerUpdate() { advance(); }
    @SubscribeEvent public void onTick(TickEvent.Pre event) {
        try { engine.tick(++clientTick, System.nanoTime()); }
        catch (RuntimeException error) { reportFailure(); }
        finally { if (!engine.isActive()) EventBus.unsubscribe(this); }
    }
    // Frame callbacks only validate lifecycle; all actions require client ticks.
    @SubscribeEvent public void onFrame(RenderGameOverlayEvent.Pre event) { advance(); }
    @SubscribeEvent public void onWorld(WorldEvent event) { cancel(); }
    @SubscribeEvent public void onShutdown(ShutdownEvent event) { cancel(); }
    @Override public void onDisable() { cancel(); }

    private void advance() {
        try { engine.advance(System.nanoTime()); }
        catch (RuntimeException error) { reportFailure(); }
        finally { if (!engine.isActive()) EventBus.unsubscribe(this); }
    }
    private void cancel() {
        try { engine.cancel(); }
        finally { EventBus.unsubscribe(this); }
    }
    private void reportFailure() {
        cancel();
        if (mc.thePlayer != null) mc.thePlayer.addChatMessage(new ChatComponentText("VoidClutch: sequence cancelled"));
    }
    private int find(Item item) {
        return VoidClutchEngine.findHotbar(slot -> {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(slot);
            return stack != null && stack.stackSize > 0 && stack.getItem() == item;
        });
    }
}
