package dev.veyra.weave.mixins;

import net.minecraft.network.play.server.S14PacketEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Reads the packet entity id without touching the client world on Netty's thread. */
@Mixin(S14PacketEntity.class)
public interface IS14PacketEntity {
    @Accessor("entityId")
    int veyra$getEntityId();
}
