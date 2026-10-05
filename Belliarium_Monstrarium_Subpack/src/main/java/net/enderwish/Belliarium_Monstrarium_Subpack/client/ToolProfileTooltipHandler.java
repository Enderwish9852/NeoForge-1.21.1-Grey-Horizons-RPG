package net.enderwish.Belliarium_Monstrarium_Subpack.client;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.ItemRarity;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.ModDataComponents;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.RepairChargesComponent;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.ToolProfileDefinition;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.ToolProfileRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

/**
 * ToolProfileTooltipHandler
 *
 * Same ItemTooltipEvent hook already proven in this project (Farming's own
 * SpoilageTooltipHandler). Only items with a ToolProfileRegistry entry are
 * touched -- everything else renders as plain vanilla.
 *
 * NOT implemented -- a colored BORDER/outline around the whole tooltip
 * panel. ItemTooltipEvent only lets a mod add/edit TEXT LINES; it has no
 * hook into the panel's own background/border rendering. That needs a
 * deeper client renderer hook I don't have confirmed 1.21.1 source for --
 * flagged as an open question below rather than guessed. Name color (done
 * here) and the repair-charge dots are both fully achievable this way.
 */
@EventBusSubscriber(modid = BelliariumMonstrariumSubpack.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ToolProfileTooltipHandler {

    private static final String FILLED_DOT = "\u25CF"; // ●
    private static final String EMPTY_DOT = "\u25CB";  // ○

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM
                .getKey(stack.getItem()).toString();

        ToolProfileRegistry.INSTANCE.getProfile(itemId).ifPresent(profile -> {
            List<Component> tooltip = event.getToolTip();
            ItemRarity rarity = profile.rarity();

            if (!tooltip.isEmpty()) {
                tooltip.set(0, tooltip.get(0).copy().withStyle(style -> style.withColor(rarity.getColor())));
            }

            RepairChargesComponent charges = stack.getOrDefault(
                    ModDataComponents.REPAIR_CHARGES.get(),
                    new RepairChargesComponent(rarity.getMaxRepairCharges()));
            StringBuilder dots = new StringBuilder();
            for (int i = 0; i < rarity.getMaxRepairCharges(); i++) {
                dots.append(i < charges.chargesRemaining() ? FILLED_DOT : EMPTY_DOT).append(' ');
            }
            tooltip.add(1, Component.literal(dots.toString().trim()).withStyle(ChatFormatting.GRAY));

            String rarityName = rarity.name().charAt(0) + rarity.name().substring(1).toLowerCase();
            tooltip.add(2, Component.literal(rarityName + ": item can be repaired "
                    + rarity.getMaxRepairCharges() + " time" + (rarity.getMaxRepairCharges() == 1 ? "" : "s")
                    + " total.").withStyle(ChatFormatting.DARK_GRAY));

            int insertAt = 3;
            for (ToolProfileDefinition.ToolTrait trait : profile.traits()) {
                tooltip.add(insertAt++, Component.literal(trait.name() + ": " + trait.description())
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
        });
    }
}
