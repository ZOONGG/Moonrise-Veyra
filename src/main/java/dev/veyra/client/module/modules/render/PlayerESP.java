package dev.veyra.client.module.modules.render;

import dev.veyra.client.module.Module;
import dev.veyra.client.module.modules.client.AntiBot;
import dev.veyra.client.module.setting.impl.ColorSetting;
import dev.veyra.client.module.setting.impl.ComboSetting;
import dev.veyra.client.module.setting.impl.DescriptionSetting;
import dev.veyra.client.module.setting.impl.SliderSetting;
import dev.veyra.client.module.setting.impl.TickSetting;
import dev.veyra.client.utils.Utils;
import dev.veyra.client.utils.player.PlayerUtils;
import dev.veyra.client.utils.render.WorldOverlayRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.weavemc.api.event.RenderWorldEvent;
import net.weavemc.api.event.SubscribeEvent;

/** Exact player hitboxes with a fully independent RGB color. */
public class PlayerESP extends Module {
    public static TickSetting filled, healthBar, hurtEffect, hideFriendlies, showInvisible;
    public static SliderSetting range, lineWidth, outlineOpacity, fillOpacity;
    public static ColorSetting color;
    public static ComboSetting<Style> style;
    public static ComboSetting<ColorMode> colorMode;

    public PlayerESP() {
        super("ESP", ModuleCategory.Render, 0);
        registerSetting(new DescriptionSetting("Exact player-sized hitboxes with custom RGB."));
        registerSetting(style = new ComboSetting<>("Style", Style.Hitbox));
        registerSetting(colorMode = new ComboSetting<>("Color mode", ColorMode.Custom));
        registerSetting(color = new ColorSetting("Color", 0xFFA855F7));
        registerSetting(range = new SliderSetting("Range", 96, 8, 160, 4));
        registerSetting(lineWidth = new SliderSetting("Line width", 1.0D, 0.5D, 5.0D, 0.5D));
        registerSetting(outlineOpacity = new SliderSetting("Outline opacity", 100, 10, 100, 5));
        registerSetting(filled = new TickSetting("Box filled", true));
        registerSetting(fillOpacity = new SliderSetting("Fill opacity", 18, 0, 70, 2));
        registerSetting(healthBar = new TickSetting("Health bar", false));
        registerSetting(hurtEffect = new TickSetting("Hurt effect", false));
        registerSetting(hideFriendlies = new TickSetting("Hide friendlies", false));
        registerSetting(showInvisible = new TickSetting("Show invisible", true));
    }

    @SubscribeEvent
    public void onRender(RenderWorldEvent event) {
        if (!PlayerUtils.isPlayerInGame()) return;
        double maxDistanceSq = range.getInput() * range.getInput();
        int rendered = 0;
        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == mc.thePlayer || !player.isEntityAlive() || player.deathTime != 0) continue;
            if (!showInvisible.isToggled() && player.isInvisible()) continue;
            if (AntiBot.bot(player)
                    || mc.thePlayer.getDistanceSqToEntity(player) > maxDistanceSq) continue;
            if (hideFriendlies.isToggled() && mc.thePlayer.isOnSameTeam(player)) continue;

            int color = colorFor(player);
            if (hurtEffect.isToggled() && player.hurtTime > 0) color = 0xFFFF3B3B;
            if (style.getMode() == Style.TwoD) {
                Utils.HUD.drawBoxAroundEntity(player, 3, 0.0D, 0.0D, color, false);
            } else {
                WorldOverlayRenderer.drawEntityHitbox(
                        player,
                        color,
                        style.getMode() == Style.Box
                                && filled.isToggled()
                                && fillOpacity.getInput() > 0.0D,
                        (float) lineWidth.getInput(),
                        (float) (outlineOpacity.getInput() / 100.0D),
                        (float) (fillOpacity.getInput() / 100.0D));
            }
            if (healthBar.isToggled()) {
                Utils.HUD.drawBoxAroundEntity(player, 4, 0.0D, 3.0D, color, false);
            }
            rendered++;
        }
        setSuffix(style.getMode() + " · " + rendered);
    }

    @Override
    public void onDisable() {
        Utils.HUD.ring_c = false;
        setSuffix(null);
    }

    private int colorFor(EntityPlayer player) {
        if (colorMode.getMode() == ColorMode.Custom) {
            return color.getColor();
        }
        String formatted = player.getDisplayName().getFormattedText();
        for (int i = 0; i + 1 < formatted.length(); i++) {
            if (formatted.charAt(i) != '\u00A7') continue;
            char code = Character.toLowerCase(formatted.charAt(i + 1));
            if ((code >= '0' && code <= '9') || (code >= 'a' && code <= 'f')) {
                return 0xFF000000 | mc.fontRendererObj.getColorCode(code);
            }
        }
        return color.getColor();
    }

    public enum Style {
        Hitbox, Box, TwoD
    }

    public enum ColorMode {
        Custom, Team
    }
}
