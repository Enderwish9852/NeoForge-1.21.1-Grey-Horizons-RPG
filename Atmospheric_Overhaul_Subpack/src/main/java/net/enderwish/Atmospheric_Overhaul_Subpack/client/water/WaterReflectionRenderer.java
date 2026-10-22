package net.enderwish.Atmospheric_Overhaul_Subpack.client.water;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
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
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL30C;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WaterReflectionRenderer
 *
 * NEW -- ray-marched screen-space reflections on water (round 2 of the
 * water system), plus the above-water refraction (the bed wobbling
 * through the surface) that was promised with the wobble round.
 *
 * Pipeline, at renderLevel TAIL (above water only; the wobble pass owns
 * the camera-in-water state):
 *
 *   0. MASK -- the translucent section layer is re-rendered into a
 *      full-res scratch target with the overridden translucent shader
 *      switched to MASK MODE (WaterShaderUniforms.setMaskMode): water
 *      faces (with the wave displacement) draw flat white and write
 *      their depth; glass/ice/portals discard. Result: color = which
 *      pixels are water, depth = the visual water surface position.
 *
 *   1. SSR -- gh_water_ssr, HALF-res: for each water pixel, reconstruct
 *      the surface from the mask depth, build the wave normal from the
 *      same wave field the surface renders from, reflect the view ray
 *      and march it against the MAIN depth buffer (terrain, buildings,
 *      entities all cut the reflection); sky-depth samples reflect the
 *      sky + our voxel clouds + god rays; 4-step binary refinement on
 *      hits.
 *
 *   2. COMPOSITE -- gh_water_composite, FULL-res into a scratch copy
 *      (never read+write one framebuffer): non-water pixels pass
 *      through; water pixels get the refraction (bed resampled through
 *      the wave tilt) and the fresnel-weighted reflection.
 *
 *   3. COPY -- the composited scratch is copied back onto the frame-
 *      buffer that was bound at TAIL, via the wobble shader's plain
 *      copy path (WobbleParams.x = 0) -- no second copy shader.
 *
 * Works in plain Fast/Fancy AND Fabulous: at TAIL the main target holds
 * the final composited scene either way. Glass/ice/portal pixels are
 * never touched (mask = water only). Costs one translucent re-render
 * (the mask) + one half-res pass + two fullscreen passes.
 *
 * Disable with -Dgh.water=false (the whole water system's switch).
 */
public final class WaterReflectionRenderer {

    private static final Logger LOGGER = LoggerFactory.getLogger("GHAtmospheric/WaterReflections");

    private WaterReflectionRenderer() {}

    // ── Tuning ──────────────────────────────────────────────────────────────

    /** Refraction: pixel shift of the bed at full wave-normal tilt. */
    private static final float REFRACT_PX = 6.0f;
    /** Reflection strength mixed in at full fresnel (grazing mirror). */
    private static final float REFLECT_STRENGTH = 0.85f;

    private static final Matrix4f IDENTITY = new Matrix4f();

    // ── GPU state (lazily created, render thread only) ────────────────────

    private static VertexBuffer quad;
    private static TextureTarget maskTarget;    // full-res, color + depth
    private static TextureTarget ssrTarget;     // half-res, color only
    private static TextureTarget compTarget;     // full-res, color only
    private static boolean loggedFirstRender;

    /**
     * True while the mask pass (the translucent layer re-rendered into
     * the mask target) is running.
     *
     * The translucent render type's output state binds the GAME's
     * translucent target when shader transparency is on (Fabulous), which
     * would steal the mask pass away from our own target. The mixin on
     * Minecraft.useShaderTransparency() reads this flag and reports "no
     * shader transparency" for the duration, so the render type leaves
     * the already bound mask target alone.
     */
    private static boolean maskPassActive;

    /** True while the mask pass is running (read by the Minecraft mixin). */
    public static boolean isMaskPassActive() {
        return maskPassActive;
    }

    /** Marks the start of the mask pass. */
    public static void beginMaskPass() {
        maskPassActive = true;
    }

    /** Marks the end of the mask pass. */
    public static void endMaskPass() {
        maskPassActive = false;
    }

    /**
     * Entry from LevelRendererWaterUniformsMixin (renderLevel TAIL).
     *
     * @param translucentRenderer re-renders the translucent section layer
     *                            (the mixin's @Shadow of renderSectionLayer)
     */
    public static void render(Camera camera, Matrix4f viewMatrix, Matrix4f projectionMatrix,
                               Runnable translucentRenderer) {
        if (!WaterShaderUniforms.ENABLED) return;
        if (camera.getFluidInCamera() == FogType.WATER) return;   // wobble owns that
        ShaderInstance ssr = WaterPostShaderRegistry.ssrShader;
        ShaderInstance comp = WaterPostShaderRegistry.compositeShader;
        ShaderInstance wobble = WaterPostShaderRegistry.wobbleShader;   // copy-back path
        if (ssr == null || comp == null || wobble == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        RenderTarget main = mc.getMainRenderTarget();
        ensureTargets(main.width, main.height);
        ensureQuad();

        Matrix4f invProj = new Matrix4f(projectionMatrix).invert();
        Matrix4f invView = new Matrix4f(viewMatrix).invert();
        Vec3 cam = camera.getPosition();

        int[] boundFbo = new int[1];
        GL30C.glGetIntegerv(GL30C.GL_DRAW_FRAMEBUFFER_BINDING, boundFbo);
        int prevFbo = boundFbo[0];

        // ── 0. mask: translucent layer re-rendered in MASK MODE ───────────
        prepareMaskTarget(main);
        beginMaskPass();
        WaterShaderUniforms.setMaskMode(true);
        try {
            translucentRenderer.run();
        } finally {
            WaterShaderUniforms.setMaskMode(false);
            endMaskPass();
        }

        // ── post passes share state: no blend, no depth, no cull ───────────
        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        // ── 1. SSR, half-res ───────────────────────────────────────────────
        ssrTarget.bindWrite(true);
        ssr.setSampler("DiffuseSampler", main.getColorTextureId());
        ssr.setSampler("MainDepth", main.getDepthTextureId());
        ssr.setSampler("MaskSampler", maskTarget.getColorTextureId());
        ssr.setSampler("MaskDepth", maskTarget.getDepthTextureId());
        uniformMat(ssr, "RayProjMat", projectionMatrix);
        uniformMat(ssr, "RayViewMat", viewMatrix);
        uniformMat(ssr, "RayInvProjMat", invProj);
        uniformMat(ssr, "RayInvViewMat", invView);
        uniform3(ssr, "CameraPos", (float) cam.x, (float) cam.y, (float) cam.z);
        uniform4v(ssr, "WavePhase", WaterShaderUniforms.getWavePhases());
        uniform2(ssr, "WindVec",
                WaterShaderUniforms.getSmoothWindDx(), WaterShaderUniforms.getSmoothWindDz());
        uniform4(ssr, "WaterParams", 1.0f, 0.0f, 0.0f, 0.0f);
        drawQuad(ssr);

        // ── 2. composite, full-res into the scratch copy ──────────────────
        compTarget.bindWrite(true);
        comp.setSampler("DiffuseSampler", main.getColorTextureId());
        comp.setSampler("SSRSampler", ssrTarget.getColorTextureId());
        comp.setSampler("MaskSampler", maskTarget.getColorTextureId());
        comp.setSampler("MaskDepth", maskTarget.getDepthTextureId());
        uniformMat(comp, "RayInvProjMat", invProj);
        uniformMat(comp, "RayInvViewMat", invView);
        uniform3(comp, "CameraPos", (float) cam.x, (float) cam.y, (float) cam.z);
        uniform4v(comp, "WavePhase", WaterShaderUniforms.getWavePhases());
        uniform2(comp, "WindVec",
                WaterShaderUniforms.getSmoothWindDx(), WaterShaderUniforms.getSmoothWindDz());
        uniform4(comp, "WaterParams", 1.0f, 0.0f, 0.0f, 0.0f);
        uniform4(comp, "RefractParams", REFRACT_PX, REFLECT_STRENGTH, 0.0f, 0.0f);
        uniform2(comp, "ScreenSize", main.width, main.height);
        drawQuad(comp);

        // ── 3. copy the composited frame back ──────────────────────────────
        GlStateManager._glBindFramebuffer(GL30C.GL_FRAMEBUFFER, prevFbo);
        GlStateManager._viewport(0, 0, main.width, main.height);
        wobble.setSampler("DiffuseSampler", compTarget.getColorTextureId());
        uniform4(wobble, "WobbleParams", 0.0f, 0.0f, 0.0f, 0.0f);
        uniform2(wobble, "ScreenSize", main.width, main.height);
        drawQuad(wobble);

        // restore the render state vanilla expects after renderLevel
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();

        if (!loggedFirstRender) {
            loggedFirstRender = true;
            LOGGER.info("first water reflections: {}x{}", main.width, main.height);
        }
    }

    // ── GPU helpers (same pattern as GodRayRenderer) ──────────────────────

    private static void drawQuad(ShaderInstance shader) {
        quad.bind();   // drawWithShader never binds the VAO itself
        quad.drawWithShader(IDENTITY, IDENTITY, shader);
        VertexBuffer.unbind();
    }

    private static void ensureTargets(int width, int height) {
        int hw = Math.max(1, width / 2);
        int hh = Math.max(1, height / 2);
        // The resize path calls RenderTarget.clear(), which uses the
        // target's own clear colour AND the global one -> keep both black
        // and restore the global colour afterwards.
        withBlackClearColor(() -> {
            if (maskTarget == null) {
                maskTarget = new TextureTarget(width, height, true, false);   // color + depth
                // BLACK, not the RenderTarget default (white): the mask
                // sampler reads red < 0.5 as "not water".
                maskTarget.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
                // LINEAR on the mask's colour: the SSR pass runs at half
                // resolution and samples this full-resolution mask, so
                // with NEAREST the edge of the water was a jagged,
                // aliased line across the puddle.
                GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, maskTarget.getColorTextureId());
                GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MIN_FILTER, GL11C.GL_LINEAR);
                GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MAG_FILTER, GL11C.GL_LINEAR);
                GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, 0);
                ssrTarget = new TextureTarget(hw, hh, false, false);
                compTarget = new TextureTarget(width, height, false, false);
            } else if (maskTarget.width != width || maskTarget.height != height) {
                maskTarget.resize(width, height, true);
                ssrTarget.resize(hw, hh, false);
                compTarget.resize(width, height, false);
            }
        });
    }

    /**
     * Prepares the mask target for the mask pass.
     *
     * Two things have to be true before the translucent layer is
     * re-rendered into it:
     *
     *  1. The colour buffer must start BLACK. The mask pass only
     *     writes the water faces (every other translucent block
     *     discards), so a leftover pixel from the previous frame would
     *     still read "water" and the reflection would be smeared over
     *     the land. A RenderTarget clears to WHITE by default
     *     (clearChannels = {1,1,1,0}) and bindWrite() does NOT clear
     *     at all -- so without this the mask was white everywhere and
     *     every pixel of the screen was treated as water.
     *  2. The depth buffer must hold the SCENE's depth, so the
     *     translucent layer is depth-tested against the world: water
     *     hidden behind a hill is rejected and never enters the mask.
     *
     * The clear colour is global GL state that the level renderer also
     * uses (FogRendererMixin sets it for blizzard/heatwave skies), so
     * it is saved and restored around the clear.
     */
    private static void prepareMaskTarget(RenderTarget main) {
        // Where the visible scene's depth lives. In Fabulous the
        // transparency chain cleared the main target's depth, so the
        // scene depth is in the game's translucent target (it holds the
        // opaque depth plus the translucent layer's own depth -- the
        // same "visible surface" depth the non-Fabulous path keeps in
        // the main target).
        RenderTarget depthSource = main;
        if (Minecraft.useShaderTransparency()) {
            RenderTarget translucent = Minecraft.getInstance().levelRenderer.getTranslucentTarget();
            if (translucent != null) {
                depthSource = translucent;
            }
        }
        final RenderTarget sceneDepth = depthSource;

        withBlackClearColor(() -> {
            // colour -> black, depth -> 1.0
            RenderSystem.depthMask(true);
            maskTarget.clear(false);
            // the scene's depth, so the mask pass is occlusion-correct
            maskTarget.copyDepthFrom(sceneDepth);
            // copyDepthFrom() leaves framebuffer 0 bound -> rebind the
            // mask target (and its viewport) for the mask pass
            maskTarget.bindWrite(true);
        });
    }

    /**
     * Runs the action with the global GL clear colour set to black,
     * then restores whatever it was.
     *
     * The clear colour is global state: FogRendererMixin sets it to the
     * blizzard/heatwave sky colour, and the level renderer clears the
     * world with it. Leaving it black would break the weather skies.
     */
    private static void withBlackClearColor(Runnable action) {
        float[] saved = new float[4];
        GL11C.glGetFloatv(GL11C.GL_COLOR_CLEAR_VALUE, saved);
        try {
            GlStateManager._clearColor(0.0F, 0.0F, 0.0F, 0.0F);
            action.run();
        } finally {
            GlStateManager._clearColor(saved[0], saved[1], saved[2], saved[3]);
        }
    }

    private static void ensureQuad() {
        if (quad != null) return;
        BufferBuilder buf = Tesselator.getInstance()
                .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buf.addVertex(-1.0f, -1.0f, 0.999f).setUv(0.0f, 0.0f);
        buf.addVertex(1.0f, -1.0f, 0.999f).setUv(1.0f, 0.0f);
        buf.addVertex(1.0f, 1.0f, 0.999f).setUv(1.0f, 1.0f);
        buf.addVertex(-1.0f, 1.0f, 0.999f).setUv(0.0f, 1.0f);
        MeshData mesh = buf.buildOrThrow();
        quad = new VertexBuffer(VertexBuffer.Usage.STATIC);
        quad.bind();
        quad.upload(mesh);
        VertexBuffer.unbind();
    }

    private static void uniform2(ShaderInstance shader, String name, float a, float b) {
        Uniform u = shader.getUniform(name);
        if (u != null) u.set(a, b);
    }

    private static void uniform3(ShaderInstance shader, String name, float a, float b, float c) {
        Uniform u = shader.getUniform(name);
        if (u != null) u.set(a, b, c);
    }

    private static void uniform4(ShaderInstance shader, String name, float a, float b, float c, float d) {
        Uniform u = shader.getUniform(name);
        if (u != null) u.set(a, b, c, d);
    }

    private static void uniform4v(ShaderInstance shader, String name, float[] v) {
        Uniform u = shader.getUniform(name);
        if (u != null && v.length >= 4) u.set(v[0], v[1], v[2], v[3]);
    }

    private static void uniformMat(ShaderInstance shader, String name, Matrix4f m) {
        Uniform u = shader.getUniform(name);
        if (u != null) u.set(m);
    }
}
