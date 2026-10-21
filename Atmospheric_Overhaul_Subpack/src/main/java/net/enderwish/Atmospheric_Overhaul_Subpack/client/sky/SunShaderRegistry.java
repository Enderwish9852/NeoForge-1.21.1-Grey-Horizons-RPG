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
 * SunShaderRegistry
 *
 * NEW -- registers the sun's own shader:
 *
 * - gh_sun -- draws the sun in screen space: a round disc with a soft
 *   limb, limb darkening and a corona, sized from the projection
 *   matrix's own FOV so it stays a fixed angular size on screen.
 *   Uses the shared screen-space vertex shader gh_screen_quad.
 *
 * Lives at assets/gh_atmospheric/shaders/core/gh_sun.{json,fsh} plus
 * gh_screen_quad.vsh; same registration pattern as GodRayShaderRegistry.
 */
@EventBusSubscriber(modid = AtmosphericOverhaulSubpack.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SunShaderRegistry {

    private SunShaderRegistry() {}

    public static volatile ShaderInstance sunShader;

    /** The volumetric raymarch that makes the true god rays. */
    public static volatile ShaderInstance shadowShader;

    /** The sun's shadow on the surfaces. */
    public static volatile ShaderInstance groundShadowShader;

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(
                new ShaderInstance(
                        event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath(AtmosphericOverhaulSubpack.MOD_ID, "gh_sun"),
                        DefaultVertexFormat.POSITION_TEX),
                shader -> sunShader = shader);
        event.registerShader(
                new ShaderInstance(
                        event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath(AtmosphericOverhaulSubpack.MOD_ID, "gh_sun_shadow"),
                        DefaultVertexFormat.POSITION_TEX),
                shader -> shadowShader = shader);
        event.registerShader(
                new ShaderInstance(
                        event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath(AtmosphericOverhaulSubpack.MOD_ID, "gh_sun_ground_shadow"),
                        DefaultVertexFormat.POSITION_TEX),
                shader -> groundShadowShader = shader);
    }
}
