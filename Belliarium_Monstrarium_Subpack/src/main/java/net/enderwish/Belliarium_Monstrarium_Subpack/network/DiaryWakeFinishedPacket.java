package net.enderwish.Belliarium_Monstrarium_Subpack.network;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.client.WakeUpScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server -> client: the rewind is finished, fade the black screen out. */
public record DiaryWakeFinishedPacket() implements CustomPacketPayload {

    public static final Type<DiaryWakeFinishedPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BelliariumMonstrariumSubpack.MODID, "diary_wake_finished"));

    public static final StreamCodec<FriendlyByteBuf, DiaryWakeFinishedPacket> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {},
            buf -> new DiaryWakeFinishedPacket()
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(IPayloadContext context) {
        context.enqueueWork(WakeUpScreen::finish);
    }
}
