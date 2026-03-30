package net.enderwish.HUD_Visuals_Subpack.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.enderwish.HUD_Visuals_Subpack.client.ClientWeatherHandler;
import net.enderwish.HUD_Visuals_Subpack.core.WeatherType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import static net.enderwish.HUD_Visuals_Subpack.client.ClientWeatherHandler.getIntensity;

/**
 * Modern NeoForge renderer for custom weather overlays.
 * Fixed for 1.21.1: Corrected event method calls and vertex builder sequence.
 */
@EventBusSubscriber(modid = "hud_visuals_subpack", value = Dist.CLIENT)
public class WeatherEffectsRenderer {

    private static final ResourceLocation BLIZZARD_TEXTURE = ResourceLocation.fromNamespaceAndPath("hud_visuals_subpack", "textures/environment/blizzard.png");
    private static final ResourceLocation HEATWAVE_TEXTURE = ResourceLocation.fromNamespaceAndPath("hud_visuals_subpack", "textures/environment/heatwave.png");
    private static final ResourceLocation POLLEN_TEXTURE = ResourceLocation.fromNamespaceAndPath("hud_visuals_subpack", "textures/environment/pollen_haze.png");
    private static final ResourceLocation DIAMOND_TEXTURE = ResourceLocation.fromNamespaceAndPath("hud_visuals_subpack", "textures/environment/diamond_dust.png");

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        // Only render during the weather stage
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) {
            return;
        }

        // NeoForge 1.21.1: Pass the PoseStack from the event
        renderCustomWeather(event.getPoseStack());
    }

    private static void renderCustomWeather(PoseStack poseStack) {
        // Fetch current state from ClientWeatherHandler [cite: 6]
        WeatherType currentType = ClientWeatherHandler.getCurrentType();
        float alpha = ClientWeatherHandler.getVisualAlpha();

        if (alpha <= 0.0f || Minecraft.getInstance().level == null) {
            return;
        }

        // Apply rendering based on the mod's specific weather types [cite: 6, 17]
        switch (currentType) {
            case BLIZZARD -> {
                renderOverlay(poseStack, BLIZZARD_TEXTURE, alpha * getIntensity(), 1.0f, 1.0f, 1.0f);
            }
            case HEATWAVE -> {
                renderOverlay(poseStack, HEATWAVE_TEXTURE, alpha * 0.7f, 1.0f, 0.6f, 0.2f);
            }
            case POLLEN_HAZE -> {
                // Soft yellow/green tint with the pollen particle texture
                renderOverlay(poseStack, POLLEN_TEXTURE, alpha * 0.5f, 0.9f, 1.0f, 0.4f);
            }
            case DIAMOND_DUST -> {
                // Bright, sparkling blue-white tint
                renderOverlay(poseStack, DIAMOND_TEXTURE, alpha * 0.8f, 0.7f, 0.9f, 1.0f);
            }
            case STRONG_WIND -> {
                // For strong wind, we can use a very subtle gray sweep or just the alpha transition
                renderOverlay(poseStack, null, alpha * 0.2f, 0.9f, 0.9f, 0.9f);
            }
        }
    }

    private static void renderOverlay(PoseStack poseStack, ResourceLocation texture, float alpha, float r, float g, float b) {
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);

        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f); // Use 1.0 if colors are passed per-vertex

        RenderSystem.setShaderTexture(0, texture);

        Tesselator tesselator = Tesselator.getInstance();
        // 1.21.1 Vertex building: Position -> Tex -> Color
        BufferBuilder bufferBuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        Matrix4f matrix = poseStack.last().pose();
        float size = 150.0f;

        // Correct sequence for POSITION_TEX_COLOR
        bufferBuilder.addVertex(matrix, -size, -size, 0).setUv(0, 0).setColor(r, g, b, alpha);
        bufferBuilder.addVertex(matrix, -size, size, 0).setUv(0, 1).setColor(r, g, b, alpha);
        bufferBuilder.addVertex(matrix, size, size, 0).setUv(1, 1).setColor(r, g, b, alpha);
        bufferBuilder.addVertex(matrix, size, -size, 0).setUv(1, 0).setColor(r, g, b, alpha);

        MeshData meshData = bufferBuilder.build();
        if (meshData != null) {
            BufferUploader.drawWithShader(meshData);
        }

        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
    }
}