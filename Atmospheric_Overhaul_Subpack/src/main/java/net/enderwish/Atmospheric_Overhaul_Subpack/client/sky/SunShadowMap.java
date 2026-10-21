package net.enderwish.Atmospheric_Overhaul_Subpack.client.sky;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL30C;

/**
 * SunShadowMap
 *
 * NEW -- a depth map of the scene seen from the SUN's point of view.
 *
 * This is what makes true volumetric god rays possible. The screen
 * space approach (GodRayRenderer) smears the sun's glow across the
 * screen, which is a 2D trick: it can only ever work where the sun is
 * in frame, and what it draws is a smear rather than real light.
 * A shadow map lets a ray march ask the real question at every point
 * along a view ray -- "is this bit of air lit, or is the sun blocked
 * here?" -- and the answer comes from the actual scene, so the shafts
 * are correct from any angle, the sun can be behind you, and any
 * occluder (leaves, a roof, a cliff) casts a real shadow.
 *
 * The map is an ORTHOGRAPHIC depth map, centred on the player and
 * looking along the sun's direction:
 *
 *   - orthographic, because a shadow map must not distort: the sun is
 *     at infinity, so parallel rays are the correct model, and a
 *     perspective shadow map would make the shadows drift as the
 *     player moves;
 *   - it covers EXTENT blocks either side of the player, which is
 *     where the shafts are actually visible;
 *   - the depth range runs from the player towards the sun, so the
 *     geometry below the player (which cannot shadow it) is clipped.
 *
 * The geometry is drawn by the ordinary chunk renderer: the section
 * renderer's renderSectionLayer takes the view and projection
 * matrices as arguments, so the very same compiled chunk buffers can
 * be drawn from the sun's point of view. The render type binds the
 * main target itself, so LevelRendererSunShadowMixin rebinds this
 * map right after the render type sets its state up.
 */
public final class SunShadowMap {

    private SunShadowMap() {}

    /** Resolution of the map. 1024 over 128 blocks is 8 texels a block. */
    public static final int SIZE = 1024;
    /** Half the width of the area the map covers, in blocks. */
    private static final float EXTENT = 64.0f;
    /**
     * How far from the player the shadow map is still worth drawing.
     * The map covers EXTENT blocks either side, so anything much
     * further cannot cast a shadow into it.
     */
    public static final double RANGE = 96.0;
    /**
     * Depth range along the sun axis, in blocks. The sun camera sits
     * ON the player, so the near plane has to reach behind it: with a
     * near plane at zero the ground the player stands on would be
     * clipped out of the map, and the light would leak straight
     * through it.
     */
    private static final float NEAR = -60.0f;
    private static final float FAR = 340.0f;

    private static TextureTarget target;
    private static boolean passActive;

    // the sun camera, handed to the chunk renderer and to the raymarch
    private static final Matrix4f view = new Matrix4f();
    private static final Matrix4f projection = new Matrix4f();
    private static final Matrix4f viewProjection = new Matrix4f();
    /**
     * The matrix the map was LAST rendered with. The raymarch has to
     * use this one, not the freshly computed one, or the shadows would
     * be projected with a matrix that does not match the map whenever
     * the map is not re-rendered this frame.
     */
    private static final Matrix4f renderedViewProjection = new Matrix4f();

    private static double camX, camY, camZ;
    private static boolean valid;
    private static int frameCounter;
    private static float lastElevation = -99.0f;
    /**
     * The viewport that was bound before the pass. The map is 1024
     * square, and binding it sets the viewport to that; the cloud
     * renderer never sets a viewport of its own, so without putting
     * this back the clouds were drawn into a 1024x1024 window and came
     * out squashed into the corner.
     */
    private static final int[] savedViewport = new int[4];

    /** How often the map is re-rendered. The sun moves slowly, so every
     *  other frame is plenty and halves the cost of the pass. */
    private static final int UPDATE_EVERY = 2;

    /** True while the sun pass is running (the mixin rebinds the map). */
    public static boolean isPassActive() {
        return passActive;
    }

    /** Binds the map as the render target, with its own viewport. */
    public static void bindTarget() {
        if (target != null) target.bindWrite(true);
    }

    /** The world -> sun clip matrix, for the raymarch pass. */
    public static Matrix4f viewProjection() {
        return renderedViewProjection;
    }

    /** The sun camera's position, for the chunk renderer's chunk offset. */
    public static double camX() { return camX; }
    public static double camY() { return camY; }
    public static double camZ() { return camZ; }

    /** The view matrix (rotation + translation) for the chunk renderer. */
    public static Matrix4f view() { return view; }
    /** The orthographic projection for the chunk renderer. */
    public static Matrix4f projection() { return projection; }

    /** The depth texture of the map, for the raymarch pass to sample. */
    public static int depthTextureId() {
        return target != null ? target.getDepthTextureId() : 0;
    }

    /**
     * Prepares the sun camera and binds the map. The caller then draws
     * the chunk layers with the matrices from view() and projection(),
     * and must call finish() afterwards.
     *
     * @return false when there is no sun to speak of (night), in which
     *         case nothing should be drawn
     */
    public static boolean begin(Camera camera, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) return false;

        Vector3f sun = SunRenderer.worldDirection(level.getTimeOfDay(partialTick));
        if (sun.y <= 0.02f) {
            valid = false;      // the sun is down: no shafts
            return false;
        }

        // The map is re-rendered every other frame: the sun moves about
        // three degrees a second, so the shadows barely change, and the
        // pass is a second draw of the near chunks. It is forced when
        // the sun is moving quickly (sunrise and sunset), where the
        // shadow direction changes fast.
        frameCounter++;
        float elevation = sun.y;
        boolean sunMovedFast = Math.abs(elevation - lastElevation) > 0.004f;
        if (valid && frameCounter % UPDATE_EVERY != 0 && !sunMovedFast) {
            return false;      // keep the map from the last update
        }
        lastElevation = elevation;

        ensureTarget();

        Vec3 player = camera.getPosition();
        camX = player.x;
        camY = player.y;
        camZ = player.z;

        // The chunk renderer hands the shader geometry RELATIVE to the
        // camera position (the CHUNK_OFFSET uniform is blockPos minus
        // the camera), and the main camera's view matrix is a pure
        // rotation. The sun camera must work the same way, so its view
        // matrix is a pure rotation too, and the camera position
        // passed in is the player's.
        Vector3f up = Math.abs(sun.y) > 0.99f ? new Vector3f(0.0f, 0.0f, 1.0f) : new Vector3f(0.0f, 1.0f, 0.0f);
        Matrix4f rotation = new Matrix4f().lookAt(new Vector3f(0.0f, 0.0f, 0.0f), sun, up);
        view.set(rotation);

        projection.setOrtho(-EXTENT, EXTENT, -EXTENT, EXTENT, NEAR, FAR);
        // the raymarch works in WORLD space, so its matrix needs the
        // player's offset as well
        viewProjection.set(projection)
                .mul(rotation)
                .mul(new Matrix4f().translate(
                        (float) -player.x, (float) -player.y, (float) -player.z));

        target.bindWrite(true);
        // the viewport has to be put back afterwards: the cloud
        // renderer does not set one of its own
        GL11C.glGetIntegerv(GL11C.GL_VIEWPORT, savedViewport);
        // save the global clear colour: the weather code sets it for
        // blizzard and heatwave skies and expects it to survive
        float[] clear = new float[4];
        GL11C.glGetFloatv(GL11C.GL_COLOR_CLEAR_VALUE, clear);
        RenderSystem.clearColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.clear(GL30C.GL_COLOR_BUFFER_BIT | GL30C.GL_DEPTH_BUFFER_BIT, false);
        RenderSystem.clearColor(clear[0], clear[1], clear[2], clear[3]);

        renderedViewProjection.set(viewProjection);
        passActive = true;
        valid = true;
        return true;
    }

    /** Ends the sun pass and restores the framebuffer that was bound. */
    public static void finish(int previousFbo) {
        passActive = false;
        GlStateManager._glBindFramebuffer(GL30C.GL_FRAMEBUFFER, previousFbo);
        GlStateManager._viewport(savedViewport[0], savedViewport[1], savedViewport[2], savedViewport[3]);
    }

    /** True when the last begin() found a sun above the horizon. */
    public static boolean isValid() {
        return valid;
    }

    private static void ensureTarget() {
        if (target == null) {
            target = new TextureTarget(SIZE, SIZE, true, false);
        }
    }
}
