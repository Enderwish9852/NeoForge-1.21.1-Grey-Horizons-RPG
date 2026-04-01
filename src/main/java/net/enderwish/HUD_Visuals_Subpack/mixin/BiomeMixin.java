package net.enderwish.HUD_Visuals_Subpack.mixin;

import net.enderwish.HUD_Visuals_Subpack.client.ClientColorHandler;
import net.enderwish.HUD_Visuals_Subpack.client.ClientSeasonHandler;
import net.enderwish.HUD_Visuals_Subpack.event.SeasonManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Biome.class)
public abstract class BiomeMixin {

    @Inject(method = "shouldSnow", at = @At("HEAD"), cancellable = true)
    private void onShouldSnow(LevelReader levelReader, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (levelReader instanceof Level level && level.dimension() == Level.OVERWORLD) {
            // Use our new "Serene-Style" Hook
            float temp = SeasonManager.getAdjustedTemperature(level, pos);

            // If temperature is below freezing (0.15f), force snow
            if (temp < 0.15f) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "shouldFreeze(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Z)Z", at = @At("HEAD"), cancellable = true)
    private void onShouldFreeze(LevelReader levelReader, BlockPos pos, boolean mustBeAtEdge, CallbackInfoReturnable<Boolean> cir) {
        if (levelReader instanceof Level level && level.dimension() == Level.OVERWORLD) {
            if (!level.getBlockState(pos).is(Blocks.WATER)) return;

            float temp = SeasonManager.getAdjustedTemperature(level, pos);

            // 1. DEEP FREEZE: Hard freeze everything
            if (temp <= -0.5f) {
                cir.setReturnValue(true);
            }
            // 2. CREEPING ICE: Only freeze near banks
            else if (temp < 0.15f) {
                if (gh_isNearSolidBlock(level, pos)) {
                    cir.setReturnValue(true);
                } else {
                    cir.setReturnValue(false);
                }
            }
            // 3. THAW: Force melt in warm weather
            else if (temp > 0.2f) {
                cir.setReturnValue(false);
            }
        }
    }
    @Inject(method = "getPrecipitationAt", at = @At("HEAD"), cancellable = true)
    private void onGetPrecipitationAt(BlockPos pos, int seaLevel, CallbackInfoReturnable<Biome.Precipitation> cir) {
        // We need to check the level, but Biome doesn't have a direct reference to it.
        // We use a workaround or check if we are on the client/server through a helper.

        // This is a simplified check - if the biome HAS precipitation at all...
        if (((Biome)(Object)this).hasPrecipitation()) {
            // We use our SeasonManager logic to see if it's cold enough right now
            // Note: Since Mixins in Biome don't have 'Level', we usually check the
            // ClientSeasonHandler on the client side or a ThreadLocal on the server.

            if (ClientSeasonHandler.isSnowing()) {
                cir.setReturnValue(Biome.Precipitation.SNOW);
            }
        }
    }

    @Unique
    private boolean gh_isNearSolidBlock(Level level, BlockPos pos) {
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
        // We cast 'this' to Biome and get its holder to check for Hot Biomes
        Biome biome = (Biome) (Object) this;
        Holder<Biome> holder = Holder.direct(biome);

        cir.setReturnValue(ClientColorHandler.modifyGrassColor(holder, cir.getReturnValue()));
    }

    @Inject(method = "getFoliageColor", at = @At("RETURN"), cancellable = true)
    private void onGetFoliageColor(CallbackInfoReturnable<Integer> cir) {
        Biome biome = (Biome) (Object) this;
        Holder<Biome> holder = Holder.direct(biome);

        cir.setReturnValue(ClientColorHandler.modifyFoliageColor(holder, cir.getReturnValue()));
    }
}