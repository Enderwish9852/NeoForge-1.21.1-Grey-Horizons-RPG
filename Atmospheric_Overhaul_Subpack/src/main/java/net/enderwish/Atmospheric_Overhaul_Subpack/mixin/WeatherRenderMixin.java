package net.enderwish.Atmospheric_Overhaul_Subpack.mixin;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class WeatherRenderMixin {

    private static boolean logDone = false;

    @Inject(method = "renderSnowAndRain", at = @At("HEAD"), cancellable = true)
    private void cancelVanillaSnowAndRain(LightTexture lightTexture, float partialTick,
                                          double camX, double camY, double camZ,
                                          CallbackInfo ci) {
        if (!logDone) {
            System.out.println("[GH DEBUG] WeatherRenderMixin.renderSnowAndRain INJECTED AND FIRING");
            logDone = true;
        }
        // Cancel the entire snow and rain rendering
        ci.cancel();
    }
}