package net.enderwish.Belliarium_Monstrarium_Subpack.mixin;

import net.enderwish.Belliarium_Monstrarium_Subpack.client.hud.LensSnowRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * LevelRendererLensSnowMixin
 *
 * NEW -- draws the snow on the camera lens.
 *
 * A screen space pass, so it has to happen after the world is drawn
 * and before the GUI: renderLevel's TAIL is exactly that.
 *
 * The rain on the lens and the snow on the lens are never both on
 * the glass at once, so the order of the two passes does not matter.
 */
@Mixin(LevelRenderer.class)
public class LevelRendererLensSnowMixin {

    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void gh_belliarium$lensSnow(DeltaTracker deltaTracker, boolean renderBlockOutline,
                                       Camera camera, GameRenderer gameRenderer,
                                       LightTexture lightTexture,
                                       Matrix4f frustumMatrix, Matrix4f projectionMatrix,
                                       CallbackInfo ci) {
        LensSnowRenderer.render(camera, deltaTracker.getGameTimeDeltaPartialTick(false));
    }
}
