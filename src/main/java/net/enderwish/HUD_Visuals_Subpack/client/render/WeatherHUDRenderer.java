package net.enderwish.HUD_Visuals_Subpack.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.enderwish.HUD_Visuals_Subpack.HUDVisualsSubpack;
import net.enderwish.HUD_Visuals_Subpack.client.ClientWeatherHandler;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Finalized NeoForge 1.21.1 HUD Renderer.
 * Handles the 4-Grade Exposure System (Chilly/Warm, Cold/Hot, Freezing/Boiling).
 */
public class WeatherHUDRenderer {

    private static final ResourceLocation VIGNETTE = ResourceLocation.fromNamespaceAndPath(HUDVisualsSubpack.MOD_ID, "textures/gui/vignette.png");
    private static final ResourceLocation BLIZZARD_TEX = ResourceLocation.fromNamespaceAndPath(HUDVisualsSubpack.MOD_ID, "textures/misc/blizzard_overlay.png");
    private static final ResourceLocation HEATWAVE_TEX = ResourceLocation.fromNamespaceAndPath(HUDVisualsSubpack.MOD_ID, "textures/misc/heatwave_overlay.png");

    // Vanilla Powder Snow texture for the "Freezing/Boiling" Grade
    private static final ResourceLocation FROST_OVERLAY = ResourceLocation.withDefaultNamespace("textures/misc/powder_snow_outline.png");

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        // 1. Fetch Core Temp (Updated by BodyTempHandler/Capability)
        float coreTemp = mc.player.getPersistentData().getFloat("coreTemp");
        renderExposureGrades(graphics, coreTemp);

        // 2. Fetch Environmental Overlays (Driven by random Weather Intensity)
        float blizzardInt = ClientWeatherHandler.getIntensity();
        float heatwaveInt = ClientWeatherHandler.getIntensity();

        if (blizzardInt > 0.01f) {
            renderOverlay(graphics, BLIZZARD_TEX, blizzardInt, 1.0f, 1.0f, 1.0f);
        }

        if (heatwaveInt > 0.01f) {
            // Heatwaves get a slight orange tint to the texture
            renderOverlay(graphics, HEATWAVE_TEX, heatwaveInt * 0.7f, 1.0f, 0.8f, 0.4f);
        }
    }

    private static void renderExposureGrades(GuiGraphics graphics, float temp) {
        // Comfort Zone is around 0.7f (Humid)
        float coldDist = 0.7f - temp;
        float heatDist = temp - 0.7f;

        // --- COLD GRADES ---
        if (coldDist > 0.2f) {
            float alpha = Math.min(1.0f, (coldDist - 0.2f) * 1.5f);

            // Grade 1 & 2: Blue Rings (Vignette)
            renderOverlay(graphics, VIGNETTE, alpha, 0.4f, 0.7f, 1.0f);

            // Grade 3: Freezing (Vanilla Frost Overlay)
            if (coldDist > 0.7f) {
                float frostAlpha = Math.min(1.0f, (coldDist - 0.7f) * 2.0f);
                renderOverlay(graphics, FROST_OVERLAY, frostAlpha, 1.0f, 1.0f, 1.0f);
            }
        }
        // --- HEAT GRADES ---
        else if (heatDist > 0.5f) {
            float alpha = Math.min(1.0f, (heatDist - 0.5f) * 1.2f);

            // Grade 1 & 2: Orange/Red Rings
            renderOverlay(graphics, VIGNETTE, alpha, 1.0f, 0.4f, 0.0f);

            // Grade 3: Boiling (Hot version of Vanilla Frost)
            if (heatDist > 1.0f) {
                float heatFrostAlpha = Math.min(1.0f, (heatDist - 1.0f) * 2.0f);
                renderOverlay(graphics, FROST_OVERLAY, heatFrostAlpha, 1.0f, 0.2f, 0.0f);
            }
        }
    }

    private static void renderOverlay(GuiGraphics graphics, ResourceLocation texture, float alpha, float r, float g, float b) {
        if (alpha <= 0) return;

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Use the RGBA setter for tinting
        graphics.setColor(r, g, b, alpha);

        // Blit across the full screen
        graphics.blit(texture, 0, 0, -90, 0.0f, 0.0f, graphics.guiWidth(), graphics.guiHeight(), graphics.guiWidth(), graphics.guiHeight());

        // Reset
        graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }
}