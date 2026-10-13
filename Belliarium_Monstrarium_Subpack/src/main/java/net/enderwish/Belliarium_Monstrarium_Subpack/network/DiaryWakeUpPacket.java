package net.enderwish.Belliarium_Monstrarium_Subpack.network;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.diary.DayTracker;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client -> server: "Wake up..." was clicked. The server re-validates everything. */
public record DiaryWakeUpPacket() implements CustomPacketPayload {

    public static final Type<DiaryWakeUpPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BelliariumMonstrariumSubpack.MODID, "diary_wake_up"));

    public static final StreamCodec<FriendlyByteBuf, DiaryWakeUpPacket> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {},
            buf -> new DiaryWakeUpPacket()
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) DayTracker.beginWakeUp(player);
        });
    }
}
