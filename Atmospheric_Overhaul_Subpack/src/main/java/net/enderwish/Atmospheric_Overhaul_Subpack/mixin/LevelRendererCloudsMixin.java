package net.enderwish.Atmospheric_Overhaul_Subpack.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.enderwish.Atmospheric_Overhaul_Subpack.client.sky.GodRayRenderer;
import net.enderwish.Atmospheric_Overhaul_Subpack.client.sky.SunShadowMap;
import net.enderwish.Atmospheric_Overhaul_Subpack.client.sky.VolumetricShaftsRenderer;
import net.enderwish.Atmospheric_Overhaul_Subpack.client.sky.VoxelCloudEngine;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL30C;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

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
 * The same hook now drives the whole light pass, in this order:
 *
 *   1. the sun's shadow map: the scene drawn from the sun's point of
 *      view, which is what the volumetric shafts are marched against;
 *   2. the voxel clouds;
 *   3. the volumetric shafts: the ray march that makes true god rays;
 *   4. the screen-space god rays: the cheap wide glow around the sun.
 *
 * The order matters: the shafts need the shadow map, and both add
 * light to the same framebuffer.
 *
 * Lives in the EXISTING atmospheric.mixins.json (shared with the
 * weather/biome mixins) rather than a second config file.
 */
@Mixin(LevelRenderer.class)
public class LevelRendererCloudsMixin {

    private static final Logger LOGGER = LoggerFactory.getLogger("GHAtmospheric/CloudsMixin");
    private static boolean firedOnce;

    /**
     * The chunk layer renderer. Its view and projection matrices are
     * arguments, which is what lets the same compiled chunk buffers
     * be drawn from the sun's point of view.
     */
    @Shadow
    private void renderSectionLayer(RenderType renderType, double x, double y, double z,
                                    Matrix4f frustumMatrix, Matrix4f projectionMatrix) {}

    /**
     * The sections the main camera sees. They are trimmed to the
     * near ones while the sun map is drawn, so the pass does not pay
     * for chunks the map cannot even cover.
     *
     * The field is final, so it cannot be replaced -- but it is a
     * list, and the far sections are taken out and put back afterwards.
     */
    @Shadow
    @Final
    private ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections;

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

            Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();

            // 1. the sun's shadow map
            renderSunShadowMap(camera, partialTick);

            // 2. the clouds
            VoxelCloudEngine.render(frustumMatrix, projectionMatrix, camX, camY, camZ);

            // 3. the true volumetric shafts, marched against the map
            VolumetricShaftsRenderer.render(camera, frustumMatrix, projectionMatrix, partialTick);

            // 4. the cheap screen-space glow
            GodRayRenderer.render(frustumMatrix, projectionMatrix, camX, camY, camZ, partialTick);
        }
    }

    /**
     * Draws the scene from the sun's point of view into the shadow
     * map. The chunk renderer's render type binds the MAIN target
     * itself, so the mixin on renderSectionLayer rebinds the map right
     * after the render type sets its state up.
     */
    private void renderSunShadowMap(Camera camera, float partialTick) {
        int[] boundFbo = new int[1];
        GL30C.glGetIntegerv(GL30C.GL_DRAW_FRAMEBUFFER_BINDING, boundFbo);
        int previousFbo = boundFbo[0];

        if (!SunShadowMap.begin(camera, partialTick)) {
            return;   // night, or the map does not need re-rendering
        }
        try {
            double x = SunShadowMap.camX();
            double y = SunShadowMap.camY();
            double z = SunShadowMap.camZ();
            Matrix4f view = SunShadowMap.view();
            Matrix4f projection = SunShadowMap.projection();

            // only the sections the map can actually cover
            List<SectionRenderDispatcher.RenderSection> far = trimToNear(x, y, z);
            try {
                // the terrain and the leaves: the leaves are what make
                // the shafts through a canopy
                this.renderSectionLayer(RenderType.solid(), x, y, z, view, projection);
                this.renderSectionLayer(RenderType.cutoutMipped(), x, y, z, view, projection);
            } finally {
                this.visibleSections.addAll(far);
            }
        } finally {
            SunShadowMap.finish(previousFbo);
        }
    }

    /**
     * Takes the far sections out of the visible list and returns them,
     * so the caller can put them back. The map covers a fixed area
     * around the player, so drawing the whole render distance into it
     * would be pure waste.
     */
    private List<SectionRenderDispatcher.RenderSection> trimToNear(double x, double y, double z) {
        ObjectArrayList<SectionRenderDispatcher.RenderSection> all = this.visibleSections;
        List<SectionRenderDispatcher.RenderSection> far = new ArrayList<>();
        double range = SunShadowMap.RANGE;
        double rangeSq = range * range;
        for (int i = all.size() - 1; i >= 0; i--) {
            SectionRenderDispatcher.RenderSection section = all.get(i);
            BlockPos origin = section.getOrigin();
            double dx = origin.getX() + 8.0 - x;
            double dy = origin.getY() + 8.0 - y;
            double dz = origin.getZ() + 8.0 - z;
            if (dx * dx + dy * dy + dz * dz > rangeSq) {
                far.add(section);
                all.remove(i);
            }
        }
        return far;
    }
}
