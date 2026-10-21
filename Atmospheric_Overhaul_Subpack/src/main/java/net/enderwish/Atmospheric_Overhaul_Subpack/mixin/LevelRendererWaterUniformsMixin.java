package net.enderwish.Atmospheric_Overhaul_Subpack.mixin;

import net.enderwish.Atmospheric_Overhaul_Subpack.client.sky.WetGroundPass;
import net.enderwish.Atmospheric_Overhaul_Subpack.client.water.UnderwaterShaftsRenderer;
import net.enderwish.Atmospheric_Overhaul_Subpack.client.water.UnderwaterWobbleRenderer;
import net.enderwish.Atmospheric_Overhaul_Subpack.client.water.WaterReflectionRenderer;
import net.enderwish.Atmospheric_Overhaul_Subpack.client.water.WaterShaderUniforms;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.material.FogType;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * LevelRendererWaterUniformsMixin
 *
 * NEW -- pushes the gh_atmospheric water uniforms (atlas rects, wind,
 * camera, sun) into the overridden rendertype_translucent core shader
 * at the very top of the frame, before any chunk layer draws. Firing
 * at renderLevel HEAD is unconditional: unlike the clouds hook it
 * runs every frame no matter what graphics settings are on.
 *
 * renderLevel signature (LevelRenderer.java:921):
 * (DeltaTracker, boolean, Camera, GameRenderer, LightTexture,
 *  Matrix4f frustumMatrix, Matrix4f projectionMatrix)
 */
@Mixin(LevelRenderer.class)
public class LevelRendererWaterUniformsMixin {

    /**
     * Re-renders one chunk section layer into whatever render target is
     * currently bound (LevelRenderer.java:1280) -- the same call vanilla
     * itself uses for the Fabulous translucent re-render. Shadowed for
     * WaterReflectionRenderer's water-mask pass, which re-renders the
     * translucent layer into a scratch target in MASK MODE.
     */
    @Shadow
    private void renderSectionLayer(RenderType renderType, double x, double y, double z,
                                    Matrix4f frustumMatrix, Matrix4f projectionMatrix) {}

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void ghWaterUniforms(DeltaTracker deltaTracker, boolean renderBlockOutline,
                                  Camera camera, GameRenderer gameRenderer,
                                  LightTexture lightTexture, Matrix4f frustumMatrix,
                                  Matrix4f projectionMatrix, CallbackInfo ci) {
        WaterShaderUniforms.push(camera, deltaTracker.getGameTimeDeltaPartialTick(false));
    }

    /**
     * TAIL: after the whole world (terrain, water, entities, particles,
     * weather) is drawn and before the GUI. Exactly one of the two water
     * post systems runs: camera IN water -> the wobble pass (everything
     * sways); camera above water -> the reflections round (water mask +
     * ray-marched SSR + refraction composite + copy back).
     */
    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void ghWaterPost(DeltaTracker deltaTracker, boolean renderBlockOutline,
                             Camera camera, GameRenderer gameRenderer,
                             LightTexture lightTexture, Matrix4f frustumMatrix,
                             Matrix4f projectionMatrix, CallbackInfo ci) {
        if (camera.getFluidInCamera() == FogType.WATER) {
            UnderwaterWobbleRenderer.render(camera);
            // after the wobble: the shafts are light IN the water, not a
            // view through the surface, so the surface's distortion
            // must not bend them
            UnderwaterShaftsRenderer.render(camera, frustumMatrix, projectionMatrix,
                    deltaTracker.getGameTimeDeltaPartialTick(false));
        } else {
            var pos = camera.getPosition();
            WaterReflectionRenderer.render(camera, frustumMatrix, projectionMatrix,
                    () -> this.renderSectionLayer(RenderType.translucent(),
                            pos.x, pos.y, pos.z, frustumMatrix, projectionMatrix));
        }
        // the puddles on the ground. Last of all, so the reflection can
        // show the trees and the entities, which are long since drawn.
        WetGroundPass.render(camera, frustumMatrix, projectionMatrix,
                deltaTracker.getGameTimeDeltaPartialTick(false));
    }
}
