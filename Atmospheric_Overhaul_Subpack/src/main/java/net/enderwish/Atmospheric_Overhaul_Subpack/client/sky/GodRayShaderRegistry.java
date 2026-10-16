package net.enderwish.Atmospheric_Overhaul_Subpack.client.sky;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.enderwish.Atmospheric_Overhaul_Subpack.AtmosphericOverhaulSubpack;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

import java.io.IOException;

/**
 * GodRayShaderRegistry
 *
 * NEW -- registers the three core shaders of the god-ray post chain
 * (all draw the same fullscreen POSITION_TEX quad, all sample the
 * previous stage through a "DiffuseSampler" sampler set from Java):
 *
 * - gh_ray_occlusion -- stamps the sun disc into a half-res target,
 *   masked to pixels whose main-target depth is exactly sky, so
 *   terrain silhouettes cut the rays;
 * - gh_ray_blur -- the radial-accumulation pass that smears the disc
 *   toward the sun's screen position (GPU Gems 3 style); run twice
 *   chained (short sharp reach, then full-length soft reach);
 * - gh_ray_composite -- additively blends the accumulated light onto
 *   the scene with the per-frame tint and intensity.
 *
 * Lives at assets/gh_atmospheric/shaders/core/gh_ray_*.{json,fsh} plus
 * the shared gh_rays_full.vsh; same registration pattern as
 * CloudShaderRegistry.
 */
@EventBusSubscriber(modid = AtmosphericOverhaulSubpack.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GodRayShaderRegistry {

    private GodRayShaderRegistry() {}

    public static volatile ShaderInstance occlusionShader;
    public static volatile ShaderInstance blurShader;
    public static volatile ShaderInstance compositeShader;

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(
                new ShaderInstance(
                        event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath(AtmosphericOverhaulSubpack.MOD_ID, "gh_ray_occlusion"),
                        DefaultVertexFormat.POSITION_TEX),
                shader -> occlusionShader = shader);
        event.registerShader(
                new ShaderInstance(
                        event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath(AtmosphericOverhaulSubpack.MOD_ID, "gh_ray_blur"),
                        DefaultVertexFormat.POSITION_TEX),
                shader -> blurShader = shader);
        event.registerShader(
                new ShaderInstance(
                        event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath(AtmosphericOverhaulSubpack.MOD_ID, "gh_ray_composite"),
                        DefaultVertexFormat.POSITION_TEX),
                shader -> compositeShader = shader);
    }
}
