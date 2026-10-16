package net.enderwish.Atmospheric_Overhaul_Subpack.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.enderwish.Atmospheric_Overhaul_Subpack.client.sky.GodRayRenderer;
import net.enderwish.Atmospheric_Overhaul_Subpack.client.sky.VoxelCloudEngine;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * LevelRendererCloudsMixin
 *
 * NEW -- replaces vanilla's flat cloud plane with the VoxelCloudEngine
 * when the engine is enabled. renderClouds is public in 1.21.1
 * (LevelRenderer.java:1723), injected at HEAD and cancelled.
 *
 * Vanilla's parameter order is (PoseStack, frustumMatrix,
 * projectionMatrix, ...): frustumMatrix is the camera-rotation VIEW
 * matrix (vanilla mulPose's it and draws with projectionMatrix as the
 * actual projection). VoxelCloudEngine.render expects the same order.
 *
 * Lives in the EXISTING atmospheric.mixins.json (shared with the
 * weather/biome mixins) rather than a second config file.
 */
@Mixin(LevelRenderer.class)
public class LevelRendererCloudsMixin {

    private static final Logger LOGGER = LoggerFactory.getLogger("GHAtmospheric/CloudsMixin");
    private static boolean firedOnce;

    @Inject(method = "renderClouds(Lcom/mojang/blaze3d/vertex/PoseStack;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;FDDD)V",
            at = @At("HEAD"), cancellable = true)
    private void gh_atmospheric$renderVoxelClouds(PoseStack poseStack, Matrix4f frustumMatrix,
                                                  Matrix4f projectionMatrix, float partialTick,
                                                  double camX, double camY, double camZ,
                                                  CallbackInfo ci) {
        if (VoxelCloudEngine.isEnabled()) {
            if (!firedOnce) {
                firedOnce = true;
                LOGGER.info("renderClouds mixin fired -- cancelling vanilla, dispatching VoxelCloudEngine");
            }
            ci.cancel();
            VoxelCloudEngine.render(frustumMatrix, projectionMatrix, camX, camY, camZ);
            GodRayRenderer.render(frustumMatrix, projectionMatrix, camX, camY, camZ, partialTick);
        }
    }
}
