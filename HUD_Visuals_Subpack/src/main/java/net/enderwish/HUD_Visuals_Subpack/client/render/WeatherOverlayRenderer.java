package net.enderwish.HUD_Visuals_Subpack.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.enderwish.HUD_Visuals_Subpack.api.ClimateData;
import net.enderwish.HUD_Visuals_Subpack.client.ClientClimateCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Reuses vanilla's frozen scope texture with custom color tinting.
 * Refactored for the Unified Climate Engine.
 */
public class WeatherOverlayRenderer {

    private static final ResourceLocation FROST_TEXTURE = ResourceLocation.withDefaultNamespace("textures/misc/frozen_scope.png");

    private static float overlayAlpha = 0.0f;
    private static final float FADE_SPEED = 0.01f; // Slightly slower for more cinematic feel

    public static void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        ClimateData data = ClientClimateCache.get();
        String weatherId = data.weather();

        // Logic check: Is it a weather type that needs a screen overlay?
        boolean shouldShow = weatherId.contains("blizzard") || weatherId.contains("heatwave");

        // Smooth Alpha Transition
        if (shouldShow) {
            overlayAlpha = Math.min(data.intensity(), overlayAlpha + FADE_SPEED);
        } else {
            overlayAlpha = Math.max(0.0f, overlayAlpha - FADE_SPEED);
        }

        if (overlayAlpha > 0.001f) {
            drawOverlay(guiGraphics, weatherId, overlayAlpha);
        }
    }

    private static void drawOverlay(GuiGraphics guiGraphics, String weatherId, float alpha) {
        int width = guiGraphics.guiWidth();
        int height = guiGraphics.guiHeight();

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);

        // Professional Tinting Logic
        if (weatherId.contains("blizzard")) {
            // Cold, icy blue-white
            guiGraphics.setColor(0.7f, 0.85f, 1.0f, alpha * 0.9f);
        } else if (weatherId.contains("heatwave")) {
            // Burning orange (Lower alpha so the player can still see the world)
            guiGraphics.setColor(1.0f, 0.4f, 0.0f, alpha * 0.5f);
        } else {
            // Default/Fading out
            guiGraphics.setColor(1.0f, 1.0f, 1.0f, alpha);
        }

        // Blit to screen
        guiGraphics.blit(FROST_TEXTURE, 0, 0, -90, 0.0F, 0.0F, width, height, width, height);

        // Cleanup
        guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }
}