package dev.veyra.client.module.modules.render;

import dev.veyra.client.module.Module;
import dev.veyra.client.module.modules.client.AntiBot;
import dev.veyra.client.module.setting.impl.DescriptionSetting;
import dev.veyra.client.module.setting.impl.TickSetting;
import dev.veyra.client.utils.Utils;
import dev.veyra.client.utils.player.PlayerUtils;
import net.minecraft.client.gui.Gui;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.util.EnumChatFormatting;
import net.weavemc.api.event.RenderGameOverlayEvent;
import net.weavemc.api.event.RenderWorldEvent;
import net.weavemc.api.event.SubscribeEvent;
import net.weavemc.api.event.TickEvent;
import net.weavemc.api.event.WorldEvent;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class MurderFinder extends Module {
    public static TickSetting requireScoreboard, includeToolSkins, announce;
    private final Map<UUID, String> murderers = new LinkedHashMap<>();

    public MurderFinder() {
        super("Murder Finder", ModuleCategory.Render, 0);
        registerSetting(new DescriptionSetting("Finds players seen holding a Murder Mystery knife."));
        registerSetting(requireScoreboard = new TickSetting("Require Murder scoreboard", true));
        registerSetting(includeToolSkins = new TickSetting("Tool knife skins", true));
        registerSetting(announce = new TickSetting("Announce", true));
    }

    @SubscribeEvent
    public void onTick(TickEvent.Pre event) {
        if (!PlayerUtils.isPlayerInGame()) return;
        if (requireScoreboard.isToggled() && !isMurderGame()) {
            murderers.clear();
            setSuffix(null);
            return;
        }

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == mc.thePlayer || !player.isEntityAlive() || AntiBot.bot(player)) continue;
            ItemStack held = player.getHeldItem();
            if (!isKnife(held)) continue;
            if (!murderers.containsKey(player.getUniqueID()) && announce.isToggled()) {
                PlayerUtils.sendMessageToSelf("§cMurderer: §f" + player.getName());
            }
            murderers.put(player.getUniqueID(), player.getName());
        }
        setSuffix(murderers.isEmpty() ? "searching" : String.join(", ", murderers.values()));
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldEvent event) {
        if (!PlayerUtils.isPlayerInGame()) return;
        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (murderers.containsKey(player.getUniqueID()) && player.isEntityAlive()) {
                Utils.HUD.drawBoxAroundEntity(player, 1, 0.03D, 0.0D, 0xFFFF3B4F, false);
                Utils.HUD.drawBoxAroundEntity(player, 2, 0.03D, 0.0D, 0x70FF3B4F, false);
            }
        }
    }

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post event) {
        if (murderers.isEmpty() || mc.currentScreen != null || mc.gameSettings.showDebugInfo) return;
        String text = "§cMurderer §7· §f" + String.join(", ", murderers.values());
        int width = mc.fontRendererObj.getStringWidth(text) + 10;
        Gui.drawRect(5, 5, 5 + width, 20, 0xB012101A);
        mc.fontRendererObj.drawStringWithShadow(text, 10.0F, 9.0F, 0xFFFFFFFF);
    }

    @SubscribeEvent
    public void onWorld(WorldEvent event) {
        murderers.clear();
        setSuffix(null);
    }

    @Override
    public void onDisable() {
        murderers.clear();
        setSuffix(null);
    }

    private boolean isMurderGame() {
        ScoreObjective objective = mc.theWorld.getScoreboard().getObjectiveInDisplaySlot(1);
        if (objective == null) return false;
        String title = EnumChatFormatting.getTextWithoutFormattingCodes(objective.getDisplayName());
        if (title == null) return false;
        title = title.toLowerCase(Locale.ROOT);
        return title.contains("murder") || title.contains("assassins") || title.contains("убийц");
    }

    private boolean isKnife(ItemStack stack) {
        if (stack == null) return false;
        Item item = stack.getItem();
        String display = EnumChatFormatting.getTextWithoutFormattingCodes(stack.getDisplayName());
        String name = display == null ? "" : display.toLowerCase(Locale.ROOT);
        if (item instanceof ItemSword || name.contains("knife") || name.contains("dagger")
                || name.contains("меч") || name.contains("нож")) return true;
        return includeToolSkins.isToggled()
                && (item instanceof ItemAxe || item instanceof ItemSpade || item instanceof ItemHoe)
                && !(item instanceof ItemPickaxe);
    }
}
