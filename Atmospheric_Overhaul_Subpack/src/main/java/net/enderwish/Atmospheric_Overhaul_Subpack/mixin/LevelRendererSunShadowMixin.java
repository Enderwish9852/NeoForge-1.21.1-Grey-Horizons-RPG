package net.enderwish.Atmospheric_Overhaul_Subpack.mixin;

import net.enderwish.Atmospheric_Overhaul_Subpack.client.sky.SunShadowMap;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * LevelRendererSunShadowMixin
 *
 * NEW -- keeps the sun's shadow map bound while the chunk layers are
 * drawn into it.
 *
 * The chunk render type (RenderType.solid and friends) has the main
 * render target as its OUTPUT STATE, so as soon as the render type
 * sets itself up it binds the main target and the geometry would go
 * there instead of into the sun's map.
 *
 * The fix is to rebind the map straight after the render type has set
 * its state up: the state (blend, depth, cull) is what the render type
 * sets, and the framebuffer is a separate thing that we take over.
 * The render type's own clearRenderState at the end of the method puts
 * the main target back, and the caller restores the framebuffer it
 * had before the pass.
 */
@Mixin(LevelRenderer.class)
public class LevelRendererSunShadowMixin {

    @Inject(
            method = "renderSectionLayer(Lnet/minecraft/client/renderer/RenderType;DDDLorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/RenderType;setupRenderState()V",
                    shift = At.Shift.AFTER))
    private void gh_atmospheric$bindSunShadowMap(RenderType renderType, double x, double y, double z,
                                                 Matrix4f frustumMatrix, Matrix4f projectionMatrix,
                                                 CallbackInfo ci) {
        if (SunShadowMap.isPassActive()) {
            SunShadowMap.bindTarget();
        }
    }
}
