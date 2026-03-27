package net.enderwish.HUD_Visuals_Subpack.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.enderwish.HUD_Visuals_Subpack.client.ClientWeatherHandler;
import net.enderwish.HUD_Visuals_Subpack.core.WeatherType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.DeltaTracker;

/**
 * Handles the full-screen visual effects for special weather types.
 * Reuses vanilla's frozen scope texture with custom color tinting.
 */
public class WeatherOverlayRenderer {

    // The vanilla 'frozen_scope' texture provides a perfect vignette/frost effect around the edges
    private static final ResourceLocation FROST_TEXTURE = ResourceLocation.withDefaultNamespace("textures/misc/frozen_scope.png");

    // State for smooth fading
    private static float overlayAlpha = 0.0f;
    private static final float FADE_SPEED = 0.015f;

    /**
     * Called by the RegisterGuiLayersEvent in HUDVisualsSubpack.
     * This replaces the old @SubscribeEvent pattern to work with NeoForge's layer system.
     */
    public static void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();

        // Safety checks: player must exist and HUD must not be hidden (F1)
        if (mc.player == null || mc.options.hideGui) return;

        // Determine if we should be showing a weather effect
        WeatherType currentType = ClientWeatherHandler.getCurrentType();
        boolean isActiveWeather = (currentType == WeatherType.BLIZZARD || currentType == WeatherType.HEATWAVE);

        // Update the alpha transparency for smooth transitions
        if (isActiveWeather) {
            if (overlayAlpha < 1.0f) {
                overlayAlpha = Math.min(1.0f, overlayAlpha + FADE_SPEED);
            }
        } else {
            if (overlayAlpha > 0.0f) {
                overlayAlpha = Math.max(0.0f, overlayAlpha - FADE_SPEED);
            }
        }

        // Only proceed to render if there is something visible
        if (overlayAlpha > 0.0f) {
            drawOverlay(guiGraphics, currentType, overlayAlpha);
        }
    }

    private static void drawOverlay(GuiGraphics guiGraphics, WeatherType type, float alpha) {
        int width = guiGraphics.guiWidth();
        int height = guiGraphics.guiHeight();

        // 1. Prepare RenderSystem state
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);

        // 2. Set the tint color based on the specific weather type
        if (type == WeatherType.BLIZZARD) {
            // Cool white-blue for freezing conditions
            guiGraphics.setColor(0.85f, 0.95f, 1.0f, alpha);
        } else if (type == WeatherType.HEATWAVE) {
            // Intense orange-red for extreme heat
            guiGraphics.setColor(1.0f, 0.45f, 0.0f, alpha * 0.7f); // Reduced alpha slightly for visibility
        } else {
            // Fallback for when fading out from a weather type that just ended
            guiGraphics.setColor(1.0f, 1.0f, 1.0f, alpha);
        }

        // 3. Draw the texture to cover the whole screen
        // In 1.21.1, we use guiGraphics.blit for drawing fullscreen textures
        guiGraphics.blit(FROST_TEXTURE, 0, 0, -90, 0.0F, 0.0F, width, height, width, height);

        // 4. Cleanup to prevent tinting/state leakage to other UI elements
        guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }
}