package dev.veyra.client.module.modules.movement;

import dev.veyra.client.module.setting.impl.DescriptionSetting;
import dev.veyra.client.module.setting.impl.TickSetting;
import dev.veyra.client.utils.player.PlayerUtils;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.weavemc.api.event.PacketEvent;
import net.weavemc.api.event.TickEvent;
import net.weavemc.api.event.SubscribeEvent;
import dev.veyra.client.module.Module;

public class Sprint extends Module {

    private static final int VANILLA_KNOCKBACK_TICKS = 12;

    public final TickSetting Omni;
    private volatile int vanillaControlUntilTick = Integer.MIN_VALUE;

    public Sprint() {
        super("Sprint", ModuleCategory.Movement, 0);
        this.registerSetting(new DescriptionSetting("Automatically sprint."));
        this.registerSetting(Omni = new TickSetting("Omni", false));
    }

    /** Lets input-driven modules finish a vanilla sprint transition. */
    public void pauseAutomationUntilTick(int tick) {
        vanillaControlUntilTick = Math.max(vanillaControlUntilTick, tick);
    }

    @SuppressWarnings("unused")
    @SubscribeEvent
    public void onVelocity(PacketEvent.Receive event) {
        if (!PlayerUtils.isPlayerInGame()
                || !(event.getPacket() instanceof S12PacketEntityVelocity packet)
                || packet.getEntityID() != mc.thePlayer.getEntityId()) {
            return;
        }

        // Never mutate/cancel the server impulse. Auto-sprint must also stay
        // out of the way while vanilla applies hit slowdown; immediately
        // re-sprinting during this window repeatedly reduces local motion and
        // makes Hypixel correct the resulting client/server position drift.
        pauseAutomationUntilTick(mc.thePlayer.ticksExisted + VANILLA_KNOCKBACK_TICKS);
    }

    @SuppressWarnings("unused")
    @SubscribeEvent
    public void autoSprint(TickEvent.Pre event) {
        if (!PlayerUtils.isPlayerInGame()) return;
        if (mc.thePlayer.hurtTime > 0
                || mc.thePlayer.ticksExisted <= vanillaControlUntilTick) {
            return;
        }

        boolean moving = Omni.isToggled()
                ? mc.thePlayer.moveForward != 0.0F || mc.thePlayer.moveStrafing != 0.0F
                : mc.thePlayer.moveForward > 0.0F;
        mc.thePlayer.setSprinting(moving && !mc.thePlayer.isSneaking());
    }

    @Override
    public void onEnable() {
        vanillaControlUntilTick = Integer.MIN_VALUE;
    }

    @Override
    public void onDisable() {
        vanillaControlUntilTick = Integer.MIN_VALUE;
    }
}
