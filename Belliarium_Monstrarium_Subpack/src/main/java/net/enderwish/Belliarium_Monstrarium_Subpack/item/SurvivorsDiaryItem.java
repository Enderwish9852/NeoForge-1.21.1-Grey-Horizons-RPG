package net.enderwish.Belliarium_Monstrarium_Subpack.item;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumConfig;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.ModDataComponents;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.diary.DiaryBook;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.diary.DiaryContent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * SurvivorsDiaryItem
 *
 * Legendary gold name while world tracking is enabled; a plain gray notebook when
 * the config switch is off.
 *
 * VERIFY IF COMPILE FAILS -- Item#appendHoverText(ItemStack, Item.TooltipContext, List, TooltipFlag),
 * InteractionResultHolder#sidedSuccess.
 */
public class SurvivorsDiaryItem extends Item {

    public SurvivorsDiaryItem(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            DiaryBook.openDiary(serverPlayer, hand, stack);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public Component getName(ItemStack stack) {
        Component name = Component.translatable(this.getDescriptionId(stack));
        return BelliariumConfig.trackingEnabled()
                ? name.copy().withStyle(style -> style.withColor(0xFFAA00))
                : name.copy().withStyle(ChatFormatting.GRAY);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        DiaryContent content = stack.getOrDefault(ModDataComponents.DIARY_CONTENT.get(), DiaryContent.EMPTY);
        if (BelliariumConfig.trackingEnabled()) {
            tooltip.add(Component.literal("Legendary").withStyle(style -> style.withColor(0xFFAA00)));
            tooltip.add(Component.literal("Open it to begin the day.").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(Component.literal("A plain notebook.").withStyle(ChatFormatting.DARK_GRAY));
        }
        tooltip.add(Component.literal("Entries: " + content.entries().size()).withStyle(ChatFormatting.DARK_GRAY));
    }
}
