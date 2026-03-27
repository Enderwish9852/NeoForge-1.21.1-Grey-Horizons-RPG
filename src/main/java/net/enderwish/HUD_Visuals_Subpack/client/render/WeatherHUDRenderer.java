package net.enderwish.HUD_Visuals_Subpack.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.enderwish.HUD_Visuals_Subpack.HUDVisualsSubpack;
import net.enderwish.HUD_Visuals_Subpack.client.ClientWeatherHandler;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class WeatherHUDRenderer {
    // Texture paths for weather overlays
    private static final ResourceLocation BLIZZARD_TEXTURE = ResourceLocation.fromNamespaceAndPath(HUDVisualsSubpack.MOD_ID, "textures/misc/blizzard_overlay.png");
    private static final ResourceLocation HEATWAVE_TEXTURE = ResourceLocation.fromNamespaceAndPath(HUDVisualsSubpack.MOD_ID, "textures/misc/heatwave_overlay.png");

    /**
     * Updated for NeoForge 1.21.1: Now uses DeltaTracker instead of float partialTicks
     */
    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        // Fetch current intensities from the client-side handler
        float blizzard = ClientWeatherHandler.getBlizzardIntensity();
        float heatwave = ClientWeatherHandler.getHeatwaveIntensity();

        if (blizzard > 0) {
            renderOverlay(graphics, BLIZZARD_TEXTURE, blizzard);
        }

        if (heatwave > 0) {
            renderOverlay(graphics, HEATWAVE_TEXTURE, heatwave);
        }
    }

    private static void renderOverlay(GuiGraphics graphics, ResourceLocation texture, float alpha) {
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();

        // Prepare rendering state for transparent overlay
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Set the transparency based on weather intensity (alpha)
        graphics.setColor(1.0F, 1.0F, 1.0F, alpha);

        // Draw the texture across the full screen
        graphics.blit(texture, 0, 0, -90, 0.0F, 0.0F, width, height, width, height);

        // Reset rendering state to avoid affecting other HUD elements
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }
}