package net.enderwish.Atmospheric_Overhaul_Subpack.mixin;

import net.enderwish.Atmospheric_Overhaul_Subpack.client.ClientSeasonState;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * WeatherParticleMixin
 *
 * Cancels vanilla's ambient weather particle spawning (rain splashes,
 * snow etc.) whenever OUR system is precipitating — we fully own that
 * visual now via RainDropParticle + RainSplashParticle + the spawner.
 *
 * BUGFIX: previously only cancelled in HOT biomes (to stop desert rain
 * visuals specifically) — vanilla's own rain splashes kept spawning in
 * every other biome the whole time. Now cancels unconditionally whenever
 * precipitating, since hot-biome suppression is already handled
 * separately by ClientWeatherHandler.isPrecipitationVisible().
 */
@Mixin(ClientLevel.class)
public class WeatherParticleMixin {

    @Inject(
            method = "animateTick",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onAnimateTick(int posX, int posY, int posZ, CallbackInfo ci) {
        if (ClientSeasonState.isPrecipitating()) {
            ci.cancel();
        }
    }
}
