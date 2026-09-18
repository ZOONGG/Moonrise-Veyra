package dev.veyra.client.module.modules.combat;

import dev.veyra.client.main.Veyra;
import dev.veyra.client.module.Module;
import dev.veyra.client.module.modules.client.AntiBot;
import dev.veyra.client.module.setting.impl.DescriptionSetting;
import dev.veyra.client.module.setting.impl.TickSetting;
import dev.veyra.client.utils.player.FriendManager;
import dev.veyra.client.utils.player.PlayerUtils;
import dev.veyra.client.utils.player.TeamDetector;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;
import net.weavemc.api.event.MouseEvent;
import net.weavemc.api.event.SubscribeEvent;

/** Shared player eligibility rules for all combat assistance modules. */
@SuppressWarnings("unused")
public final class TargetFilter extends Module {
    private final TickSetting ignoreTeams;
    private final TickSetting ignoreFriends;
    private final TickSetting ignoreBots;
    private final TickSetting ignoreInvisible;
    private final TickSetting throughWalls;
    private final TickSetting middleClickFriends;

    public TargetFilter() {
        super("TargetFilter", ModuleCategory.Combat, 0);
        defaultEnabled = true;
        registerSetting(new DescriptionSetting("Shared target rules for combat modules."));
        registerSetting(ignoreTeams = new TickSetting("Ignore teams", true));
        registerSetting(ignoreFriends = new TickSetting("Ignore friends", true));
        registerSetting(ignoreBots = new TickSetting("Ignore bots", true));
        registerSetting(ignoreInvisible = new TickSetting("Ignore invisible", true));
        registerSetting(throughWalls = new TickSetting("Through walls", false));
        registerSetting(middleClickFriends = new TickSetting("Middle-click friends", true));
        FriendManager.load();
    }

    public static boolean isValidPlayer(EntityPlayer player) {
        if (!PlayerUtils.isPlayerInGame()
                || player == null
                || player == mc.thePlayer
                || !player.isEntityAlive()
                || player.deathTime != 0) {
            return false;
        }

        TargetFilter filter = getActiveFilter();
        if (filter == null) return true;
        if (filter.ignoreInvisible.isToggled() && player.isInvisible()) return false;
        if (filter.ignoreTeams.isToggled() && TeamDetector.areTeammates(mc.thePlayer, player)) return false;
        if (filter.ignoreFriends.isToggled() && FriendManager.isFriend(player)) return false;
        if (filter.ignoreBots.isToggled() && AntiBot.bot(player)) return false;
        return filter.throughWalls.isToggled() || mc.thePlayer.canEntityBeSeen(player);
    }

    private static TargetFilter getActiveFilter() {
        if (Veyra.moduleManager == null) return null;
        Module module = Veyra.moduleManager.getModuleByClazz(TargetFilter.class);
        return module instanceof TargetFilter filter && filter.isEnabled() ? filter : null;
    }

    @SubscribeEvent
    public void onMiddleClick(MouseEvent event) {
        if (!middleClickFriends.isToggled()
                || !event.getButtonState()
                || event.getButton() != 2
                || !PlayerUtils.isPlayerInGame()
                || mc.currentScreen != null
                || mc.objectMouseOver == null
                || mc.objectMouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.ENTITY
                || !(mc.objectMouseOver.entityHit instanceof EntityPlayer player)
                || player == mc.thePlayer) {
            return;
        }

        boolean friend = FriendManager.toggle(player);
        PlayerUtils.sendMessageToSelf((friend ? "&aAdded " : "&cRemoved ")
                + player.getName() + (friend ? " &aas a friend." : " &cfrom friends."));
    }
}
