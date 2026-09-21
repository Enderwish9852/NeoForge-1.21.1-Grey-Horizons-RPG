package net.enderwish.Atmospheric_Overhaul_Subpack.mixin;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class WeatherParticleMixin {

    @Inject(method = "tickRain", at = @At("HEAD"), cancellable = true)
    private void onTickRain(Camera camera, CallbackInfo ci) {
        ci.cancel();
    }
}
