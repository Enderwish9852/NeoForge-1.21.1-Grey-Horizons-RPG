package net.enderwish.Farming_Overhaul_Subpack.event;

import net.enderwish.Farming_Overhaul_Subpack.FarmingOverhaulSubpack;
import net.enderwish.Farming_Overhaul_Subpack.core.food.FoodRegistry;
import net.enderwish.Farming_Overhaul_Subpack.core.crop.CropRegistry;
import net.enderwish.Farming_Overhaul_Subpack.core.spoilage.ModDataComponents;
import net.enderwish.Farming_Overhaul_Subpack.core.spoilage.SpoilageComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * CraftingSpoilageHandler
 *
 * When a player crafts an item, finds the worst spoilage %
 * among all spoilable ingredients and applies it to the output.
 *
 * Rules:
 *   - Only spoilable ingredients count
 *   - If NO spoilable ingredients exist, output starts fresh
 *   - The highest % spoiled ingredient determines the output start %
 *   - Output % is applied to the output item's own max spoil ticks
 *
 * Example:
 *   Raw beef at 30% spoiled + bread at 10% spoiled = output at 30%
 *   Output max ticks = 5000, so starts at 1500 ticks used (30%)
 */
@EventBusSubscriber(modid = FarmingOverhaulSubpack.MODID, bus = EventBusSubscriber.Bus.GAME)
public class CraftingSpoilageHandler {

    @SubscribeEvent
    public static void onCraft(PlayerEvent.ItemCraftedEvent event) {
        Player player = event.getEntity();

        // Server side only
        if (player.level().isClientSide()) return;

        ItemStack output = event.getCrafting();
        if (output.isEmpty()) return;

        // Get output item ID
        String outputId = output.getItem()
                .builtInRegistryHolder()
                .key().location().getPath();

        // Check if output is a spoilable item
        int outputMaxTicks = 0;
        if (FoodRegistry.INSTANCE.isRegistered(outputId)) {
            outputMaxTicks = FoodRegistry.INSTANCE.getByName(outputId).spoilTicks();
        } else if (CropRegistry.INSTANCE.isRegistered(outputId)) {
            outputMaxTicks = CropRegistry.INSTANCE.getByName(outputId).spoilTicks();
        }

        // Output not spoilable — nothing to do
        if (outputMaxTicks <= 0) return;

        // Scan crafting grid for worst spoilage %
        float worstProgress = 0.0f;
        boolean foundSpoilable = false;

        for (int i = 0; i < event.getInventory().getContainerSize(); i++) {
            ItemStack ingredient = event.getInventory().getItem(i);
            if (ingredient.isEmpty()) continue;

            // Skip the output slot (slot 0)
            if (i == 0) continue;

            SpoilageComponent comp = ingredient.get(ModDataComponents.SPOILAGE.get());
            if (comp == null) continue; // not spoilable — skip

            foundSpoilable = true;
            if (comp.getProgress() > worstProgress) {
                worstProgress = comp.getProgress();
            }
        }

        // No spoilable ingredients — output starts fresh
        if (!foundSpoilable) {
            output.set(
                    ModDataComponents.SPOILAGE.get(),
                    SpoilageComponent.fresh(outputMaxTicks)
            );
            return;
        }

        // Apply worst % to output max ticks
        int inheritedTicks = (int) (worstProgress * outputMaxTicks);
        output.set(
                ModDataComponents.SPOILAGE.get(),
                new SpoilageComponent(inheritedTicks, outputMaxTicks, 1.0f)
        );
    }
}
