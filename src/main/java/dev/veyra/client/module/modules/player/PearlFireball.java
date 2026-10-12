package dev.veyra.client.module.modules.player;

import dev.veyra.client.module.Module;
import dev.veyra.client.module.setting.impl.DescriptionSetting;
import dev.veyra.client.module.setting.impl.SliderSetting;
import dev.veyra.client.player.clutch.VoidClutchEngine;
import dev.veyra.client.player.clutch.PearlFireballEngine;
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
public final class PearlFireball extends Module {
    private final SliderSetting throwDelay = new SliderSetting("Throw Delay",
            VoidClutchEngine.DEFAULT_DELAY_MS, VoidClutchEngine.MIN_DELAY_MS,
            VoidClutchEngine.MAX_DELAY_MS, 25);
    private EntityPlayerSP player;
    private WorldClient world;
    private final dev.veyra.client.player.clutch.ClutchInput input = new dev.veyra.client.player.clutch.ClutchInput();
    private long clientTick;
    private int lastPlayerTicks;
    private volatile boolean lifecycleInvalidated;
    private final PearlFireballEngine engine = new PearlFireballEngine(new PearlFireballEngine.Actions() {
        public boolean valid() {
            if (player != null) {
                if (player.ticksExisted < lastPlayerTicks) lifecycleInvalidated = true;
                lastPlayerTicks = player.ticksExisted;
            }
            return !lifecycleInvalidated && !(mc.currentScreen instanceof GuiGameOver)
                    && mc.currentScreen == null && mc.inGameHasFocus
                    && mc.thePlayer != null && mc.theWorld != null && mc.playerController != null
                    && mc.getNetHandler() != null && !mc.thePlayer.isDead && mc.thePlayer.getHealth() > 0
                    && (player == null || player == mc.thePlayer && world == mc.theWorld);
        }
        public int pearlSlot() { return find(Items.ender_pearl); }
        public int fireballSlot() { return find(Items.fire_charge); }
        public void missing(String item) {
            mc.thePlayer.addChatMessage(new ChatComponentText("PearlFireball: " + item + " not found"));
        }
        public void capture() {
            player = mc.thePlayer;
            world = mc.theWorld;
            lastPlayerTicks = player.ticksExisted;
        }
        public void select(int slot) { input.select(mc, slot); }
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
        public void trace(long tick, long elapsed, String state, String action) {
            if (Boolean.getBoolean("veyra.clutch.debug"))
                System.out.printf("%s tick=%d elapsedMs=%d state=%s mouseDelta=%d pitch=%.3f slot=%d item=%s action=%s source=client-tick/pre-or-lifecycle nano=%d%n",
                    getClass().getEnclosingClass().getSimpleName(), tick, elapsed / 1_000_000,
                    state, dev.veyra.client.player.clutch.ClutchInput.lastDelta(),
                    mc.thePlayer == null ? 0 : mc.thePlayer.rotationPitch,
                    mc.thePlayer == null ? -1 : mc.thePlayer.inventory.currentItem,
                    mc.thePlayer == null || mc.thePlayer.getHeldItem() == null ? "empty" : mc.thePlayer.getHeldItem().getItem().getUnlocalizedName(), action, System.nanoTime());
        }
    });

    public PearlFireball() {
        super("PearlFireball", ModuleCategory.Player, 0);
        registerSetting(new DescriptionSetting("Aim manually, then press bind: Pearl / Fireball. Throw Delay in ms, with slight variation."));
        registerSetting(throwDelay);
    }
    @Override public boolean isBindOnly() { return true; }
    @Override public boolean supportsHoldBind() { return false; }
    @Override public boolean supportsPressBind() { return true; }
    @Override public void onBindPressed() {
        if (engine.isActive()) return;
        lifecycleInvalidated = false;
        EventBus.subscribe(this);
        try { engine.start((int) throwDelay.getInput()); }
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
    /** Lifecycle validation continues even when client ticks are paused. */
    public void updateSequence() { if (engine.isActive()) advance(); }
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
        if (mc.thePlayer != null) mc.thePlayer.addChatMessage(new ChatComponentText("PearlFireball: sequence cancelled"));
    }
    private int find(Item item) {
        return VoidClutchEngine.findHotbar(slot -> {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(slot);
            return stack != null && stack.stackSize > 0 && stack.getItem() == item;
        });
    }
}
