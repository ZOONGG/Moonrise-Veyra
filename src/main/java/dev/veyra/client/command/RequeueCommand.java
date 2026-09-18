package dev.veyra.client.command;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.weavemc.api.command.Command;
import net.weavemc.api.event.ChatEvent;
import net.weavemc.api.event.SubscribeEvent;

/** Requeues the player's current Hypixel BedWars mode through /locraw. */
public final class RequeueCommand extends Command {
    private static final long REQUEST_TIMEOUT_NANOS = 5_000_000_000L;

    private long pendingUntil;

    public RequeueCommand() {
        super("rq");
    }

    @Override
    public void execute(String[] arguments) {
        if (arguments.length > 1) {
            message("Использование: /rq");
            return;
        }
        if (Minecraft.getMinecraft().thePlayer == null) {
            return;
        }

        pendingUntil = System.nanoTime() + REQUEST_TIMEOUT_NANOS;
        Minecraft.getMinecraft().thePlayer.sendChatMessage("/locraw");
    }

    @SubscribeEvent
    public void onChatReceived(ChatEvent.Received event) {
        if (pendingUntil == 0L || System.nanoTime() > pendingUntil) {
            pendingUntil = 0L;
            return;
        }

        RequeueCommandParser.Result result = RequeueCommandParser.fromLocraw(
                event.getMessage().getUnformattedText());
        if (!result.handled()) {
            return;
        }

        pendingUntil = 0L;
        event.setCancelled(true);
        if (result.playCommand() == null) {
            message("/rq работает только в BedWars на Hypixel.");
            return;
        }

        if (Minecraft.getMinecraft().thePlayer != null) {
            Minecraft.getMinecraft().thePlayer.sendChatMessage(result.playCommand());
        }
    }

    private static void message(String text) {
        if (Minecraft.getMinecraft().thePlayer != null) {
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(
                    EnumChatFormatting.DARK_PURPLE + "Veyra: "
                            + EnumChatFormatting.AQUA + text));
        }
    }

}
