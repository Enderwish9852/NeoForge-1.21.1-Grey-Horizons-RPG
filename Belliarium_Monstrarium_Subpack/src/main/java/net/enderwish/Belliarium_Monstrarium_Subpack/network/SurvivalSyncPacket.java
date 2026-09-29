package net.enderwish.Belliarium_Monstrarium_Subpack.network;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.ModAttachments;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.SurvivalCapability;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SurvivalSyncPacket(float energy, float stamina, boolean collapsed) implements CustomPacketPayload {

    public static final Type<SurvivalSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BelliariumMonstrariumSubpack.MODID, "survival_sync"));

    public static final StreamCodec<FriendlyByteBuf, SurvivalSyncPacket> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> { buf.writeFloat(p.energy); buf.writeFloat(p.stamina); buf.writeBoolean(p.collapsed); },
            buf -> new SurvivalSyncPacket(buf.readFloat(), buf.readFloat(), buf.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = Minecraft.getInstance().player;
            if (player == null) return;
            player.setData(ModAttachments.SURVIVAL, new SurvivalCapability(energy, stamina, collapsed));
        });
    }
}
