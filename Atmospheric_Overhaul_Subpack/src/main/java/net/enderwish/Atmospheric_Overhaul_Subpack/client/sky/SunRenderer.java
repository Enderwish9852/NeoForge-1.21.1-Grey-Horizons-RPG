package net.enderwish.Atmospheric_Overhaul_Subpack.client.sky;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import com.mojang.math.Axis;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * SunRenderer
 *
 * NEW -- the sun as a real sun instead of vanilla's hard white square.
 *
 * Vanilla draws the sun as a 60x60 world quad at distance 100 textured
 * with a 14px white square inside a 32px texture, so it is a SQUARE
 * roughly 15 degrees across. Worse for us: the god-ray occlusion pass
 * used to stamp a glare disc reaching 45% of the screen height on top
 * of it, which is what produced the "90% of the screen flash bang".
 *
 * This renderer draws the sun in SCREEN SPACE (NDC), which is the only
 * way to keep it a perfect circle and a fixed angular size at any FOV,
 * aspect ratio or window size:
 *
 *   - the disc's angular radius is a constant (DISC_ANGLE), converted
 *     to a screen radius through the projection matrix's own m11
 *     (= 1/tan(fovy/2)), so the sun never changes size when the FOV
 *     slider moves;
 *   - the shader draws the disc with a soft limb, limb darkening
 *     (the edge of a real sun is dimmer and redder) and a corona;
 *   - the corona reaches CORONA_ANGLE, a few times the disc, which is
 *     what the god-ray pass uses as its light source, so the shafts
 *     start exactly at the sun's limb.
 *
 * Drawn at the TAIL of LevelRenderer.renderSky, i.e. after the sky, the
 * sunrise glow, the moon and the stars, but BEFORE the terrain: the sun
 * is drawn with the depth test off and the depth write off, so the
 * chunks that come afterwards simply cover it (correct occlusion, no
 * depth interaction needed).
 *
 * The vanilla square is suppressed by LevelRendererSunMixin, which
 * redirects the sun's texture to a fully transparent 1x1.
 */
public final class SunRenderer {

    private SunRenderer() {}

    // ── Tuning ─────────────────────────────────────────────────────────────

    /**
     * Angular RADIUS of the sun's disc, in radians. 0.030 rad = 1.72
     * degrees, so the disc is 3.4 degrees across. The real sun is 0.53
     * degrees, which at a 70 degree FOV is 8 pixels -- far too small to
     * read as a sun in a blocky world, so this is the "stylised but
     * real" size most games use.
     */
    public static final float DISC_ANGLE = 0.030f;

    /** Angular radius of the corona, radians (0.15 rad = 8.6 degrees). */
    public static final float CORONA_ANGLE = 0.150f;

    private static final Matrix4f IDENTITY = new Matrix4f();

    private static VertexBuffer quad;

    // ── Render ─────────────────────────────────────────────────────────────

    /**
     * Draws the sun. Called at the TAIL of LevelRenderer.renderSky.
     *
     * @param frustumMatrix  the camera's view matrix
     * @param projectionMatrix the projection matrix actually used
     */
    public static void render(Matrix4f frustumMatrix, Matrix4f projectionMatrix,
                              float partialTick, Camera camera, boolean isFoggy) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if (isFoggy) return;

        // mirror vanilla's own sky guards
        FogType fluid = camera.getFluidInCamera();
        if (fluid == FogType.POWDER_SNOW || fluid == FogType.LAVA) return;
        if (mc.level.effects().skyType() == DimensionSpecialEffects.SkyType.END) return;
        if (camera.getEntity() instanceof LivingEntity living
                && (living.hasEffect(MobEffects.BLINDNESS) || living.hasEffect(MobEffects.DARKNESS))) return;

        ShaderInstance shader = SunShaderRegistry.sunShader;
        if (shader == null) return;

        float dayT = mc.level.getTimeOfDay(partialTick);
        Vector3f sunDir = worldDirection(dayT);
        float elevation = sunDir.y;
        if (elevation < -0.30f) return;   // far under the horizon

        float[] screen = new float[3];
        screenPosition(frustumMatrix, projectionMatrix, dayT, screen);
        if (screen[2] < 0.5f) return;     // the sun is behind the camera
        float sunU = screen[0];
        float sunV = screen[1];

        // screen radius of the corona, in units of the screen HEIGHT
        float coronaUv = angularRadiusToUv(CORONA_ANGLE, projectionMatrix);
        var main = mc.getMainRenderTarget();
        float aspect = (float) main.width / main.height;

        // NDC half-extents of a pixel-isotropic circle of radius r (in
        // screen-height units): x spans W pixels over 2 NDC units, y
        // spans H pixels over 2 NDC units
        float halfX = 2.0f * coronaUv / aspect;
        float halfY = 2.0f * coronaUv;

        // strength: dimmed by rain, faded out at the horizon
        float rain = mc.level.getRainLevel(partialTick);
        float bright = (1.0f - 0.85f * rain)
                * Mth.clamp((elevation + 0.15f) / 0.25f, 0.0f, 1.0f);
        // tint: deep orange at the horizon, white high up
        float high = Mth.clamp(elevation / 0.35f, 0.0f, 1.0f);
        float tintR = 1.0f;
        float tintG = Mth.lerp(high, 0.74f, 0.97f);
        float tintB = Mth.lerp(high, 0.40f, 0.92f);

        ensureQuad();

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
        RenderSystem.disableCull();

        uniform2(shader, "QuadCenter", sunU * 2.0f - 1.0f, sunV * 2.0f - 1.0f);
        uniform2(shader, "QuadHalfSize", halfX, halfY);
        uniform1(shader, "DiscRatio", (float) (Math.tan(DISC_ANGLE) / Math.tan(CORONA_ANGLE)));
        uniform3(shader, "SunColor", tintR, tintG, tintB);
        uniform1(shader, "SunBrightness", bright);

        quad.bind();
        quad.drawWithShader(IDENTITY, IDENTITY, shader);
        VertexBuffer.unbind();

        // leave the state the way vanilla's sky left it
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableCull();
    }

    // ── Shared sun math (the god-ray pass uses this too) ─────────────────

    /** The sun's direction in WORLD space (no camera rotation). */
    public static Vector3f worldDirection(float dayT) {
        return new Matrix4f()
                .rotate(Axis.YP.rotationDegrees(-90.0f))
                .rotate(Axis.XP.rotationDegrees(dayT * 360.0f))
                .transformDirection(new Vector3f(0.0f, 1.0f, 0.0f));
    }

    /**
     * The sun's position on screen.
     *
     * @param out out[0] = u (0..1, left to right), out[1] = v (0..1, BOTTOM
     *             to top -- the same space the shaders' texCoord lives in),
     *             out[2] = 1 when the sun is in front of the camera, 0 when
     *             it is behind it
     */
    public static void screenPosition(Matrix4f viewMatrix, Matrix4f projectionMatrix,
                                      float dayT, float[] out) {
        Matrix4f sunViewMat = new Matrix4f(viewMatrix)
                .rotate(Axis.YP.rotationDegrees(-90.0f))
                .rotate(Axis.XP.rotationDegrees(dayT * 360.0f));
        Vector3f sunView = sunViewMat.transformDirection(new Vector3f(0.0f, 1.0f, 0.0f));
        Vector4f clip = new Vector4f(sunView.x * 100.0f, sunView.y * 100.0f, sunView.z * 100.0f, 1.0f);
        projectionMatrix.transform(clip);
        if (clip.w <= 1.0e-4f) {
            // behind the camera: the projection mirrors, so there is no
            // meaningful screen position
            out[0] = 0.0f;
            out[1] = 0.0f;
            out[2] = 0.0f;
            return;
        }
        out[0] = clip.x / clip.w * 0.5f + 0.5f;
        out[1] = clip.y / clip.w * 0.5f + 0.5f;
        out[2] = 1.0f;
    }

    /**
     * Where the god-ray shafts converge.
     *
     * When the sun is in front of the camera that is the sun itself.
     * When the sun is BEHIND the camera it is the ANTI-SOLAR point --
     * the direction opposite the sun -- because that is where the
     * counter-glow and the radiating shafts live. Real crepuscular
     * rays are visible in every direction, and the old code simply
     * gave up when the sun went behind the player.
     *
     * @param out out[0] = u, out[1] = v, out[2] = 1 when the sun itself
     *             is in front, 0 when it is behind
     */
    public static void convergencePoint(Matrix4f viewMatrix, Matrix4f projectionMatrix,
                                        float dayT, float[] out) {
        Matrix4f sunViewMat = new Matrix4f(viewMatrix)
                .rotate(Axis.YP.rotationDegrees(-90.0f))
                .rotate(Axis.XP.rotationDegrees(dayT * 360.0f));
        Vector3f sunView = sunViewMat.transformDirection(new Vector3f(0.0f, 1.0f, 0.0f));

        // MC's view space looks down -Z, so z < 0 means "in front"
        boolean inFront = sunView.z < 0.0f;
        Vector3f use = inFront ? sunView : new Vector3f(-sunView.x, -sunView.y, -sunView.z);

        Vector4f clip = new Vector4f(use.x * 100.0f, use.y * 100.0f, use.z * 100.0f, 1.0f);
        projectionMatrix.transform(clip);
        if (clip.w <= 1.0e-4f) {
            // the sun is exactly to the side: the shafts run parallel to
            // the view, so send the convergence point far off in that
            // direction instead of failing
            out[0] = 0.5f + (use.x >= 0.0f ? 4.0f : -4.0f);
            out[1] = 0.5f;
            out[2] = inFront ? 1.0f : 0.0f;
            return;
        }
        out[0] = clip.x / clip.w * 0.5f + 0.5f;
        out[1] = clip.y / clip.w * 0.5f + 0.5f;
        out[2] = inFront ? 1.0f : 0.0f;
    }

    /**
     * Converts an ANGULAR radius into a screen radius expressed in
     * units of the screen height.
     *
     * The projection is Matrix4f.perspective(fovy, aspect, ...), so
     * m11 == 1/tan(fovy/2) and the vertical half-angle is tan(fovy/2).
     * Reading it straight off the matrix means the sun's size always
     * matches the FOV actually in use, with no second source of truth.
     */
    public static float angularRadiusToUv(float angleRad, Matrix4f projectionMatrix) {
        double tanHalfFov = 1.0 / Math.max(1.0e-4, projectionMatrix.m11());
        return (float) (0.5 * Math.tan(angleRad) / tanHalfFov);
    }

    // ── GPU helpers ───────────────────────────────────────────────────────

    private static void ensureQuad() {
        if (quad != null) return;
        BufferBuilder buf = Tesselator.getInstance()
                .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buf.addVertex(-1.0f, -1.0f, 0.0f).setUv(0.0f, 0.0f);
        buf.addVertex(1.0f, -1.0f, 0.0f).setUv(1.0f, 0.0f);
        buf.addVertex(1.0f, 1.0f, 0.0f).setUv(1.0f, 1.0f);
        buf.addVertex(-1.0f, 1.0f, 0.0f).setUv(0.0f, 1.0f);
        MeshData mesh = buf.buildOrThrow();
        quad = new VertexBuffer(VertexBuffer.Usage.STATIC);
        quad.bind();
        quad.upload(mesh);
        VertexBuffer.unbind();
    }

    private static void uniform1(ShaderInstance shader, String name, float v) {
        Uniform u = shader.getUniform(name);
        if (u != null) u.set(v);
    }

    private static void uniform2(ShaderInstance shader, String name, float a, float b) {
        Uniform u = shader.getUniform(name);
        if (u != null) u.set(a, b);
    }

    private static void uniform3(ShaderInstance shader, String name, float a, float b, float c) {
        Uniform u = shader.getUniform(name);
        if (u != null) u.set(a, b, c);
    }
}
