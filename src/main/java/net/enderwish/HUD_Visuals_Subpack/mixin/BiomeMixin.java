package net.enderwish.HUD_Visuals_Subpack.mixin;

import net.enderwish.HUD_Visuals_Subpack.client.ClientColorHandler;
import net.enderwish.HUD_Visuals_Subpack.event.SeasonManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Biome.class)
public class BiomeMixin {

    @Inject(method = "shouldSnow", at = @At("HEAD"), cancellable = true)
    private void onShouldSnow(LevelReader levelReader, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (levelReader instanceof Level level && level.dimension() == Level.OVERWORLD) {
            float temp = SeasonManager.getSubTemperature(level);

            // Vanilla snow threshold is roughly 0.15f
            if (temp < 0.15f) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "shouldFreeze(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Z)Z", at = @At("HEAD"), cancellable = true)
    private void onShouldFreeze(LevelReader levelReader, BlockPos pos, boolean mustBeAtEdge, CallbackInfoReturnable<Boolean> cir) {
        if (levelReader instanceof Level level && level.dimension() == Level.OVERWORLD) {
            // Only proceed if the block is actually water
            if (!level.getBlockState(pos).is(Blocks.WATER)) return;

            float temp = SeasonManager.getSubTemperature(level);

            // 1. DEEP FREEZE (Mid-Winter): Everything freezes
            if (temp <= -0.5f) {
                cir.setReturnValue(true);
            }
            // 2. SLOW FREEZE (Early Winter / Late Autumn): Ice creeps from the banks
            else if (temp < 0.15f) {
                if (isNearSolidBlock(level, pos)) {
                    cir.setReturnValue(true);
                } else {
                    // Middle of the lake stays water during light chill
                    cir.setReturnValue(false);
                }
            }
            // 3. MELTING (Spring/Summer): Force melt by returning false
            else if (temp > 0.2f) {
                cir.setReturnValue(false);
            }
        }
    }

    /**
     * Helper to check if a water block is touching a "bank" (solid ground).
     * This creates the "creeping ice" effect you asked for.
     */
    private boolean isNearSolidBlock(Level level, BlockPos pos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos neighbor = pos.relative(direction);
            if (level.getBlockState(neighbor).isSolidRender(level, neighbor)) {
                return true;
            }
        }
        return false;
    }

    @Inject(method = "getGrassColor", at = @At("RETURN"), cancellable = true)
    private void onGetGrassColor(double x, double z, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(ClientColorHandler.modifyGrassColor(cir.getReturnValue()));
    }

    @Inject(method = "getFoliageColor", at = @At("RETURN"), cancellable = true)
    private void onGetFoliageColor(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(ClientColorHandler.modifyFoliageColor(cir.getReturnValue()));
    }
}