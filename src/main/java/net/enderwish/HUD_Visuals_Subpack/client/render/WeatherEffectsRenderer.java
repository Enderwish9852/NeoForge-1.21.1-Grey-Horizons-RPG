package net.enderwish.HUD_Visuals_Subpack.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.enderwish.HUD_Visuals_Subpack.HUDVisualsSubpack;
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

/**
 * Optimized NeoForge 1.21.1 Weather Renderer.
 * Fixed for merged WeatherTypes and modern Vertex Building.
 */
@EventBusSubscriber(modid = HUDVisualsSubpack.MOD_ID, value = Dist.CLIENT)
public class WeatherEffectsRenderer {

    private static final ResourceLocation BLIZZARD_TEX = ResourceLocation.fromNamespaceAndPath(HUDVisualsSubpack.MOD_ID, "textures/environment/blizzard.png");
    private static final ResourceLocation HEATWAVE_TEX = ResourceLocation.fromNamespaceAndPath(HUDVisualsSubpack.MOD_ID, "textures/environment/heatwave.png");
    private static final ResourceLocation POLLEN_TEX = ResourceLocation.fromNamespaceAndPath(HUDVisualsSubpack.MOD_ID, "textures/environment/pollen_haze.png");
    private static final ResourceLocation DIAMOND_TEX = ResourceLocation.fromNamespaceAndPath(HUDVisualsSubpack.MOD_ID, "textures/environment/diamond_dust.png");
    private static final ResourceLocation WIND_TEX = ResourceLocation.fromNamespaceAndPath(HUDVisualsSubpack.MOD_ID, "textures/environment/wind_streaks.png");

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        // We render at AFTER_WEATHER to overlay on top of vanilla rain/snow
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        WeatherType type = ClientWeatherHandler.getType();
        float intensity = ClientWeatherHandler.getIntensity();

        if (intensity <= 0.0f) return;

        // Use the PoseStack from the event
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();

        // Face the player so the overlay covers the view
        renderCustomWeather(poseStack, type, intensity);

        poseStack.popPose();
    }

    private static void renderCustomWeather(PoseStack poseStack, WeatherType type, float intensity) {
        switch (type) {
            case BLIZZARD -> renderOverlay(poseStack, BLIZZARD_TEX, intensity, 0.9f, 0.95f, 1.0f);
            case HEATWAVE -> renderOverlay(poseStack, HEATWAVE_TEX, intensity * 0.5f, 1.0f, 0.7f, 0.3f);
            case POLLEN_HAZE -> renderOverlay(poseStack, POLLEN_TEX, intensity * 0.4f, 0.8f, 1.0f, 0.2f);
            case DIAMOND_DUST -> renderOverlay(poseStack, DIAMOND_TEX, intensity * 0.6f, 1.0f, 1.0f, 1.0f);
            case WIND -> renderOverlay(poseStack, WIND_TEX, intensity * 0.3f, 1.0f, 1.0f, 1.0f);
            case RAIN -> {
                // If rain is very intense, add a grey "mist" overlay
                if (intensity > 0.7f) renderOverlay(poseStack, null, (intensity - 0.7f), 0.5f, 0.5f, 0.6f);
            }
        }
    }

    private static void renderOverlay(PoseStack poseStack, ResourceLocation texture, float alpha, float r, float g, float b) {
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);

        if (texture != null) {
            RenderSystem.setShaderTexture(0, texture);
        }

        Tesselator tesselator = Tesselator.getInstance();
        // 1.21.1: Vertex format uses POSITION_TEX_COLOR
        BufferBuilder builder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        Matrix4f matrix = poseStack.last().pose();
        float size = 100.0f; // Large enough to cover view distance planes
        float z = 0.01f;     // Slight offset to prevent z-fighting

        // Build vertices: Position -> UV -> Color
        builder.addVertex(matrix, -size, -size, z).setUv(0, 0).setColor(r, g, b, alpha);
        builder.addVertex(matrix, -size, size, z).setUv(0, 1).setColor(r, g, b, alpha);
        builder.addVertex(matrix, size, size, z).setUv(1, 1).setColor(r, g, b, alpha);
        builder.addVertex(matrix, size, -size, z).setUv(1, 0).setColor(r, g, b, alpha);

        MeshData meshData = builder.build();
        if (meshData != null) {
            BufferUploader.drawWithShader(meshData);
        }

        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
    }
}