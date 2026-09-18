package dev.veyra.weave;

import net.weavemc.api.ModInitializer;
import net.weavemc.api.event.EventBus;
import net.weavemc.api.event.StartGameEvent;
import dev.veyra.client.main.Veyra;

public class Main implements ModInitializer {
    @Override
    public void init() {
        EventBus.subscribe(StartGameEvent.Post.class, (startGameEvent) -> Veyra.init());
    }
}
