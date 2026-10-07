package net.enderwish.Belliarium_Monstrarium_Subpack.network;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.ModDataComponents;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.medical.AdrenalineManager;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.medical.FirstAidKitContents;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.medical.PainkillerManager;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.medical.TargetedMedItem;
import net.enderwish.Belliarium_Monstrarium_Subpack.item.medical.AdrenalineItem;
import net.enderwish.Belliarium_Monstrarium_Subpack.item.medical.PainkillerItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * UseKitSlotPacket
 *
 * FIX -- Painkiller/Adrenaline branches no longer call item.use(). That
 * was reading the WRONG stack (the kit itself, held in the player's hand
 * -- not the med slot inside it), a rough edge flagged when this was
 * first written. Both managers are now activated directly here instead,
 * matching how FirstAidTriage's auto-use already does it.
 *
 * TargetedMedItem branch (Bandage/Suture Kit/Splint) now opens the
 * generalized MedTargetScreen via OpenMedTargetFromKitPacket.
 */
public record UseKitSlotPacket(int handOrdinal, int slotIndex) implements CustomPacketPayload {

    public static final Type<UseKitSlotPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BelliariumMonstrariumSubpack.MODID, "use_kit_slot"));

    public static final StreamCodec<FriendlyByteBuf, UseKitSlotPacket> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> { buf.writeVarInt(p.handOrdinal); buf.writeVarInt(p.slotIndex); },
            buf -> new UseKitSlotPacket(buf.readVarInt(), buf.readVarInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            InteractionHand hand = InteractionHand.values()[handOrdinal];
            ItemStack kitStack = player.getItemInHand(hand);
            if (kitStack.isEmpty()) return;

            FirstAidKitContents contents = kitStack.getOrDefault(
                    ModDataComponents.FIRST_AID_KIT_CONTENTS.get(), FirstAidKitContents.empty());
            ItemStack medStack = contents.getSlot(slotIndex);
            if (medStack.isEmpty()) return;

            Item med = medStack.getItem();

            if (med instanceof TargetedMedItem) {
                ModMessages.sendToPlayer(
                        new OpenMedTargetFromKitPacket(handOrdinal, slotIndex, medStack.copy()), player);
                return;
            }

            if (med instanceof PainkillerItem) {
                PainkillerManager.INSTANCE.activate(player.getUUID(),
                        player.level().getGameTime() + PainkillerItem.DURATION_TICKS);
                shrinkKitSlot(player, kitStack, contents, slotIndex);
            } else if (med instanceof AdrenalineItem) {
                long endTick = player.level().getGameTime() + AdrenalineItem.DURATION_TICKS;
                AdrenalineManager.INSTANCE.activate(player.getUUID(), endTick);
                PainkillerManager.INSTANCE.activate(player.getUUID(), endTick);
                shrinkKitSlot(player, kitStack, contents, slotIndex);
            }
        });
    }

    private void shrinkKitSlot(ServerPlayer player, ItemStack kitStack, FirstAidKitContents contents, int slotIndex) {
        ItemStack medStack = contents.getSlot(slotIndex).copy();
        medStack.shrink(1);
        kitStack.set(ModDataComponents.FIRST_AID_KIT_CONTENTS.get(), contents.withSlot(slotIndex, medStack));
    }
}
