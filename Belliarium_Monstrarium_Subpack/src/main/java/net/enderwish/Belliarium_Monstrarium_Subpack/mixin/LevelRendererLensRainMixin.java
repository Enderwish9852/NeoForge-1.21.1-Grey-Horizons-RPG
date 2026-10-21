package net.enderwish.Belliarium_Monstrarium_Subpack.mixin;

import net.enderwish.Belliarium_Monstrarium_Subpack.client.hud.LensRainRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * LevelRendererLensRainMixin
 *
 * NEW -- draws the water on the camera lens.
 *
 * The effect is a screen space pass, so it has to happen after the
 * world is drawn and before the GUI: renderLevel's TAIL is exactly
 * that. It goes last, because the lens is in front of everything else.
 *
 * The lens effect lives here rather than in the atmospheric subpack
 * because it is driven by the player's body wetness, which is the
 * survival system's own number.
 */
@Mixin(LevelRenderer.class)
public class LevelRendererLensRainMixin {

    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void gh_belliarium$lensRain(DeltaTracker deltaTracker, boolean renderBlockOutline,
                                        Camera camera, net.minecraft.client.renderer.GameRenderer gameRenderer,
                                        net.minecraft.client.renderer.LightTexture lightTexture,
                                        org.joml.Matrix4f frustumMatrix, org.joml.Matrix4f projectionMatrix,
                                        CallbackInfo ci) {
        LensRainRenderer.render(camera, deltaTracker.getGameTimeDeltaPartialTick(false));
    }
}
