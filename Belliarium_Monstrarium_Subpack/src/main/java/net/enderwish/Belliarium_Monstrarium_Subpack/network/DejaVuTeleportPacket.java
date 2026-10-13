package net.enderwish.Belliarium_Monstrarium_Subpack.network;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.client.DejaVuClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server -> client: the "bad dream" teleport fired. Plays a disorientation glitch before/after the jump. */
public record DejaVuTeleportPacket() implements CustomPacketPayload {

    public static final Type<DejaVuTeleportPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BelliariumMonstrariumSubpack.MODID, "deja_vu_teleport"));

    public static final StreamCodec<FriendlyByteBuf, DejaVuTeleportPacket> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {},
            buf -> new DejaVuTeleportPacket()
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().player == null) return;
            DejaVuClientState.triggerTeleportGlitch();
        });
    }
}
