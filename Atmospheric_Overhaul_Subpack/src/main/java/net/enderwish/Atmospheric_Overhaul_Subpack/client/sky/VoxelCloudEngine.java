package net.enderwish.Atmospheric_Overhaul_Subpack.client.sky;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;

/**
 * VoxelCloudEngine
 *
 * Facade for the tile-based voxel cloud system -- the renderClouds
 * mixin's entry point (vanilla clouds are cancelled at HEAD; this
 * replaces the old monolithic same-thread engine).
 *
 * The system behind it:
 * - CloudDensityField -- the 3D density function (pure, cloud-space);
 * - CloudMesher -- worker-thread tile meshes, soft per-vertex rims;
 * - CloudTile / CloudTileManager -- async tile lifecycle: one daemon
 *   meshing thread, budgeted render-thread uploads, eviction, drift,
 *   time-of-day tint through the gh_clouds shader's ColorModulator.
 *
 * Keeps the original entry signatures the mixin calls.
 */
public final class VoxelCloudEngine {

    private VoxelCloudEngine() {}

    public static boolean isEnabled() { return true; }

    /**
     * @param viewMatrix       camera-rotation view matrix vanilla passes as
     *                         renderClouds' frustumMatrix (rotation only --
     *                         per-tile ModelOffset is camera-relative)
     * @param projectionMatrix the actual projection matrix
     */
    public static void render(Matrix4f viewMatrix, Matrix4f projectionMatrix,
                              double camX, double camY, double camZ) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return;
        CloudTileManager.INSTANCE.render(level, viewMatrix, projectionMatrix, camX, camY, camZ);
    }
}
