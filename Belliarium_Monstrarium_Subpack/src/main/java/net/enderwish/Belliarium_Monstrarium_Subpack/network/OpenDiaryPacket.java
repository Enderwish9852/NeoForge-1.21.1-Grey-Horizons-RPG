package net.enderwish.Belliarium_Monstrarium_Subpack.network;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.client.DiaryScreen;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.diary.DiaryContent;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Carries the full diary content itself, so the screen never depends on the item having synced yet. */
public record OpenDiaryPacket(int handOrdinal, DiaryContent content) implements CustomPacketPayload {

    public static final Type<OpenDiaryPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BelliariumMonstrariumSubpack.MODID, "open_diary"));

    public static final StreamCodec<FriendlyByteBuf, OpenDiaryPacket> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeVarInt(p.handOrdinal);
                Tag encoded = DiaryContent.CODEC.encodeStart(NbtOps.INSTANCE, p.content)
                        .result().orElse(new CompoundTag());
                buf.writeNbt(encoded);
            },
            buf -> {
                int hand = buf.readVarInt();
                Tag tag = buf.readNbt();
                DiaryContent content = tag == null
                        ? DiaryContent.EMPTY
                        : DiaryContent.CODEC.parse(NbtOps.INSTANCE, tag).result().orElse(DiaryContent.EMPTY);
                return new OpenDiaryPacket(hand, content);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().player == null) return;
            Minecraft.getInstance().setScreen(new DiaryScreen(handOrdinal, content));
        });
    }
}
