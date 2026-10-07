package net.enderwish.Belliarium_Monstrarium_Subpack.network;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.ModAttachments;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.ModDataComponents;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.body.BodyHealthCapability;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.body.BodyPart;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.body.BodyPartTier;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.medical.FirstAidKitContents;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.medical.TargetedMedItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * MedApplyPacket
 *
 * Generalized from the Bandage-only version -- resolves whichever
 * TargetedMedItem is actually in the source slot and validates/applies
 * through the shared interface. The server re-validates tier/eligibility
 * itself rather than trusting the client's own check (that check in
 * MedTargetScreen is just UX -- this is the real gate).
 */
public record MedApplyPacket(int handOrdinal, int kitSlotIndex, String bodyPartName) implements CustomPacketPayload {

    public static final Type<MedApplyPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BelliariumMonstrariumSubpack.MODID, "med_apply"));

    public static final StreamCodec<FriendlyByteBuf, MedApplyPacket> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> { buf.writeVarInt(p.handOrdinal); buf.writeVarInt(p.kitSlotIndex); buf.writeUtf(p.bodyPartName); },
            buf -> new MedApplyPacket(buf.readVarInt(), buf.readVarInt(), buf.readUtf())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            BodyPart part = BodyPart.fromDisplayName(bodyPartName);
            if (part == null) return;

            InteractionHand hand = InteractionHand.values()[handOrdinal];
            ItemStack sourceStack = kitSlotIndex < 0 ? player.getItemInHand(hand) : resolveKitSlotStack(player, hand);

            if (sourceStack.isEmpty() || !(sourceStack.getItem() instanceof TargetedMedItem med)) return;

            BodyHealthCapability cap = player.getData(ModAttachments.BODY_HEALTH);
            BodyPartTier currentTier = BodyPartTier.from(part.getPct(cap));

            if (currentTier != med.treatsTier() || !med.canTreat(part)) {
                player.displayClientMessage(Component.literal("That treatment won't help there."), true);
                return;
            }

            med.applyTreatment(cap, part);
            consumeOne(player, hand);
        });
    }

    private ItemStack resolveKitSlotStack(ServerPlayer player, InteractionHand hand) {
        ItemStack kitStack = player.getItemInHand(hand);
        FirstAidKitContents contents = kitStack.getOrDefault(
                ModDataComponents.FIRST_AID_KIT_CONTENTS.get(), FirstAidKitContents.empty());
        return contents.getSlot(kitSlotIndex);
    }

    private void consumeOne(ServerPlayer player, InteractionHand hand) {
        if (kitSlotIndex < 0) {
            player.getItemInHand(hand).shrink(1);
            return;
        }
        ItemStack kitStack = player.getItemInHand(hand);
        FirstAidKitContents contents = kitStack.getOrDefault(
                ModDataComponents.FIRST_AID_KIT_CONTENTS.get(), FirstAidKitContents.empty());
        ItemStack medStack = contents.getSlot(kitSlotIndex).copy();
        medStack.shrink(1);
        kitStack.set(ModDataComponents.FIRST_AID_KIT_CONTENTS.get(), contents.withSlot(kitSlotIndex, medStack));
    }
}
