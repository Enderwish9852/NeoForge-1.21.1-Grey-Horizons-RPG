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
 * CloudShaderRegistry
 *
 * NEW -- registers gh_clouds, the core shader the voxel cloud field
 * draws with. Own shader instead of vanilla position_color because the
 * per-tile world placement lives in a ModelOffset uniform (vec3): tile
 * meshes are built in tile-local coordinates by the worker thread and
 * placed per frame, camera-relative, without touching any matrix. The
 * vertex shader derives fog distance from that camera-relative
 * position, and ColorModulator carries the per-frame time-of-day tint
 * (auto-filled from RenderSystem state, like every shader uniform
 * declared here).
 *
 * Lives at assets/gh_atmospheric/shaders/core/gh_clouds.{json,vsh,fsh};
 * ShaderInstance resolves the json at <ns>:shaders/core/<name>.json and
 * parses "vertex"/"fragment" as full ResourceLocations (verified in the
 * 1.21.1 sources), so the descriptor names gh_atmospheric:gh_clouds.
 */
@EventBusSubscriber(modid = AtmosphericOverhaulSubpack.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CloudShaderRegistry {

    private CloudShaderRegistry() {}

    /** The cloud shader; null until RegisterShadersEvent has run (or after a failed reload). */
    public static volatile ShaderInstance cloudShader;

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(
                new ShaderInstance(
                        event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath(AtmosphericOverhaulSubpack.MOD_ID, "gh_clouds"),
                        DefaultVertexFormat.POSITION_COLOR),
                shader -> cloudShader = shader);
    }
}
