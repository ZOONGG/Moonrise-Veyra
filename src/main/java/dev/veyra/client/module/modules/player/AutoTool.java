package dev.veyra.client.module.modules.player;

import dev.veyra.client.module.Module;
import dev.veyra.client.module.setting.impl.DescriptionSetting;
import dev.veyra.client.module.setting.impl.SliderSetting;
import dev.veyra.client.module.setting.impl.TickSetting;
import dev.veyra.client.utils.world.BlockInteractionUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.weavemc.api.event.SubscribeEvent;
import net.weavemc.api.event.TickEvent;

/** Selects the fastest tool and only restores a slot the module actually changed. */
public final class AutoTool extends Module {
    public static SliderSetting activationTime;
    public static TickSetting switchBack;

    private BlockPos miningTarget;
    private int previousSlot = -1;
    private int selectedToolSlot = -1;
    private long targetAcquiredAt;
    private boolean ownsSwitch;

    public AutoTool() {
        super("AutoTool", ModuleCategory.Player, 0);
        registerSetting(new DescriptionSetting("Selects the fastest hotbar tool while mining."));
        registerSetting(activationTime = new SliderSetting("Switch delay ms", 0, 0, 500, 25));
        registerSetting(switchBack = new TickSetting("Switch back", true));
    }

    @SubscribeEvent
    public void onTick(TickEvent.Pre event) {
        if (!isMiningBlock()) {
            finishMining(true);
            return;
        }

        // A manual scroll or number-key press takes ownership away from AutoTool.
        if (ownsSwitch && mc.thePlayer.inventory.currentItem != selectedToolSlot) {
            clearOwnership();
        }

        BlockPos target = mc.objectMouseOver.getBlockPos();
        long now = System.currentTimeMillis();
        if (!target.equals(miningTarget)) {
            finishMining(true);
            miningTarget = target;
            targetAcquiredAt = now;
        }
        if (now - targetAcquiredAt < (long) activationTime.getInput()) {
            setSuffix("Wait");
            return;
        }

        int bestSlot = BlockInteractionUtils.findBestToolSlot(target);
        if (bestSlot < 0) return;
        int currentSlot = mc.thePlayer.inventory.currentItem;
        if (bestSlot != currentSlot) {
            if (!ownsSwitch) previousSlot = currentSlot;
            setSlot(bestSlot);
            selectedToolSlot = bestSlot;
            ownsSwitch = true;
        }

        ItemStack selected = mc.thePlayer.inventory.getStackInSlot(bestSlot);
        setSuffix(selected == null ? "Hand" : selected.getDisplayName());
    }

    @Override
    public void onDisable() {
        finishMining(true);
    }

    private boolean isMiningBlock() {
        return mc.thePlayer != null
                && mc.theWorld != null
                && mc.currentScreen == null
                && mc.gameSettings.keyBindAttack.isKeyDown()
                && mc.objectMouseOver != null
                && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
                && mc.objectMouseOver.getBlockPos() != null;
    }

    private void finishMining(boolean allowRestore) {
        if (allowRestore && ownsSwitch && switchBack.isToggled() && previousSlot >= 0 && mc.thePlayer != null
                && mc.thePlayer.inventory.currentItem == selectedToolSlot) {
            setSlot(previousSlot);
        }
        miningTarget = null;
        targetAcquiredAt = 0L;
        clearOwnership();
        setSuffix(null);
    }

    private void clearOwnership() {
        ownsSwitch = false;
        previousSlot = -1;
        selectedToolSlot = -1;
    }

    private void setSlot(int slot) {
        if (mc.thePlayer == null || slot < 0 || slot > 8) return;
        mc.thePlayer.inventory.currentItem = slot;
        mc.playerController.updateController();
        mc.getItemRenderer().resetEquippedProgress();
    }
}
