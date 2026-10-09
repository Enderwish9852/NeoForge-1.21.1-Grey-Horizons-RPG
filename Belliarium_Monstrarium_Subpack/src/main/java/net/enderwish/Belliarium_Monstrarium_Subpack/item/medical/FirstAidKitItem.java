package net.enderwish.Belliarium_Monstrarium_Subpack.item.medical;

import net.enderwish.Belliarium_Monstrarium_Subpack.core.ModDataComponents;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.medical.FirstAidKitContents;
import net.enderwish.Belliarium_Monstrarium_Subpack.network.FirstAidKitContentsPacket;
import net.enderwish.Belliarium_Monstrarium_Subpack.network.ModMessages;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class FirstAidKitItem extends Item {

    public FirstAidKitItem(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack kitStack = player.getItemInHand(hand);
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            FirstAidKitContents contents = kitStack.getOrDefault(
                    ModDataComponents.FIRST_AID_KIT_CONTENTS.get(), FirstAidKitContents.empty());
            ModMessages.sendToPlayer(new FirstAidKitContentsPacket(hand.ordinal(), contents.items()), serverPlayer);
        }
        return InteractionResultHolder.success(kitStack);
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack kitStack, ItemStack otherStack, Slot slot,
                                            ClickAction action, Player player, SlotAccess cursorAccess) {
        if (action != ClickAction.PRIMARY) return false;
        if (otherStack.isEmpty() || !isMed(otherStack)) return false;

        FirstAidKitContents contents = kitStack.getOrDefault(
                ModDataComponents.FIRST_AID_KIT_CONTENTS.get(), FirstAidKitContents.empty());

        boolean isAdrenaline = otherStack.getItem() instanceof AdrenalineItem;
        int targetSlot = isAdrenaline
                ? (contents.getSlot(FirstAidKitContents.ADRENALINE_SLOT_INDEX).isEmpty()
                ? FirstAidKitContents.ADRENALINE_SLOT_INDEX : -1)
                : findFirstEmptyNonAdrenalineSlot(contents);

        if (targetSlot < 0) {
            if (!player.level().isClientSide()) {
                player.displayClientMessage(Component.literal(
                        isAdrenaline ? "The kit already holds Adrenaline." : "The kit has no room for that."), true);
            }
            return true;
        }

        kitStack.set(ModDataComponents.FIRST_AID_KIT_CONTENTS.get(),
                contents.withSlot(targetSlot, otherStack.copyWithCount(1)));
        otherStack.shrink(1);
        cursorAccess.set(otherStack.isEmpty() ? ItemStack.EMPTY : otherStack);

        player.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 0.8F + player.level().getRandom().nextFloat() * 0.4F);
        return true;
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
