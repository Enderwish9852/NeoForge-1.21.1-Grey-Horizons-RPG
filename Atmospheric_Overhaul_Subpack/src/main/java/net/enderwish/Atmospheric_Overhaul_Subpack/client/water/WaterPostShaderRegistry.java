package net.enderwish.Atmospheric_Overhaul_Subpack.client.water;

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
 * WaterPostShaderRegistry
 *
 * NEW -- registers the core shader of the underwater-wobble post pass
 * (POSITION_TEX fullscreen quad, samples the scene copy through
 * "DiffuseSampler", same registration pattern as GodRayShaderRegistry):
 *
 * - gh_water_wobble -- screen-space refraction wobble applied when the
 *   camera is in water: the whole already-rendered frame is resampled
 *   through a UV distortion driven by the SAME wave-phase clock the
 *   water surface renders from (WaterShaderUniforms), so the wobble
 *   and the surface waves can never disagree. The shared fullscreen
 *   vertex shader gh_rays_full is reused (no new .vsh).
 *
 * Lives at assets/gh_atmospheric/shaders/core/gh_water_wobble.{json,fsh}.
 */
@EventBusSubscriber(modid = AtmosphericOverhaulSubpack.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class WaterPostShaderRegistry {

    private WaterPostShaderRegistry() {}

    public static volatile ShaderInstance wobbleShader;

    /** Sunlight coming down through the surface, seen from under water. */
    public static volatile ShaderInstance shaftsShader;

    /** Ray-marched reflection pass (half-res) + the composite pass (full-res). */
    public static volatile ShaderInstance ssrShader;
    public static volatile ShaderInstance compositeShader;

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(
                new ShaderInstance(
                        event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath(AtmosphericOverhaulSubpack.MOD_ID, "gh_water_wobble"),
                        DefaultVertexFormat.POSITION_TEX),
                shader -> wobbleShader = shader);
        event.registerShader(
                new ShaderInstance(
                        event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath(AtmosphericOverhaulSubpack.MOD_ID, "gh_water_ssr"),
                        DefaultVertexFormat.POSITION_TEX),
                shader -> ssrShader = shader);
        event.registerShader(
                new ShaderInstance(
                        event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath(AtmosphericOverhaulSubpack.MOD_ID, "gh_water_composite"),
                        DefaultVertexFormat.POSITION_TEX),
                shader -> compositeShader = shader);
        event.registerShader(
                new ShaderInstance(
                        event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath(AtmosphericOverhaulSubpack.MOD_ID, "gh_water_shafts"),
                        DefaultVertexFormat.POSITION_TEX),
                shader -> shaftsShader = shader);
    }
}
