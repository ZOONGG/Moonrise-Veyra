package dev.veyra.client.module.modules.player;

import dev.veyra.client.module.Module;
import dev.veyra.client.coordination.input.InputLeaseManager;
import dev.veyra.client.module.setting.impl.DescriptionSetting;
import dev.veyra.client.module.setting.impl.SliderSetting;
import dev.veyra.client.utils.player.PlayerUtils;
import net.weavemc.api.event.SubscribeEvent;
import net.weavemc.api.event.TickEvent;

public class AntiAFK extends Module {
    private static final String INPUT_OWNER = "anti-afk";
    public static SliderSetting intervalSeconds;
    private long nextActionAt;
    private int actionTicks;
    private boolean forwardPhase;

    public AntiAFK() {
        super("Anti-AFK", ModuleCategory.Player, 0);
        registerSetting(new DescriptionSetting("Makes a tiny forward/back movement while idle."));
        registerSetting(intervalSeconds = new SliderSetting("Interval seconds", 45, 10, 180, 5));
    }

    @Override
    public void onEnable() {
        scheduleNext();
        actionTicks = 0;
    }

    @Override
    public void onDisable() {
        releaseKeys();
        setSuffix(null);
    }

    @SubscribeEvent
    public void onTick(TickEvent.Pre event) {
        if (!PlayerUtils.isPlayerInGame()) return;
        long now = System.currentTimeMillis();
        setSuffix(Math.max(0L, (nextActionAt - now + 999L) / 1000L) + "s");

        if (actionTicks > 0) {
            actionTicks--;
            if (actionTicks == 2) {
                InputLeaseManager.set(INPUT_OWNER,
                        mc.gameSettings.keyBindForward.getKeyCode(), false, 5);
                InputLeaseManager.set(INPUT_OWNER,
                        mc.gameSettings.keyBindBack.getKeyCode(), true, 5);
            } else if (actionTicks == 0) {
                releaseKeys();
                scheduleNext();
            }
            return;
        }

        boolean manuallyMoving = mc.gameSettings.keyBindForward.isKeyDown()
                || mc.gameSettings.keyBindBack.isKeyDown()
                || mc.gameSettings.keyBindLeft.isKeyDown()
                || mc.gameSettings.keyBindRight.isKeyDown();
        if (now >= nextActionAt && !manuallyMoving && mc.currentScreen == null && mc.thePlayer.onGround) {
            forwardPhase = !forwardPhase;
            mc.thePlayer.rotationYaw += forwardPhase ? 0.6F : -0.6F;
            InputLeaseManager.set(INPUT_OWNER,
                    mc.gameSettings.keyBindForward.getKeyCode(), true, 5);
            actionTicks = 4;
        }
    }

    private void scheduleNext() {
        nextActionAt = System.currentTimeMillis() + (long) (intervalSeconds.getInput() * 1000.0D);
    }

    private void releaseKeys() {
        InputLeaseManager.releaseOwner(INPUT_OWNER);
    }
}
