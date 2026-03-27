package net.enderwish.HUD_Visuals_Subpack.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.enderwish.HUD_Visuals_Subpack.client.ClientWeatherHandler;
import net.enderwish.HUD_Visuals_Subpack.core.WeatherType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/**
 * Modern NeoForge renderer for custom weather overlays.
 * Hooks into the level rendering pipeline to apply environmental visuals.
 */
@EventBusSubscriber(modid = "hud_visuals_subpack", value = Dist.CLIENT)
public class WeatherEffectsRenderer {

    private static final ResourceLocation BLIZZARD_TEXTURE = ResourceLocation.fromNamespaceAndPath("hud_visuals_subpack", "textures/environment/blizzard.png");
    private static final ResourceLocation HEATWAVE_TEXTURE = ResourceLocation.fromNamespaceAndPath("hud_visuals_subpack", "textures/environment/heatwave.png");

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        // We only care about the weather stage to overlay our custom effects
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) {
            return;
        }

        renderCustomWeather(event.getPoseStack(), event.getPartialTick());
    }

    private static void renderCustomWeather(PoseStack poseStack, DeltaTracker partialTick) {
        // FIXED: Using getCurrentType() and getVisualAlpha() to match your ClientWeatherHandler
        WeatherType currentType = ClientWeatherHandler.getCurrentType();
        float alpha = ClientWeatherHandler.getVisualAlpha();

        // Guard against zero alpha or null worlds
        if (alpha <= 0.0f || Minecraft.getInstance().level == null) {
            return;
        }

        // Only handle weather types actually defined in your mod
        switch (currentType) {
            case BLIZZARD -> renderOverlay(poseStack, BLIZZARD_TEXTURE, alpha, 0.95f, 0.95f, 1.0f);
            case HEATWAVE -> renderOverlay(poseStack, HEATWAVE_TEXTURE, alpha, 1.0f, 0.9f, 0.7f);
            default -> { /* Standard MC weather or Clear */ }
        }
    }

    /**
     * Renders a screen-space overlay or world-aligned plane.
     */
    private static void renderOverlay(PoseStack poseStack, ResourceLocation texture, float alpha, float r, float g, float b) {
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);

        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderColor(r, g, b, alpha);

        if (texture != null) {
            RenderSystem.setShaderTexture(0, texture);
        }

        Tesselator tesselator = Tesselator.getInstance();
        // 1.21.1 requires mode and format in the begin() call
        BufferBuilder bufferBuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        Matrix4f matrix = poseStack.last().pose();

        // Large quad to cover the view for atmospheric effects
        float size = 150.0f;
        bufferBuilder.addVertex(matrix, -size, -size, 0).setUv(0, 0).setColor(r, g, b, alpha);
        bufferBuilder.addVertex(matrix, -size, size, 0).setUv(0, 1).setColor(r, g, b, alpha);
        bufferBuilder.addVertex(matrix, size, size, 0).setUv(1, 1).setColor(r, g, b, alpha);
        bufferBuilder.addVertex(matrix, size, -size, 0).setUv(1, 0).setColor(r, g, b, alpha);

        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());

        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
    }
}