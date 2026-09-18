package dev.veyra.client.compat;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BwhRosterUuidResolverTest {
    @Test
    void resolvesCurrentRosterToBwhUndashedUuids() {
        UUID current = UUID.fromString("12345678-1234-4234-9234-1234567890ab");
        UUID stale = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");

        Set<String> resolved = BwhRosterUuidResolver.resolve(
                Set.of("CURRENT_player"),
                List.of(
                        new BwhRosterUuidResolver.PlayerIdentity("current_PLAYER", current),
                        new BwhRosterUuidResolver.PlayerIdentity("OldPlayer", stale)));

        assertEquals(Set.of("123456781234423492341234567890ab"), resolved);
    }

    @Test
    void omitsWhoPlayersNotYetPresentInTab() {
        Set<String> resolved = BwhRosterUuidResolver.resolve(
                Set.of("NotLoadedYet"),
                List.of(new BwhRosterUuidResolver.PlayerIdentity(
                        "SomeoneElse", UUID.randomUUID())));

        assertEquals(Set.of(), resolved);
    }
}
