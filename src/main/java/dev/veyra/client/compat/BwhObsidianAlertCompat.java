package dev.veyra.client.compat;

import dev.veyra.client.utils.player.TeamDetector;
import dev.veyra.weave.events.EventDirection;
import dev.veyra.weave.events.PacketEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S04PacketEntityEquipment;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.weavemc.api.event.SubscribeEvent;
import net.weavemc.api.event.TickEvent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Replaces BWH's nearest-player obsidian guess with packet-backed attribution.
 * The third-party JAR remains untouched; only its obsidian processor is disabled.
 */
public final class BwhObsidianAlertCompat {
    private static final String ALERTS_CLASS = "xyz.mangal.bwhelper.mods.Alerts";
    private static final String PROCESSOR_CLASS = "xyz.mangal.bwhelper.utils.PacketProcessor";
    private static final String CONFIG_CLASS = "xyz.mangal.bwhelper.config.Config";
    private static final double MAX_PLACEMENT_DISTANCE_SQ = 36.0D;
    private static final long HOLD_ALERT_COOLDOWN_MS = 1500L;

    private final Minecraft minecraft = Minecraft.getMinecraft();
    private final ConcurrentLinkedQueue<EquipmentUpdate> equipmentUpdates = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<BlockPos> obsidianPlacements = new ConcurrentLinkedQueue<>();
    private final Set<UUID> activeHolders = new HashSet<>();
    private final Map<UUID, Long> lastHoldAlert = new HashMap<>();
    private boolean bwhAvailable;
    private boolean suppressionFailureReported;

    public BwhObsidianAlertCompat() {
        installSuppression();
    }

    @SubscribeEvent
    public void onPacket(PacketEvent event) {
        if (event.getDirection() != EventDirection.INCOMING || !bwhAvailable) return;
        if (event.getPacket() instanceof S04PacketEntityEquipment packet
                && packet.getEquipmentSlot() == 0) {
            ItemStack stack = packet.getItemStack();
            equipmentUpdates.add(new EquipmentUpdate(packet.getEntityID(), isObsidian(stack)));
        } else if (event.getPacket() instanceof S23PacketBlockChange packet
                && packet.getBlockState() != null
                && packet.getBlockState().getBlock() == Blocks.obsidian) {
            obsidianPlacements.add(packet.getBlockPosition());
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.Post event) {
        if (!bwhAvailable || minecraft.theWorld == null || minecraft.thePlayer == null) {
            clearPending();
            return;
        }

        EquipmentUpdate update;
        while ((update = equipmentUpdates.poll()) != null) applyEquipmentUpdate(update);

        BlockPos placement;
        while ((placement = obsidianPlacements.poll()) != null) attributePlacement(placement);

        activeHolders.removeIf(uuid -> findPlayer(uuid) == null);
    }

    private void applyEquipmentUpdate(EquipmentUpdate update) {
        Entity entity = minecraft.theWorld.getEntityByID(update.entityId());
        if (!(entity instanceof EntityPlayer player)
                || player == minecraft.thePlayer
                || TeamDetector.areTeammates(minecraft.thePlayer, player)) return;
        UUID playerId = player.getUniqueID();
        if (!update.holdingObsidian()) {
            activeHolders.remove(playerId);
            return;
        }

        boolean newlyHolding = activeHolders.add(playerId);
        long now = System.currentTimeMillis();
        long previousAlert = lastHoldAlert.getOrDefault(playerId, 0L);
        if (newlyHolding && now - previousAlert >= HOLD_ALERT_COOLDOWN_MS && alertsEnabled()) {
            lastHoldAlert.put(playerId, now);
            printAlert(formattedPlayerName(player), " §7has §0Obsidian");
        }
    }

    private void attributePlacement(BlockPos position) {
        Collection<BwhObsidianAttribution.Candidate> candidates = new ArrayList<>();
        for (UUID holderId : activeHolders) {
            EntityPlayer player = findPlayer(holderId);
            if (player == null || TeamDetector.areTeammates(minecraft.thePlayer, player)) continue;
            candidates.add(new BwhObsidianAttribution.Candidate(
                    holderId,
                    player.getDistanceSq(position.getX() + 0.5D,
                            position.getY() + 0.5D, position.getZ() + 0.5D),
                    isObsidian(player.getHeldItem())));
        }

        UUID authorId = BwhObsidianAttribution.uniqueNearbyHolder(
                candidates, MAX_PLACEMENT_DISTANCE_SQ);
        EntityPlayer author = findPlayer(authorId);
        if (author != null && alertsEnabled()) {
            printAlert(formattedPlayerName(author), " §7placed §0Obsidian");
        }
    }

    private EntityPlayer findPlayer(UUID playerId) {
        if (playerId == null || minecraft.theWorld == null) return null;
        for (EntityPlayer player : minecraft.theWorld.playerEntities) {
            if (playerId.equals(player.getUniqueID())) return player;
        }
        return null;
    }

    private String formattedPlayerName(EntityPlayer player) {
        if (minecraft.getNetHandler() != null) {
            NetworkPlayerInfo info = minecraft.getNetHandler().getPlayerInfo(player.getUniqueID());
            if (info != null && info.getGameProfile() != null) {
                return ScorePlayerTeam.formatPlayerName(
                        info.getPlayerTeam(), info.getGameProfile().getName());
            }
        }
        return player.getName();
    }

    private void printAlert(String playerName, String action) {
        minecraft.thePlayer.addChatMessage(new ChatComponentText(
                "§7[§dBWH§7]§r §eAlert: " + playerName + action));
    }

    private boolean alertsEnabled() {
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            return Class.forName(CONFIG_CLASS, false, loader).getField("alerts").getBoolean(null);
        } catch (Exception ignored) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private void installSuppression() {
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            Class<?> alerts = Class.forName(ALERTS_CLASS, true, loader);
            Field alertItemsField = alerts.getDeclaredField("alert");
            alertItemsField.setAccessible(true);
            Object alertItems = alertItemsField.get(null);
            if (alertItems instanceof Set<?> set) {
                ((Set<Item>) set).remove(Item.getItemFromBlock(Blocks.obsidian));
            }

            Class<?> processor = Class.forName(PROCESSOR_CLASS, true, loader);
            Field processorsField = processor.getDeclaredField("processors");
            processorsField.setAccessible(true);
            Object processors = processorsField.get(null);
            if (processors instanceof Map<?, ?> map) {
                ((Map<Class<?>, ?>) map).remove(S23PacketBlockChange.class);
            }
            bwhAvailable = true;
            System.out.println("[Veyra] Replaced BWH obsidian nearest-player alerts with exact attribution.");
        } catch (ClassNotFoundException ignored) {
            // BWH is optional.
        } catch (Exception exception) {
            if (!suppressionFailureReported) {
                suppressionFailureReported = true;
                System.err.println("[Veyra] BWH obsidian alert compatibility unavailable: "
                        + exception.getClass().getSimpleName());
            }
        }
    }

    private void clearPending() {
        equipmentUpdates.clear();
        obsidianPlacements.clear();
        activeHolders.clear();
    }

    private static boolean isObsidian(ItemStack stack) {
        return stack != null && stack.getItem() == Item.getItemFromBlock(Blocks.obsidian);
    }

    private record EquipmentUpdate(int entityId, boolean holdingObsidian) {
    }
}
