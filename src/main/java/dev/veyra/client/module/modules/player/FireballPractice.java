package dev.veyra.client.module.modules.player;

import dev.veyra.client.module.Module;
import dev.veyra.client.module.setting.impl.DescriptionSetting;
import dev.veyra.client.module.setting.impl.SliderSetting;
import dev.veyra.client.module.setting.impl.TickSetting;
import dev.veyra.client.player.clutch.LocalFireballPractice;
import net.weavemc.api.event.*;

/** Optional local sandbox, disabled by default. */
public final class FireballPractice extends Module {
    private final SliderSetting speed = new SliderSetting("Fireball Speed", 1.5, 0.25, 3, 0.05);
    private final SliderSetting knockback = new SliderSetting("Knockback", 1.35, 1, 3, 0.05);
    private final SliderSetting power = new SliderSetting("Explosion Power", 2, 0.5, 4, 0.25);
    private final TickSetting debug = new TickSetting("Debug Trace", false);
    private final TickSetting blocks = new TickSetting("Destroy Blocks", false);
    public FireballPractice() {
        super("FireballPractice", ModuleCategory.Player, 0);
        registerSetting(new DescriptionSetting("Singleplayer fire charge practice. Approximate physics, not a Hypixel replica."));
        registerSetting(speed); registerSetting(power); registerSetting(knockback); registerSetting(blocks); registerSetting(debug);
    }
    @Override public void onEnable() { publish(); }
    @Override public void onDisable() { LocalFireballPractice.clear(); }
    @SubscribeEvent public void onTick(TickEvent.Post event) { publish(); }
    @SubscribeEvent public void onWorld(WorldEvent event) { LocalFireballPractice.clear(); }
    @SubscribeEvent public void onShutdown(ShutdownEvent event) { LocalFireballPractice.clear(); }
    private void publish() { LocalFireballPractice.publish(mc, speed.getInput(), (float)power.getInput(), blocks.isToggled(), debug.isToggled(), knockback.getInput()); }
}
