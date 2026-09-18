package dev.veyra.client.module.modules.player;

import dev.veyra.client.module.Module;
import dev.veyra.client.module.setting.impl.DescriptionSetting;
import dev.veyra.client.module.setting.impl.TickSetting;
import dev.veyra.client.utils.player.PlayerUtils;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.weavemc.api.event.SubscribeEvent;
import net.weavemc.api.event.TickEvent;
import net.weavemc.api.event.WorldEvent;

@SuppressWarnings("unused")
public class AutoDodge extends Module {
    public static TickSetting notify;
    public boolean shouldNotify = false;

    public AutoDodge() {
        super("AutoDodge", ModuleCategory.Player, 0);
        this.registerSetting(new DescriptionSetting("Reports scoreboard teams after joining."));
        this.registerSetting(notify = new TickSetting("Show team report", true));
    }

    @SubscribeEvent
    public void checkTeamsExist(TickEvent e) {
        if (notify.isToggled() && shouldNotify && PlayerUtils.isPlayerInGame()) {
            for (ScorePlayerTeam team : mc.theWorld.getScoreboard().getTeams()) {
                String teamName = team.getRegisteredName();
                mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.DARK_PURPLE + "Veyra: " + EnumChatFormatting.AQUA + "Team: " + teamName));
                shouldNotify = false;
            }
        }
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent e) {
        shouldNotify = true;
    }
}
