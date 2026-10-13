package net.enderwish.Belliarium_Monstrarium_Subpack.network;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.ModDataComponents;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.diary.DiaryBook;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.diary.DiaryContent;
import net.enderwish.Belliarium_Monstrarium_Subpack.item.SurvivorsDiaryItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client -> server: the player's notes for one day. Length-capped and index-checked server-side. */
public record DiarySavePacket(int handOrdinal, int entryIndex, String notes) implements CustomPacketPayload {

    public static final Type<DiarySavePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BelliariumMonstrariumSubpack.MODID, "diary_save"));

    public static final StreamCodec<FriendlyByteBuf, DiarySavePacket> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> { buf.writeVarInt(p.handOrdinal); buf.writeVarInt(p.entryIndex); buf.writeUtf(p.notes, 32767); },
            buf -> new DiarySavePacket(buf.readVarInt(), buf.readVarInt(), buf.readUtf(32767))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (handOrdinal < 0 || handOrdinal >= InteractionHand.values().length) return;

            ItemStack stack = player.getItemInHand(InteractionHand.values()[handOrdinal]);
            if (!(stack.getItem() instanceof SurvivorsDiaryItem)) return;

            DiaryContent content = stack.getOrDefault(ModDataComponents.DIARY_CONTENT.get(), DiaryContent.EMPTY);
            if (entryIndex < 0 || entryIndex >= content.entries().size()) return;

            String text = notes.length() > DiaryBook.MAX_NOTES_CHARS
                    ? notes.substring(0, DiaryBook.MAX_NOTES_CHARS) : notes;
            stack.set(ModDataComponents.DIARY_CONTENT.get(),
                    content.withEntryReplaced(entryIndex, content.entries().get(entryIndex).withNotes(text)));
        });
    }
}
