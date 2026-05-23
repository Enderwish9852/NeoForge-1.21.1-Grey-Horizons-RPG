package net.enderwish.Farming_Overhaul_Subpack.event;

import net.enderwish.Farming_Overhaul_Subpack.FarmingOverhaulSubpack;
import net.enderwish.Farming_Overhaul_Subpack.core.food.FoodRegistry;
import net.enderwish.Farming_Overhaul_Subpack.core.crop.CropRegistry;
import net.enderwish.Farming_Overhaul_Subpack.core.spoilage.ModDataComponents;
import net.enderwish.Farming_Overhaul_Subpack.core.spoilage.SpoilageComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * CookingSpoilageHandler
 *
 * Handles spoilage inheritance when a player collects
 * a smelted item from a furnace or smoker.
 *
 * Uses ItemSmeltedEvent which fires when the player
 * takes the output from the furnace result slot.
 *
 * Furnace/Smoker: 10% reduction
 * Campfire handled separately by FoodPickupHandler
 * since campfire drops items into the world.
 */
@EventBusSubscriber(modid = FarmingOverhaulSubpack.MODID, bus = EventBusSubscriber.Bus.GAME)
public class CookingSpoilageHandler {

    // ── Cooking reductions ────────────────────────────────────────────────────

    private static final float FURNACE_REDUCTION  = 0.10f;
    private static final float CAMPFIRE_REDUCTION = 0.05f;

    @SubscribeEvent
    public static void onItemSmelted(PlayerEvent.ItemSmeltedEvent event) {
        if (event.getEntity().level().isClientSide()) return;

        ItemStack output = event.getSmelting();
        if (output.isEmpty()) return;

        // Already has spoilage component — skip
        if (output.has(ModDataComponents.SPOILAGE.get())) return;

        // Get output item ID
        String outputId = output.getItem()
                .builtInRegistryHolder()
                .key().location().getPath();

        // Check if output is spoilable
        int outputMaxTicks = 0;
        if (FoodRegistry.INSTANCE.isRegistered(outputId)) {
            outputMaxTicks = FoodRegistry.INSTANCE.getByName(outputId).spoilTicks();
        } else if (CropRegistry.INSTANCE.isRegistered(outputId)) {
            outputMaxTicks = CropRegistry.INSTANCE.getByName(outputId).spoilTicks();
        }

        // Output not spoilable — nothing to do
        if (outputMaxTicks <= 0) return;

        // For now start at FURNACE_REDUCTION fresh
        // e.g. cooked beef starts at 10% already used
        // This is a placeholder until clay pot is built
        int startTicks = (int) (FURNACE_REDUCTION * outputMaxTicks);
        output.set(
                ModDataComponents.SPOILAGE.get(),
                new SpoilageComponent(startTicks, outputMaxTicks, 1.0f)
        );
    }
}
