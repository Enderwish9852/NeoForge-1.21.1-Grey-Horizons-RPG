package net.enderwish.HUD_Visuals_Subpack.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.enderwish.HUD_Visuals_Subpack.api.WeatherRegistry;
import net.enderwish.HUD_Visuals_Subpack.api.WeatherType;
import net.enderwish.HUD_Visuals_Subpack.client.ClientClimateCache;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FogRenderer.class)
public class FogRendererMixin {

    @Unique
    private static float gh_currentR, gh_currentG, gh_currentB;

    /**
     * Injects into the fog color setup to apply custom weather colors (Heatwaves, Pollen, Storms).
     */
    @Inject(method = "setupColor", at = @At("RETURN"))
    private static void gh_onSetupColor(Camera camera, float partialTicks, ClientLevel level, int renderDistanceChunks, float bossColorModifier, CallbackInfo ci) {
        var data = ClientClimateCache.getInstance();
        if (data == null) return;

        WeatherType type = WeatherRegistry.getById(data.weather());
        if (type == null) return;

        // Get the target RGB from our Hex color in the Registry
        float targetR = gh_getFloatR(type.fogColor());
        float targetG = gh_getFloatG(type.fogColor());
        float targetB = gh_getFloatB(type.fogColor());

        // Smooth transition (Lerp) so colors don't "pop"
        // 0.05f provides a nice 2-3 second fade between weather types
        gh_currentR += (targetR - gh_currentR) * 0.05f;
        gh_currentG += (targetG - gh_currentG) * 0.05f;
        gh_currentB += (targetB - gh_currentB) * 0.05f;

        RenderSystem.clearColor(gh_currentR, gh_currentG, gh_currentB, 1.0f);
    }

    @Unique
    private static float gh_getFloatR(int hex) { return (float)(hex >> 16 & 255) / 255.0F; }
    @Unique
    private static float gh_getFloatG(int hex) { return (float)(hex >> 8 & 255) / 255.0F; }
    @Unique
    private static float gh_getFloatB(int hex) { return (float)(hex & 255) / 255.0F; }
}