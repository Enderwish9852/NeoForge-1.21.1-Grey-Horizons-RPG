package net.enderwish.HUD_Visuals_Subpack.mixin;

import net.enderwish.HUD_Visuals_Subpack.api.ClimateHooks;
import net.enderwish.HUD_Visuals_Subpack.client.ClientColorHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Biome.class)
public abstract class BiomeMixin {

    /**
     * GRASS COLORS: Uses your existing modifyGrassColor method.
     */
    @Inject(method = "getGrassColor", at = @At("RETURN"), cancellable = true)
    private void gh_onGetGrass(double x, double z, CallbackInfoReturnable<Integer> cir) {
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            // We cast "this" to a Holder so your modifyGrassColor method is happy
            Holder<Biome> holder = Holder.direct((Biome) (Object) this);
            int originalColor = cir.getReturnValue();

            // Calls your existing method: public static int modifyGrassColor(Holder<Biome> biome, int originalColor)
            int seasonalColor = ClientColorHandler.modifyGrassColor(holder, originalColor);
            cir.setReturnValue(seasonalColor);
        }
    }

    /**
     * FOLIAGE COLORS: Uses your existing modifyFoliageColor method.
     */
    @Inject(method = "getFoliageColor", at = @At("RETURN"), cancellable = true)
    private void gh_onGetFoliage(CallbackInfoReturnable<Integer> cir) {
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            Holder<Biome> holder = Holder.direct((Biome) (Object) this);
            int originalColor = cir.getReturnValue();

            // Calls your existing method: public static int modifyFoliageColor(Holder<Biome> biome, int originalColor)
            int seasonalColor = ClientColorHandler.modifyFoliageColor(holder, originalColor);
            cir.setReturnValue(seasonalColor);
        }
    }

    /* --- Your Existing Snow/Ice/Precipitation Logic Below --- */

    @Inject(method = "shouldSnow", at = @At("HEAD"), cancellable = true)
    private void gh_onShouldSnow(LevelReader levelReader, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        Level level = gh_getLevel(levelReader);
        if (level != null && level.dimension() == Level.OVERWORLD) {
            if (ClimateHooks.isColdToFreeze(level)) {
                if (pos.getY() >= 0 && levelReader.canSeeSky(pos)) {
                    cir.setReturnValue(true);
                }
            }
        }
    }

    @Redirect(
            method = "shouldFreeze(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Z)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/biome/Biome;warmEnoughToRain(Lnet/minecraft/core/BlockPos;)Z")
    )
    private boolean gh_redirectWarmCheck(Biome instance, BlockPos pos, LevelReader levelReader) {
        Level level = gh_getLevel(levelReader);
        if (level != null && level.dimension() == Level.OVERWORLD) {
            return !ClimateHooks.isColdToFreeze(level);
        }
        return instance.warmEnoughToRain(pos);
    }

    @Redirect(
            method = "getPrecipitationAt",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/biome/Biome;coldEnoughToSnow(Lnet/minecraft/core/BlockPos;)Z")
    )
    private boolean gh_redirectColdCheck(Biome instance, BlockPos pos, BlockPos posAgain) {
        Level level = gh_getLevel(null);
        if (level != null && level.dimension() == Level.OVERWORLD) {
            return ClimateHooks.isColdToFreeze(level);
        }
        return instance.coldEnoughToSnow(pos);
    }

    @Unique
    private Level gh_getLevel(LevelReader reader) {
        if (reader instanceof Level level) return level;
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            return net.minecraft.client.Minecraft.getInstance().level;
        }
        return null;
    }
}