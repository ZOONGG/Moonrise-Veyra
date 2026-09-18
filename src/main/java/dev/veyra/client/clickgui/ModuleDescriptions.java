package dev.veyra.client.clickgui;

import java.util.HashMap;
import java.util.Map;

public final class ModuleDescriptions {
    private static final Map<String, String> COPY = new HashMap<>();

    static {
        COPY.put("AimAssist", "Smoothly guides your aim toward nearby targets.");
        COPY.put("AntiBot", "Filters common non-player entities from targeting.");
        COPY.put("Anti-AFK", "Makes tiny idle movements to prevent AFK removal.");
        COPY.put("AntiVoid", "Helps prevent accidental falls into the void.");
        COPY.put("ArrayList", "Shows enabled modules with configurable colors, layout, animation, and filters.");
        COPY.put("AutoBlock", "Blocks predicted hits using BlockHit, Predictive, or Blatant behavior.");
        COPY.put("AutoBlock In", "Builds a compact protective shell around you.");
        COPY.put("AutoClicker", "Automates primary clicks with configurable timing.");
        COPY.put("AutoDodge", "Reports scoreboard teams once after joining a world.");
        COPY.put("AutoFish", "Retracts a fishing rod on a bite and recasts it.");
        COPY.put("AutoPlace", "Places blocks automatically under valid conditions.");
        COPY.put("AutoTool", "Selects the fastest hotbar tool while mining.");
        COPY.put("Backtrack", "Delays selected updates for configurable target tracking.");
        COPY.put("BedNuker", "Finds and interacts with nearby beds automatically.");
        COPY.put("BedESP", "Highlights nearby beds through blocks.");
        COPY.put("BedPlates", "Shows the protection blocks surrounding nearby beds.");
        COPY.put("Bhop", "Provides configurable bunny-hop movement behavior.");
        COPY.put("Blink", "Temporarily queues movement packets before releasing them.");
        COPY.put("Chams", "Renders players with an alternate visibility style.");
        COPY.put("ChestESP", "Highlights nearby chest locations in the world.");
        COPY.put("ClickAssist", "Adds occasional assisted clicks to manual input.");
        COPY.put("Clutch", "Executes a fixed camera-hidden rescue path after a confirmed fall.");
        COPY.put("ClickGui", "Opens and customizes the Veyra control center.");
        COPY.put("ClosetSpeed", "Applies subtle configurable movement adjustments.");
        COPY.put("Criticals", "Adjusts attack movement for critical-hit behavior.");
        COPY.put("FakeLag", "Queues network updates for a configurable interval.");
        COPY.put("FastPlace", "Uses the classic configurable right-click delay in ticks.");
        COPY.put("Flight", "Provides configurable aerial movement modes.");
        COPY.put("Freecam", "Detaches local camera movement from the server position.");
        COPY.put("InvMove", "Keeps movement controls available in inventory screens.");
        COPY.put("HitSelect", "Filters attacks during a target's recent-damage immunity window.");
        COPY.put("JumpReset", "Uses ordinary jump input after supported incoming knockback.");
        COPY.put("KeepSprint", "Preserves sprint state after supported interactions.");
        COPY.put("Killaura", "Automatically targets nearby entities using selected rules.");
        COPY.put("Manager", "Manages selected inventory items and utility actions.");
        COPY.put("Murder Finder", "Marks players seen holding a Murder Mystery knife.");
        COPY.put("Nametags", "Replaces player labels with clearer custom nametags.");
        COPY.put("NoHitDelay", "Removes the standard delay between registered hits.");
        COPY.put("NoJumpDelay", "Removes the vanilla delay between consecutive jumps.");
        COPY.put("NoRotate", "Prevents selected server rotations from moving your view.");
        COPY.put("NoSlow", "Reduces movement slowdown while using supported items.");
        COPY.put("NoUseDelay", "Removes only the delay after finishing food or potion use.");
        COPY.put("ESP", "Highlights player positions using configurable visuals.");
        COPY.put("Reach", "Changes the interaction distance used for supported targets.");
        COPY.put("RightClicker", "Automates secondary clicks with configurable timing.");
        COPY.put("BridgeAssist", "Prevents edge falls and randomly uses safe no-shift passes at the configured chance.");
        COPY.put("Sprint", "Keeps sprint active according to selected conditions.");
        COPY.put("Stealer", "Moves selected container items using configurable timing.");
        COPY.put("Strafe", "Adjusts air movement and directional acceleration.");
        COPY.put("Timer", "Changes the local game timer speed while enabled.");
        COPY.put("TargetFilter", "Applies shared team, friend, bot, visibility, and wall rules.");
        COPY.put("Velocity", "Controls how incoming knockback is applied.");
        COPY.put("WTap", "Briefly releases forward after real player attacks to restart sprint.");
    }

    private ModuleDescriptions() {
    }

    public static String forModule(String moduleName) {
        return COPY.getOrDefault(moduleName, "Configure this module's behavior and keybind.");
    }
}
