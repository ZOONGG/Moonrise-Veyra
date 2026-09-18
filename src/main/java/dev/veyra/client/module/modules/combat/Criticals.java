package dev.veyra.client.module.modules.combat;

import dev.veyra.client.module.Module;
import dev.veyra.client.module.setting.impl.DescriptionSetting;
import dev.veyra.client.utils.player.PlayerUtils;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.weavemc.api.event.PacketEvent;
import net.weavemc.api.event.SubscribeEvent;


@SuppressWarnings("unused")
public class Criticals extends Module {

        public Criticals() {
                super("Criticals", ModuleCategory.Combat, 0);
                this.registerSetting(new DescriptionSetting("Packet criticals"));
        }

        @SubscribeEvent
        public void owie(PacketEvent.Send e) {
                if (!PlayerUtils.isPlayerInGame()) return;
                if (mc.thePlayer.onGround
                        && !mc.thePlayer.isInWater()
                        && !mc.thePlayer.isOnLadder()
                        && e.getPacket() instanceof C02PacketUseEntity attack
                        && attack.getAction() == C02PacketUseEntity.Action.ATTACK) {
                        mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY + 0.07, mc.thePlayer.posZ, false));
                        mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, false));

                }
        }
}
