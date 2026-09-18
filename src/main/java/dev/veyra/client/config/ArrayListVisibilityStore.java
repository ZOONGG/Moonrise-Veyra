package dev.veyra.client.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/** Crash-safe, order-independent persistence for the ArrayList module picker. */
public final class ArrayListVisibilityStore {
    private static final String FILE_NAME = "arraylist-modules.txt";

    private final Path file;

    public ArrayListVisibilityStore(Path file) {
        this.file = file;
    }

    public static ArrayListVisibilityStore userDefault() {
        return new ArrayListVisibilityStore(Path.of(
                System.getProperty("user.home"), ".weave", "veyra", FILE_NAME));
    }

    public boolean exists() {
        return Files.isRegularFile(file);
    }

    public Set<String> load() throws IOException {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        if (!exists()) return values;
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            String normalized = normalize(line);
            if (!normalized.isEmpty()) values.add(normalized);
        }
        return values;
    }

    public void save(Collection<String> values) throws IOException {
        Path parent = file.getParent();
        if (parent != null) Files.createDirectories(parent);

        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            String cleaned = normalize(value);
            if (!cleaned.isEmpty()) normalized.add(cleaned);
        }

        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        Files.write(temporary, normalized, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
        try {
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String normalize(String value) {
        if (value == null) return "";
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return "playeresp".equals(normalized) ? "esp" : normalized;
    }
}
