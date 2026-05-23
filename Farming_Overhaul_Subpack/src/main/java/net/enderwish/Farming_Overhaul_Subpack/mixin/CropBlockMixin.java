package net.enderwish.Farming_Overhaul_Subpack.mixin;

import net.enderwish.Farming_Overhaul_Subpack.core.crop.CropDefinition;
import net.enderwish.Farming_Overhaul_Subpack.core.crop.CropRegistry;
import net.enderwish.Atmospheric_Overhaul_Subpack.api.SeasonsAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.CommonHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * CropBlockMixin
 *
 * Injects into CommonHooks.canCropGrow() which is the NeoForge
 * hook called inside CropBlock.randomTick() before any growth happens.
 *
 * This is the correct and proper place to cancel crop growth in 1.21.1.
 * The crop never advances even a single stage — no reset needed.
 *
 * Only imports SeasonsAPI — never touches Atmospheric internals.
 */
@Mixin(value = CommonHooks.class, remap = false)
public class CropBlockMixin {

    @Inject(
            method = "canCropGrow",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void onCanCropGrow(Level level, BlockPos pos,
                                      BlockState state, boolean def,
                                      CallbackInfoReturnable<Boolean> cir) {

        // Server side only
        if (!(level instanceof ServerLevel serverLevel)) return;

        // Get crop ID from block registry name
        String cropId = state.getBlock()
                .builtInRegistryHolder()
                .key().location().getPath();

        // Not registered in our system — let vanilla handle it
        if (!CropRegistry.INSTANCE.isRegistered(cropId)) return;

        CropDefinition cropDef = CropRegistry.INSTANCE.getByName(cropId);

        // Year round crops always grow
        if (cropDef.yearRound()) return;

        // Check current season via SeasonsAPI only
        String currentSeason = SeasonsAPI.getSeason(serverLevel).name();

        // Cancel growth if wrong season
        if (!cropDef.canGrowIn(currentSeason)) {
            cir.setReturnValue(false);
        }
    }
}
