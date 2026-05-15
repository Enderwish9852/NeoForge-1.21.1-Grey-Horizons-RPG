package net.enderwish.Farming_Overhaul_Subpack.event;

import net.enderwish.Farming_Overhaul_Subpack.FarmingOverhaulSubpack;
import net.enderwish.Farming_Overhaul_Subpack.core.crop.CropDefinition;
import net.enderwish.Farming_Overhaul_Subpack.core.crop.CropRegistry;
import net.enderwish.Atmospheric_Overhaul_Subpack.api.SeasonsAPI;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.CropBlock;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockGrowFeatureEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * CropGrowthHandler
 *
 * Listens to crop growth events and cancels them if the crop
 * is planted in the wrong season.
 *
 * Off-season behaviour:
 *   - Crop stays at stage 0 visually (just planted look)
 *   - Growth tick is cancelled — no progress made
 *   - Crop does NOT die — it waits for the right season
 *   - When the correct season arrives growth resumes normally
 *
 * Only imports SeasonsAPI — never touches Atmospheric internals.
 */
@EventBusSubscriber(modid = FarmingOverhaulSubpack.MODID, bus = EventBusSubscriber.Bus.GAME)
public class CropGrowthHandler {

    @SubscribeEvent
    public static void onCropGrow(net.neoforged.neoforge.event.level.BlockGrowFeatureEvent event) {
        // Server side only
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        // Only care about crop blocks
        if (!(event.getLevel().getBlockState(event.getPos()).getBlock() instanceof CropBlock)) return;

        // Get the crop ID from the block's registry name
        String cropId = event.getLevel().getBlockState(event.getPos())
                .getBlock().builtInRegistryHolder()
                .key().location().getPath();

        // If not registered in our system let it grow normally
        if (!CropRegistry.INSTANCE.isRegistered(cropId)) return;

        CropDefinition def = CropRegistry.INSTANCE.getByName(cropId);

        // Year round crops always grow
        if (def.yearRound()) return;

        // Check current season via SeasonsAPI — the ONLY atmospheric import allowed
        String currentSeason = SeasonsAPI.getSeason(level).name();

        // Cancel growth if wrong season
        if (!def.canGrowIn(currentSeason)) {
            event.setCanceled(true);
        }
    }
}
