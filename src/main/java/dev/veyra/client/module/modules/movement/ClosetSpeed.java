package dev.veyra.client.module.modules.movement;

import dev.veyra.client.coordination.input.InputLeaseManager;
import net.weavemc.api.event.SubscribeEvent;
import net.weavemc.api.event.TickEvent;
import dev.veyra.client.module.Module;
import dev.veyra.client.module.setting.impl.DescriptionSetting;
import dev.veyra.client.module.setting.impl.TickSetting;

public class ClosetSpeed extends Module {
    private static final String INPUT_OWNER = "closet-speed";

    public static TickSetting jump;

    public ClosetSpeed() {
        super("ClosetSpeed", ModuleCategory.Movement, 0);
        this.registerSetting(new DescriptionSetting("For more minor speed cheats."));
        this.registerSetting(jump = new TickSetting("Hold Jump", true));
    }

    @SubscribeEvent
    public void legitJump(TickEvent e) {
        if (mc.thePlayer != null) {
            if (jump.isToggled() && !mc.thePlayer.isSneaking())  {
                if (mc.gameSettings.keyBindForward.isKeyDown() || mc.gameSettings.keyBindBack.isKeyDown() ||mc.gameSettings.keyBindLeft.isKeyDown() || mc.gameSettings.keyBindRight.isKeyDown()) {
                    InputLeaseManager.set(INPUT_OWNER,
                            mc.gameSettings.keyBindJump.getKeyCode(), true, 10);
                } else {
                    InputLeaseManager.releaseOwner(INPUT_OWNER);
                }
            } else {
                InputLeaseManager.releaseOwner(INPUT_OWNER);
            }
        }
    }

    @Override
    public void onDisable() {
        InputLeaseManager.releaseOwner(INPUT_OWNER);
    }
}
