package net.enderwish.Belliarium_Monstrarium_Subpack.network;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.BodyHealthCapability;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record BodyHealthSyncPacket(
        float head, float torso, float lArm, float rArm, float lLeg, float rLeg, float lFoot, float rFoot
) implements CustomPacketPayload {

    public static final Type<BodyHealthSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BelliariumMonstrariumSubpack.MODID, "body_health_sync"));

    public static final StreamCodec<FriendlyByteBuf, BodyHealthSyncPacket> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeFloat(p.head); buf.writeFloat(p.torso);
                buf.writeFloat(p.lArm); buf.writeFloat(p.rArm);
                buf.writeFloat(p.lLeg); buf.writeFloat(p.rLeg);
                buf.writeFloat(p.lFoot); buf.writeFloat(p.rFoot);
            },
            buf -> new BodyHealthSyncPacket(
                    buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(),
                    buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = Minecraft.getInstance().player;
            if (player == null) return;
            BodyHealthCapability cap = new BodyHealthCapability(head, torso, lArm, rArm, lLeg, rLeg, lFoot, rFoot);
            player.setData(ModAttachments.BODY_HEALTH, cap);
        });
    }
}
