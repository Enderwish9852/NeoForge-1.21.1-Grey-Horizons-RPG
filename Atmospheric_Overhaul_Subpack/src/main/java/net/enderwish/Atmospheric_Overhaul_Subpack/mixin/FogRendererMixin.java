package net.enderwish.Atmospheric_Overhaul_Subpack.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.enderwish.Atmospheric_Overhaul_Subpack.client.ClientSeasonState;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * FogRendererMixin
 *
 * Only activates during special weathers — blizzard and heatwave.
 *
 * Blizzard: dense white fog, near-zero visibility
 * Heatwave: warm orange-yellow haze, reduced visibility
 *
 * During all other weathers this mixin does nothing —
 * vanilla fog renders normally.
 */
@Mixin(FogRenderer.class)
public class FogRendererMixin {

    @Inject(
            method = "setupColor",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void onSetupFogColor(Camera camera,
                                        float partialTick,
                                        net.minecraft.client.multiplayer.ClientLevel level,
                                        int renderDistanceChunks,
                                        float darkenWorldAmount,
                                        CallbackInfo ci) {

        String weatherId = ClientSeasonState.getWeatherId();

        switch (weatherId) {
            case "blizzard" -> applyBlizzardFog();
            case "heatwave" -> applyHeatwaveFog();
            // All other weathers — do nothing, vanilla fog handles it
        }
    }

    @Inject(
            method = "setupFog",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void onSetupFogDistance(Camera camera,
                                           FogRenderer.FogMode fogMode,
                                           float renderDistanceChunks,
                                           boolean isFoggy,
                                           float partialTick,
                                           CallbackInfo ci) {

        String weatherId = ClientSeasonState.getWeatherId();
        float intensity  = ClientSeasonState.getIntensity();

        switch (weatherId) {
            case "blizzard" -> applyBlizzardFogDistance(intensity);
            case "heatwave" -> applyHeatwaveFogDistance(intensity);
        }
    }

    // ── Blizzard ──────────────────────────────────────────────────────────────

    /**
     * Dense white fog — near whiteout conditions.
     * RGB: (0.92, 0.95, 1.0) — cold white with slight blue tint
     */
    private static void applyBlizzardFog() {
        RenderSystem.clearColor(0.92f, 0.95f, 1.0f, 1.0f);
    }

    /**
     * Very short fog distance for blizzard — near zero visibility.
     * Start: 2 blocks, End: 10 blocks
     */
    private static void applyBlizzardFogDistance(float intensity) {
        RenderSystem.setShaderFogStart(2.0f);
        RenderSystem.setShaderFogEnd(10.0f * (1.0f - intensity * 0.5f));
    }

    // ── Heatwave ──────────────────────────────────────────────────────────────

    /**
     * Warm orange-yellow haze.
     * RGB: (1.0, 0.85, 0.55) — desert heat shimmer
     */
    private static void applyHeatwaveFog() {
        RenderSystem.clearColor(1.0f, 0.85f, 0.55f, 1.0f);
    }

    /**
     * Moderate fog distance for heatwave — visibility reduced but not zero.
     * Start: 20 blocks, End: 60 blocks
     */
    private static void applyHeatwaveFogDistance(float intensity) {
        RenderSystem.setShaderFogStart(20.0f);
        RenderSystem.setShaderFogEnd(60.0f * (1.0f - intensity * 0.3f));
    }
}