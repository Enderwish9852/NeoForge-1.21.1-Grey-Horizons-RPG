package net.enderwish.Atmospheric_Overhaul_Subpack.mixin;

import net.enderwish.Atmospheric_Overhaul_Subpack.client.ClientSeasonState;
import net.enderwish.Atmospheric_Overhaul_Subpack.client.ClientWeatherHandler;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * WeatherParticleMixin
 *
 * Intercepts the method that spawns rain/snow particles each tick.
 * In hot biomes (desert, savanna, badlands) — cancels particle spawning.
 *
 * This approach is safe because:
 * - It never touches setRainLevel or vanilla weather state
 * - Clouds, darkness, thunder sound, mob spawning all work normally
 * - Rain fades naturally when entering a desert (existing particles finish)
 * - Rain resumes naturally when leaving the desert
 * - No ghost rain / broken state bugs
 */
@Mixin(ClientLevel.class)
public class WeatherParticleMixin {

    @Inject(
            method = "animateTick",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onAnimateTick(int posX, int posY, int posZ, CallbackInfo ci) {
        if (ClientWeatherHandler.getCurrentCategory()
                == ClientWeatherHandler.BiomeCategory.HOT) {
            if (ClientSeasonState.isPrecipitating()) {
                ci.cancel();
            }
        }
    }
}
