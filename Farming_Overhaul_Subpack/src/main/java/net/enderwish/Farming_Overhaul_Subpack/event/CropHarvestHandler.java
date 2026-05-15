package net.enderwish.Farming_Overhaul_Subpack.event;

import net.enderwish.Farming_Overhaul_Subpack.FarmingOverhaulSubpack;
import net.enderwish.Farming_Overhaul_Subpack.core.crop.CropDefinition;
import net.enderwish.Farming_Overhaul_Subpack.core.crop.CropRegistry;
import net.enderwish.Farming_Overhaul_Subpack.core.spoilage.ModDataComponents;
import net.enderwish.Farming_Overhaul_Subpack.core.spoilage.SpoilageComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

/**
 * CropHarvestHandler
 *
 * Listens to block drops and attaches a fresh SpoilageComponent
 * to any dropped item that has a CropDefinition in CropRegistry.
 *
 * This is the entry point for the spoilage system —
 * every spoilable item starts its life here with a full bar.
 *
 * Only fully grown crops trigger this — vanilla handles the
 * growth stage check before dropping items.
 */
@EventBusSubscriber(modid = FarmingOverhaulSubpack.MODID, bus = EventBusSubscriber.Bus.GAME)
public class CropHarvestHandler {

    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        BlockState state = event.getState();

        // Only care about crop blocks
        if (!(state.getBlock() instanceof CropBlock cropBlock)) return;

        // Only attach spoilage to fully grown crops
        if (!cropBlock.isMaxAge(state)) return;

        // Process each dropped item entity
        event.getDrops().forEach(itemEntity -> {
            ItemStack stack = itemEntity.getItem();
            if (stack.isEmpty()) return;

            // Get item ID and check if it is registered in our crop system
            String itemId = stack.getItem()
                    .builtInRegistryHolder()
                    .key().location().getPath();

            if (!CropRegistry.INSTANCE.isRegistered(itemId)) return;

            CropDefinition def = CropRegistry.INSTANCE.getByName(itemId);

            // Attach a fresh SpoilageComponent — spoilage starts now
            stack.set(
                    ModDataComponents.SPOILAGE.get(),
                    SpoilageComponent.fresh(def.spoilTicks())
            );
        });
    }
}
