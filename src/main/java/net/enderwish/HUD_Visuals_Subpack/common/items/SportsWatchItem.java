package net.enderwish.HUD_Visuals_Subpack.common.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import java.util.List;

/**
 * The core item for the HUD Visuals subpack.
 */
public class SportsWatchItem extends Item {

    public SportsWatchItem(Properties properties) {
        // Rarity.EPIC makes the item name itself purple.
        super(properties.stacksTo(1).rarity(Rarity.EPIC));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        // Create "Epic " and set it to Purple
        MutableComponent epicPart = Component.translatable("item.gh_hud_visuals.sports_watch.epic")
                .withStyle(ChatFormatting.DARK_PURPLE);

        // Create "key wearable" and explicitly set it to WHITE (Generic color)
        MutableComponent wearablePart = Component.translatable("item.gh_hud_visuals.sports_watch.key_wearable")
                .withStyle(ChatFormatting.GRAY); // Gray is the standard generic tooltip color

        // Epic (Purple) + key wearable (Gray)
        tooltip.add(epicPart.append(wearablePart));

        // Standard gray text
        tooltip.add(Component.translatable("item.gh_hud_visuals.sports_watch.desc")
                .withStyle(ChatFormatting.GRAY));

        tooltip.add(Component.empty());

        // Aqua and Italic
        tooltip.add(Component.translatable("item.gh_hud_visuals.sports_watch.hint")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
    }


}