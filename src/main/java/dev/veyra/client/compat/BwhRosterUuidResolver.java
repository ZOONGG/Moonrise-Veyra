package dev.veyra.client.compat;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/** Converts Hypixel /who names to the undashed UUID format expected by BWH. */
final class BwhRosterUuidResolver {
    private BwhRosterUuidResolver() {
    }

    static Set<String> resolve(Set<String> roster, Collection<PlayerIdentity> tabPlayers) {
        LinkedHashSet<String> resolved = new LinkedHashSet<>();
        for (PlayerIdentity tabPlayer : tabPlayers) {
            if (tabPlayer.name() == null || tabPlayer.uuid() == null) continue;
            for (String rosterName : roster) {
                if (rosterName.equalsIgnoreCase(tabPlayer.name())) {
                    resolved.add(tabPlayer.uuid().toString().replace("-", ""));
                    break;
                }
            }
        }
        return resolved;
    }

    record PlayerIdentity(String name, UUID uuid) {
    }
}
