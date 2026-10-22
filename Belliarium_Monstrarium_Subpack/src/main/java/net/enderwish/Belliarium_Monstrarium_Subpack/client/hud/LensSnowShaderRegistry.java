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
 * LensSnowShaderRegistry
 *
 * NEW -- the shaders for the snow on the camera lens.
 *
 * - gh_lens_snow -- the flakes on the glass: they stick as the snow
 *   builds up and melt off as it lets up.
 * - the copy pass is a plain blit, used to put the frame back,
 *   because a fragment shader must never sample the buffer it writes
 *   to.
 */
@EventBusSubscriber(modid = BelliariumMonstrariumSubpack.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class LensSnowShaderRegistry {

    private LensSnowShaderRegistry() {}

    public static volatile ShaderInstance lensSnowShader;
    public static volatile ShaderInstance copyShader;

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(
                new ShaderInstance(
                        event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath(BelliariumMonstrariumSubpack.MODID, "gh_lens_snow"),
                        DefaultVertexFormat.POSITION_TEX),
                shader -> lensSnowShader = shader);
        event.registerShader(
                new ShaderInstance(
                        event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath(BelliariumMonstrariumSubpack.MODID, "gh_lens_copy"),
                        DefaultVertexFormat.POSITION_TEX),
                shader -> copyShader = shader);
    }
}
