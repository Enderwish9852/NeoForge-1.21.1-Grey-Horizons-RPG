package net.enderwish.Belliarium_Monstrarium_Subpack.item.medical;

import net.enderwish.Belliarium_Monstrarium_Subpack.core.ModDataComponents;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.medical.FirstAidKitContents;
import net.enderwish.Belliarium_Monstrarium_Subpack.network.FirstAidKitContentsPacket;
import net.enderwish.Belliarium_Monstrarium_Subpack.network.ModMessages;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * FirstAidKitItem
 *
 * isMed() now recognises Suture Kit and Splint too, so refilling against
 * either works the same way it already did for Bandage. Right-click still
 * only opens the manual GUI -- the new fast-use keybind is a completely
 * separate trigger path (see ModKeyMappings/FirstAidFastUseHandler/
 * FastUseFirstAidPacket) and doesn't touch this file at all.
 */
public class FirstAidKitItem extends Item {

    public FirstAidKitItem(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack kitStack = player.getItemInHand(hand);
        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.success(kitStack);
        }

        InteractionHand otherHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack otherStack = player.getItemInHand(otherHand);

        FirstAidKitContents contents = kitStack.getOrDefault(
                ModDataComponents.FIRST_AID_KIT_CONTENTS.get(), FirstAidKitContents.empty());

        if (isMed(otherStack) && !otherStack.isEmpty()) {
            boolean isAdrenaline = otherStack.getItem() instanceof AdrenalineItem;
            int targetSlot = isAdrenaline
                    ? (contents.getSlot(FirstAidKitContents.ADRENALINE_SLOT_INDEX).isEmpty() ? FirstAidKitContents.ADRENALINE_SLOT_INDEX : -1)
                    : findFirstEmptyNonAdrenalineSlot(contents);

            if (targetSlot >= 0) {
                ItemStack toInsert = otherStack.copyWithCount(1);
                contents = contents.withSlot(targetSlot, toInsert);
                kitStack.set(ModDataComponents.FIRST_AID_KIT_CONTENTS.get(), contents);
                otherStack.shrink(1);
            }
        }

        ModMessages.sendToPlayer(new FirstAidKitContentsPacket(hand.ordinal(), contents.items()), serverPlayer);
        return InteractionResultHolder.success(kitStack);
    }

    private static boolean isMed(ItemStack stack) {
        return stack.getItem() instanceof BandageItem
                || stack.getItem() instanceof SutureKitItem
                || stack.getItem() instanceof SplintItem
                || stack.getItem() instanceof PainkillerItem
                || stack.getItem() instanceof AdrenalineItem;
    }

    private static int findFirstEmptyNonAdrenalineSlot(FirstAidKitContents contents) {
        for (int i = 0; i < FirstAidKitContents.ADRENALINE_SLOT_INDEX; i++) {
            if (contents.getSlot(i).isEmpty()) return i;
        }
        return -1;
    }
}
