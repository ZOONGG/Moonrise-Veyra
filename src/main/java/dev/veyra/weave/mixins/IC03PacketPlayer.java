package dev.veyra.weave.mixins;

import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Mutable access to the rotation fields of the vanilla movement packet. */
@Mixin(C03PacketPlayer.class)
public interface IC03PacketPlayer extends Packet {
    @Accessor("yaw")
    void setYaw(float yaw);

    @Accessor("pitch")
    void setPitch(float pitch);

    @Accessor("rotating")
    void setRotating(boolean rotating);
}
