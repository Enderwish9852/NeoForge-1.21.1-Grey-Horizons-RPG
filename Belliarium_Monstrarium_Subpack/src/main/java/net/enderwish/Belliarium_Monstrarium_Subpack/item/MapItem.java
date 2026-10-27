package net.enderwish.Belliarium_Monstrarium_Subpack.item;

import net.enderwish.Belliarium_Monstrarium_Subpack.client.MapScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * MapItem
 *
 * The legendary map: one per adventure, creative only. Right-click while
 * holding it (or press the M keybind while it is in the hotbar) opens the
 * full-screen map GUI.
 *
 * The map itself is a client-side Screen -- no container, no server menu --
 * so opening it is purely client-side. The server only needs to know the
 * item exists.
 */
public class MapItem extends Item {

    public MapItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide() && hasMapInHotbar(player)) {
            Minecraft.getInstance().setScreen(new MapScreen());
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /** True when the legendary map is somewhere in the player's hotbar. */
    private static boolean hasMapInHotbar(Player player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof MapItem) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal("Legendary Map")
                .withStyle(style -> style.withColor(0xFFAA00).withBold(true));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Legendary").withStyle(style -> style.withColor(0xFFAA00)));
        tooltip.add(Component.literal("One map for the entire adventure.").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("Press M or right-click to open.").withStyle(ChatFormatting.GRAY));
    }
}
