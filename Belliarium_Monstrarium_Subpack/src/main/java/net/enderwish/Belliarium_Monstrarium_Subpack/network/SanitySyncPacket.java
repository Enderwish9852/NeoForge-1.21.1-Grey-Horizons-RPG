package net.enderwish.Belliarium_Monstrarium_Subpack.network;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.ModAttachments;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.survival.SanityCapability;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** S2C sanity sync — mirrors SurvivalSyncPacket's pattern. */
public record SanitySyncPacket(float sanity) implements CustomPacketPayload {

    public static final Type<SanitySyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BelliariumMonstrariumSubpack.MODID, "sanity_sync"));

    public static final StreamCodec<FriendlyByteBuf, SanitySyncPacket> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> buf.writeFloat(p.sanity),
            buf -> new SanitySyncPacket(buf.readFloat())
    );

    @Override
    public Type<? extends SanitySyncPacket> type() { return TYPE; }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = Minecraft.getInstance().player;
            if (player == null) return;
            player.setData(ModAttachments.SANITY, new SanityCapability(sanity));
        });
    }
}
