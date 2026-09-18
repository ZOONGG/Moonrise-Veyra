package dev.veyra.client.compat;

import dev.veyra.client.utils.player.TeamDetector;
import dev.veyra.weave.events.EventDirection;
import dev.veyra.weave.events.PacketEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S04PacketEntityEquipment;
import net.minecraft.network.play.server.S0CPacketSpawnPlayer;
import net.minecraft.network.play.server.S13PacketDestroyEntities;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;
import net.weavemc.api.event.ChatEvent;
import net.weavemc.api.event.SubscribeEvent;
import net.weavemc.api.event.TickEvent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** Adds exact 16+ diamond and armor Protection alerts to optional BWH. */
public final class BwhGearAlertCompat {
    private static final String CONFIG_CLASS = "xyz.mangal.bwhelper.config.Config";
    private static final String PROCESSOR_CLASS = "xyz.mangal.bwhelper.utils.PacketProcessor";
    private static final long SCAN_INTERVAL_MS = 100L;
    private static final long EQUIPMENT_RESOLVE_WINDOW_MS = 2_000L;

    private final Minecraft minecraft = Minecraft.getMinecraft();
    private final BwhGearAlertState state = new BwhGearAlertState();
    private final BwhBowAlertState bowState = new BwhBowAlertState();
    private final BwhTeamProtectionState teamProtection = new BwhTeamProtectionState();
    private final Map<String, Integer> announcedProtection = new HashMap<>();
    private final Map<Integer, UUID> playerIdsByEntity = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<ProtectionEquipmentUpdate> protectionEquipmentUpdates =
            new ConcurrentLinkedQueue<>();
    private boolean bwhAvailable;
    private boolean equipmentFilterInstalled;
    private World trackedWorld;
    private long nextScan;

    public BwhGearAlertCompat() {
        bwhAvailable = classExists(CONFIG_CLASS);
        if (bwhAvailable) {
            installEquipmentFilter();
            System.out.println("[Veyra] BWH diamond/protection gear scan ready.");
        }
    }

    @SubscribeEvent
    public void onChat(ChatEvent.Received event) {
        if (minecraft.thePlayer == null || event.getMessage() == null) return;
        BwhProtectionPurchaseParser.Result purchase = BwhProtectionPurchaseParser.parse(
                event.getMessage().getUnformattedText());
        if (!purchase.matched()) return;

        EntityPlayer purchaser = findPlayer(purchase.purchaser());
        ScorePlayerTeam team = teamOf(purchaser);
        boolean upgraded;
        if (team != null) {
            upgraded = teamProtection.update(teamKey(team), purchase.level());
        } else {
            String key = purchase.purchaser().toLowerCase(Locale.ROOT);
            int previous = announcedProtection.getOrDefault(key, 0);
            upgraded = purchase.level() > previous;
            if (upgraded) announcedProtection.put(key, purchase.level());
        }
        if (purchaser != null) state.updateProtection(purchaser.getUniqueID(), purchase.level());
        if (upgraded && alertsEnabled()) {
            String displayName = team != null
                    ? BwhBedwarsTeamLabel.fromPrefix(team.getColorPrefix())
                    : purchaser == null ? "§f" + purchase.purchaser() : formattedPlayerName(purchaser);
            printAlert(displayName, " §7now has §bProtection §f"
                    + BwhGearAlertState.romanLevel(purchase.level()));
            System.out.println("[Veyra] BWH team protection purchase: "
                    + purchase.purchaser() + " -> "
                    + BwhGearAlertState.romanLevel(purchase.level()) + '.');
        }
    }

    /** Reads Protection from the packet before any periodic entity-scan race. */
    @SubscribeEvent
    public void onPacket(PacketEvent event) {
        if (event.getDirection() != EventDirection.INCOMING) return;
        if (event.getPacket() instanceof S0CPacketSpawnPlayer spawn) {
            playerIdsByEntity.put(spawn.getEntityID(), spawn.getPlayer());
            return;
        }
        if (event.getPacket() instanceof S13PacketDestroyEntities destroyed) {
            for (int entityId : destroyed.getEntityIDs()) playerIdsByEntity.remove(entityId);
            return;
        }
        if (!(event.getPacket() instanceof S04PacketEntityEquipment packet)
                || packet.getEquipmentSlot() < 1
                || packet.getEquipmentSlot() > 4) return;
        ItemStack stack = packet.getItemStack();
        int level = stack == null ? 0 : BwhItemEnchantments.level(
                stack, Enchantment.protection, BwhItemEnchantments.PROTECTION_ID);
        if (level < 1 || level > 4) return;
        protectionEquipmentUpdates.add(new ProtectionEquipmentUpdate(
                packet.getEntityID(), playerIdsByEntity.get(packet.getEntityID()), level,
                System.currentTimeMillis() + EQUIPMENT_RESOLVE_WINDOW_MS));
    }

    @SubscribeEvent
    public void onTick(TickEvent.Post event) {
        if (!bwhAvailable) {
            bwhAvailable = classExists(CONFIG_CLASS);
            if (!bwhAvailable) return;
            installEquipmentFilter();
            System.out.println("[Veyra] BWH diamond/protection gear scan ready.");
        }
        if (minecraft.theWorld == null || minecraft.thePlayer == null) {
            state.reset();
            teamProtection.reset();
            bowState.reset();
            announcedProtection.clear();
            playerIdsByEntity.clear();
            protectionEquipmentUpdates.clear();
            trackedWorld = null;
            return;
        }
        if (trackedWorld != minecraft.theWorld) {
            trackedWorld = minecraft.theWorld;
            state.reset();
            teamProtection.reset();
            bowState.reset();
            announcedProtection.clear();
            playerIdsByEntity.clear();
            protectionEquipmentUpdates.clear();
        }

        drainProtectionEquipmentUpdates();

        long now = System.currentTimeMillis();
        if (now < nextScan) return;
        nextScan = now + SCAN_INTERVAL_MS;

        for (EntityPlayer player : minecraft.theWorld.playerEntities) {
            if (player == null || player == minecraft.thePlayer) continue;
            if (!isTeammate(player)) updateHeldDiamonds(player, player.getHeldItem());
            updateProtection(player, highestProtectionLevel(player));
        }
    }

    private void drainProtectionEquipmentUpdates() {
        long now = System.currentTimeMillis();
        int queued = protectionEquipmentUpdates.size();
        List<ProtectionEquipmentUpdate> retry = new ArrayList<>();
        for (int index = 0; index < queued; index++) {
            ProtectionEquipmentUpdate update = protectionEquipmentUpdates.poll();
            if (update == null) break;
            Entity entity = minecraft.theWorld.getEntityByID(update.entityId());
            if (entity instanceof EntityPlayer player) {
                if (player != minecraft.thePlayer) updateProtection(player, update.level());
            } else if (update.playerId() != null && updateTeamProtection(update)) {
                // Team was resolved from TAB/scoreboard without waiting for the
                // remote entity to finish spawning into the client world.
            } else if (now <= update.resolveUntilMs()) {
                retry.add(update);
            }
        }
        protectionEquipmentUpdates.addAll(retry);
    }

    private boolean updateTeamProtection(ProtectionEquipmentUpdate update) {
        if (minecraft.getNetHandler() == null || update.playerId() == null) return false;
        NetworkPlayerInfo info = minecraft.getNetHandler().getPlayerInfo(update.playerId());
        if (info == null || info.getPlayerTeam() == null) return false;
        ScorePlayerTeam team = info.getPlayerTeam();
        boolean upgraded = teamProtection.update(teamKey(team), update.level());
        if (upgraded && alertsEnabled()) {
            printAlert(BwhBedwarsTeamLabel.fromPrefix(team.getColorPrefix()),
                    " §7now has §bProtection §f"
                            + BwhGearAlertState.romanLevel(update.level()));
            System.out.println("[Veyra] BWH packet-resolved team protection: "
                    + info.getGameProfile().getName() + " -> "
                    + BwhGearAlertState.romanLevel(update.level()) + '.');
        }
        return true;
    }

    private void updateHeldDiamonds(EntityPlayer player, ItemStack stack) {
        int amount = stack != null && stack.getItem() == Items.diamond ? stack.stackSize : 0;
        if (state.updateDiamonds(player.getUniqueID(), amount) && alertsEnabled()) {
            printAlert(player, " §7has §b" + amount + " Diamonds");
            System.out.println("[Veyra] BWH gear alert: " + player.getName()
                    + " holds " + amount + " diamonds.");
        }
    }

    private void updateProtection(EntityPlayer player, int level) {
        if (level < 1 || level > 4) return;
        ScorePlayerTeam team = teamOf(player);
        String teamKey = teamKey(team);
        boolean upgraded = teamKey == null
                ? state.updateProtection(player.getUniqueID(), level)
                : teamProtection.update(teamKey, level);
        if (upgraded && alertsEnabled()) {
            String subject = team == null
                    ? formattedPlayerName(player)
                    : BwhBedwarsTeamLabel.fromPrefix(team.getColorPrefix());
            printAlert(subject, " §7now has §bProtection §f"
                    + BwhGearAlertState.romanLevel(level));
            System.out.println("[Veyra] BWH observed team protection: "
                    + player.getName() + " -> " + BwhGearAlertState.romanLevel(level) + '.');
        }
    }

    private static int highestProtectionLevel(EntityPlayer player) {
        int highest = 0;
        for (int armorSlot = 0; armorSlot < 4; armorSlot++) {
            ItemStack stack = player.getCurrentArmor(armorSlot);
            if (stack == null || !(stack.getItem() instanceof ItemArmor)) continue;
            highest = Math.max(highest, BwhItemEnchantments.level(
                    stack, Enchantment.protection, BwhItemEnchantments.PROTECTION_ID));
        }
        return highest;
    }

    private void printAlert(EntityPlayer player, String action) {
        printAlert(formattedPlayerName(player), action);
    }

    private void printAlert(String playerName, String action) {
        minecraft.thePlayer.addChatMessage(new ChatComponentText(
                "§7[§dBWH§7]§r §eAlert: " + playerName + action));
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

    private EntityPlayer findPlayer(String playerName) {
        if (minecraft.theWorld == null) return null;
        for (EntityPlayer player : minecraft.theWorld.playerEntities) {
            if (player != null && player.getName().equalsIgnoreCase(playerName)) return player;
        }
        return null;
    }

    private ScorePlayerTeam teamOf(EntityPlayer player) {
        if (player == null) return null;
        if (minecraft.getNetHandler() != null) {
            NetworkPlayerInfo info = minecraft.getNetHandler().getPlayerInfo(player.getUniqueID());
            if (info != null && info.getPlayerTeam() != null) return info.getPlayerTeam();
        }
        return player.getTeam() instanceof ScorePlayerTeam team ? team : null;
    }

    private static String teamKey(ScorePlayerTeam team) {
        return team == null ? null : BwhBedwarsTeamLabel.teamKey(
                team.getRegisteredName(), team.getColorPrefix());
    }

    private boolean isTeammate(EntityPlayer player) {
        return TeamDetector.areTeammates(minecraft.thePlayer, player);
    }

    @SuppressWarnings("unchecked")
    private void installEquipmentFilter() {
        if (equipmentFilterInstalled) return;
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            Class<?> processor = Class.forName(PROCESSOR_CLASS, true, loader);
            Field processorsField = processor.getDeclaredField("processors");
            processorsField.setAccessible(true);
            Object value = processorsField.get(null);
            if (!(value instanceof Map<?, ?> processors)) return;

            Object current = processors.get(S04PacketEntityEquipment.class);
            if (!(current instanceof Consumer<?>)) return;
            Consumer<S04PacketEntityEquipment> original =
                    (Consumer<S04PacketEntityEquipment>) current;
            Consumer<S04PacketEntityEquipment> enemyOnly =
                    packet -> forwardEnemyEquipment(packet, original);
            ((Map<Class<?>, Consumer<?>>) processors).put(
                    S04PacketEntityEquipment.class,
                    enemyOnly);
            equipmentFilterInstalled = true;
            System.out.println("[Veyra] BWH teammate equipment alerts suppressed.");
        } catch (Exception exception) {
            System.err.println("[Veyra] BWH teammate alert filter unavailable: "
                    + exception.getClass().getSimpleName());
        }
    }

    private void forwardEnemyEquipment(
            S04PacketEntityEquipment packet,
            Consumer<S04PacketEntityEquipment> original) {
        if (minecraft.theWorld == null || minecraft.thePlayer == null) return;
        Entity entity = minecraft.theWorld.getEntityByID(packet.getEntityID());
        if (!(entity instanceof EntityPlayer player)
                || player == minecraft.thePlayer
                || isTeammate(player)) return;
        ItemStack stack = packet.getItemStack();
        if (stack != null && stack.getItem() instanceof ItemBow) {
            alertBow(player, stack);
            return;
        }
        original.accept(packet);
    }

    private void alertBow(EntityPlayer player, ItemStack stack) {
        int power = BwhItemEnchantments.level(
                stack, Enchantment.power, BwhItemEnchantments.POWER_ID);
        int punch = BwhItemEnchantments.level(
                stack, Enchantment.punch, BwhItemEnchantments.PUNCH_ID);
        int enchantmentCount = BwhItemEnchantments.count(stack);
        String bowType = BwhBowAlertLabel.fromEvidence(
                power, punch, enchantmentCount, BwhItemEnchantments.itemData(stack));
        if (!bowState.update(player.getUniqueID(), bowType) || !alertsEnabled()) return;
        printAlert(player, " §7has a §d" + bowType);
        System.out.println("[Veyra] BWH bow alert: " + player.getName()
                + " holds " + bowType + " (power=" + power
                + ", punch=" + punch + ", tags=" + enchantmentCount + ").");
    }

    private boolean alertsEnabled() {
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            return Class.forName(CONFIG_CLASS, false, loader).getField("alerts").getBoolean(null);
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean classExists(String className) {
        try {
            Class.forName(className, false, Thread.currentThread().getContextClassLoader());
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    private record ProtectionEquipmentUpdate(
            int entityId, UUID playerId, int level, long resolveUntilMs) {
    }
}
