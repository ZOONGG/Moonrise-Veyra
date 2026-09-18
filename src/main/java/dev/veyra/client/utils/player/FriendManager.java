package dev.veyra.client.utils.player;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.entity.player.EntityPlayer;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/** Persistent UUID-based friend list used by every combat target selector. */
public final class FriendManager {
    private static final Path FRIENDS_FILE = Path.of(
            System.getProperty("user.home"), ".weave", "veyra", "friends.json");
    private static final Set<UUID> FRIENDS = new LinkedHashSet<>();
    private static boolean loaded;

    private FriendManager() {
    }

    public static synchronized void load() {
        if (loaded) return;
        loaded = true;
        if (!Files.isRegularFile(FRIENDS_FILE)) return;

        try (Reader reader = Files.newBufferedReader(FRIENDS_FILE, StandardCharsets.UTF_8)) {
            JsonElement parsed = new JsonParser().parse(reader);
            if (!parsed.isJsonObject()) return;
            JsonArray entries = parsed.getAsJsonObject().getAsJsonArray("friends");
            if (entries == null) return;
            for (JsonElement entry : entries) {
                try {
                    FRIENDS.add(UUID.fromString(entry.getAsString()));
                } catch (IllegalArgumentException ignored) {
                }
            }
        } catch (Exception ignored) {
            FRIENDS.clear();
        }
    }

    public static synchronized boolean isFriend(EntityPlayer player) {
        load();
        return player != null && FRIENDS.contains(player.getUniqueID());
    }

    /** @return true when the player is a friend after toggling. */
    public static synchronized boolean toggle(EntityPlayer player) {
        load();
        UUID id = player.getUniqueID();
        boolean added;
        if (FRIENDS.remove(id)) {
            added = false;
        } else {
            FRIENDS.add(id);
            added = true;
        }
        save();
        return added;
    }

    private static void save() {
        try {
            Files.createDirectories(FRIENDS_FILE.getParent());
            JsonArray entries = new JsonArray();
            for (UUID friend : FRIENDS) entries.add(friend.toString());
            JsonObject root = new JsonObject();
            root.add("friends", entries);

            Path temporary = FRIENDS_FILE.resolveSibling(FRIENDS_FILE.getFileName() + ".tmp");
            Files.writeString(
                    temporary,
                    root.toString(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
            try {
                Files.move(temporary, FRIENDS_FILE,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, FRIENDS_FILE, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception ignored) {
        }
    }
}
