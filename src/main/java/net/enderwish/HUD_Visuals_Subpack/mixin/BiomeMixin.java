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
     * GRASS COLORS: Uses ClientColorHandler to shift colors based on Season.
     */
    @Inject(method = "getGrassColor", at = @At("RETURN"), cancellable = true)
    private void gh_onGetGrass(double x, double z, CallbackInfoReturnable<Integer> cir) {
        // Only run on Client side
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            Holder<Biome> holder = Holder.direct((Biome) (Object) this);
            int seasonalColor = ClientColorHandler.modifyGrassColor(holder, cir.getReturnValue());
            cir.setReturnValue(seasonalColor);
        }
    }

    /**
     * FOLIAGE COLORS: Changes leaf colors (e.g., Orange in Autumn).
     */
    @Inject(method = "getFoliageColor", at = @At("RETURN"), cancellable = true)
    private void gh_onGetFoliage(CallbackInfoReturnable<Integer> cir) {
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            Holder<Biome> holder = Holder.direct((Biome) (Object) this);
            int seasonalColor = ClientColorHandler.modifyFoliageColor(holder, cir.getReturnValue());
            cir.setReturnValue(seasonalColor);
        }
    }

    /**
     * SNOW LAYERS: Determines if a block can have a snow layer.
     * Logic: Winter = Yes. Early Spring = Only in 15% random patches.
     */
    @Inject(method = "shouldSnow", at = @At("HEAD"), cancellable = true)
    private void gh_onShouldSnow(LevelReader levelReader, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        Level level = gh_getLevel(levelReader);
        if (level != null && level.dimension() == Level.OVERWORLD) {

            // If it's Winter or a tagged Snow/Blizzard weather
            if (ClimateHooks.isColdToFreeze(level)) {
                if (pos.getY() >= level.getSeaLevel() && levelReader.canSeeSky(pos)) {
                    cir.setReturnValue(true);
                }
            }
            // If it's the "Initial Thaw" period, only allow snow in pre-set patches
            else if (ClimateHooks.isThawingPeriod(level) && ClimateHooks.isPosInSnowPatch(pos)) {
                if (pos.getY() >= level.getSeaLevel() && levelReader.canSeeSky(pos)) {
                    cir.setReturnValue(true);
                }
            }
        }
    }

    /**
     * ICE FORMATION: Redirects the 'warmEnoughToRain' check.
     * This allows ice to form even in "warm" biomes if your mod says it's Winter or a Thaw Night.
     */
    @Redirect(
            method = "shouldFreeze(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Z)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/biome/Biome;warmEnoughToRain(Lnet/minecraft/core/BlockPos;)Z")
    )
    private boolean gh_redirectWarmCheck(Biome instance, BlockPos pos, LevelReader levelReader) {
        Level level = gh_getLevel(levelReader);
        if (level != null && level.dimension() == Level.OVERWORLD) {
            // If it's a Thaw Night, it's NOT "warm enough to rain" (so it freezes)
            if (ClimateHooks.isNightThaw(level)) return false;

            // Otherwise, it's only warm enough to rain if it's NOT freezing weather
            return !ClimateHooks.isColdToFreeze(level);
        }
        return instance.warmEnoughToRain(pos);
    }

    /**
     * PARTICLE TYPE: Controls if the sky drops Rain or Snow particles.
     */
    @Redirect(
            method = "getPrecipitationAt",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/biome/Biome;coldEnoughToSnow(Lnet/minecraft/core/BlockPos;)Z")
    )
    private boolean gh_redirectColdCheck(Biome instance, BlockPos pos, BlockPos posAgain) {
        // Pass null to gh_getLevel because getPrecipitationAt doesn't provide a reader,
        // we will fall back to the Minecraft Instance on client or level data on server.
        Level level = gh_getLevel(null);
        if (level != null && level.dimension() == Level.OVERWORLD) {
            return ClimateHooks.isColdToFreeze(level) || ClimateHooks.isNightThaw(level);
        }
        return instance.coldEnoughToSnow(pos);
    }

    @Unique
    private Level gh_getLevel(LevelReader reader) {
        if (reader instanceof Level level) return level;
        // Client-side fallback
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            return net.minecraft.client.Minecraft.getInstance().level;
        }
        return null;
    }
}