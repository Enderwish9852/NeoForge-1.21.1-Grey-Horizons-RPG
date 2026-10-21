package net.enderwish.Belliarium_Monstrarium_Subpack.client.hud;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

import java.io.IOException;

/**
 * LensRainShaderRegistry
 *
 * NEW -- the shaders for the water on the camera lens.
 *
 * - gh_lens_rain -- draws the lens effect: the rain running down it,
 *   and the sheet of water that comes off when the player surfaces.
 *   The world is sampled through the drops, so the world bends behind
 *   each one.
 * - gh_lens_quad -- the fullscreen vertex shader it shares with the
 *   other post passes.
 * - the copy pass, a plain blit, is used to put the distorted frame
 *   back, because a fragment shader must never sample the buffer it
 *   writes to.
 */
@EventBusSubscriber(modid = BelliariumMonstrariumSubpack.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class LensRainShaderRegistry {

    private LensRainShaderRegistry() {}

    public static volatile ShaderInstance lensRainShader;
    public static volatile ShaderInstance copyShader;

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(
                new ShaderInstance(
                        event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath(BelliariumMonstrariumSubpack.MODID, "gh_lens_rain"),
                        DefaultVertexFormat.POSITION_TEX),
                shader -> lensRainShader = shader);
        event.registerShader(
                new ShaderInstance(
                        event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath(BelliariumMonstrariumSubpack.MODID, "gh_lens_copy"),
                        DefaultVertexFormat.POSITION_TEX),
                shader -> copyShader = shader);
    }
}
