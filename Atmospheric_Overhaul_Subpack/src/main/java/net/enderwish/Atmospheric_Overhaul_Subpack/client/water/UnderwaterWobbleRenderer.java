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
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL30C;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * UnderwaterWobbleRenderer
 *
 * NEW -- screen-space refraction when the camera is in water: the
 * already-rendered frame wobbles like real water makes underwater
 * things wobble (blocks, entities, the bed all look off-shaped,
 * traveling with the waves).
 *
 * Runs at renderLevel TAIL (LevelRendererWaterUniformsMixin), after the
 * whole world is drawn, before the GUI. Two fullscreen passes, because
 * a fragment shader must never sample the framebuffer it writes to:
 *
 *   pass 1: main -> scratch target, resampled through the wave-driven
 *           UV distortion (gh_water_wobble);
 *   pass 2: scratch -> back onto the framebuffer that was bound at
 *           TAIL, with WobbleParams.x = 0 (the same shader's plain-copy
 *           path) -- in plain/Fancy rendering that framebuffer IS the
 *           main target, so the wobble lands in the final frame.
 *
 * The distortion is driven by the SAME wave-phase clock and wind the
 * water surface renders from (WaterShaderUniforms' continuous phase
 * accumulation and wind EMA), so the wobble always agrees with the
 * surface: same drift, same storms, no vanilla GameTime day-wrap snap.
 * Strength scales with wind (calm ~1.7 px, gale ~4.6 px at 1080p).
 *
 * Above water the whole pass is skipped (one enum check, zero cost).
 * Over-the-surface refraction -- distorting only the pixels BEHIND the
 * water plane while looking down into it -- needs a water mask and
 * arrives with the planned reflections round, which needs the same
 * mask machinery.
 *
 * Disable with -Dgh.water=false (same switch as the whole water system).
 */
public final class UnderwaterWobbleRenderer {

    private static final Logger LOGGER = LoggerFactory.getLogger("GHAtmospheric/WaterWobble");

    private UnderwaterWobbleRenderer() {}

    // ── Tuning ──────────────────────────────────────────────────────────────

    /** Wobble strength in pixels: calm-water base. */
    private static final float STRENGTH_CALM = 3.0f;
    /** Added at full wind (gale): calm 3.0 px -> gale ~7.6 px. */
    private static final float STRENGTH_WIND_EXTRA = 4.6f;

    private static final Matrix4f IDENTITY = new Matrix4f();

    // ── GPU state (lazily created, render thread only) ────────────────────

    private static VertexBuffer quad;
    private static TextureTarget scratch;
    private static boolean loggedFirstRender;

    /** Called at renderLevel TAIL from LevelRendererWaterUniformsMixin. */
    public static void render(Camera camera) {
        if (!WaterShaderUniforms.ENABLED) return;
        if (camera.getFluidInCamera() != FogType.WATER) return;
        ShaderInstance wobble = WaterPostShaderRegistry.wobbleShader;
        if (wobble == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        RenderTarget main = mc.getMainRenderTarget();
        ensureScratch(main.width, main.height);
        ensureQuad();

        // strength follows the smoothed wind, like the surface choppiness
        float windLen = Math.min((float) Math.sqrt(
                WaterShaderUniforms.getSmoothWindDx() * WaterShaderUniforms.getSmoothWindDx()
                        + WaterShaderUniforms.getSmoothWindDz() * WaterShaderUniforms.getSmoothWindDz()), 1.0f);
        float strength = STRENGTH_CALM + STRENGTH_WIND_EXTRA * windLen;

        int[] boundFbo = new int[1];
        GL30C.glGetIntegerv(GL30C.GL_DRAW_FRAMEBUFFER_BINDING, boundFbo);
        int prevFbo = boundFbo[0];

        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        // pass 1: main -> scratch, through the wave-driven distortion
        scratch.bindWrite(true);
        wobble.setSampler("DiffuseSampler", main.getColorTextureId());
        uniform4v(wobble, "WavePhase", WaterShaderUniforms.getWavePhases());
        uniform2(wobble, "WindVec",
                WaterShaderUniforms.getSmoothWindDx(), WaterShaderUniforms.getSmoothWindDz());
        uniform4(wobble, "WobbleParams", strength, 0.0f, 0.0f, 0.0f);
        uniform2(wobble, "ScreenSize", main.width, main.height);
        drawQuad(wobble);

        // pass 2: scratch -> the TAIL-bound framebuffer, plain copy
        GlStateManager._glBindFramebuffer(GL30C.GL_FRAMEBUFFER, prevFbo);
        GlStateManager._viewport(0, 0, main.width, main.height);
        wobble.setSampler("DiffuseSampler", scratch.getColorTextureId());
        uniform4(wobble, "WobbleParams", 0.0f, 0.0f, 0.0f, 0.0f);
        drawQuad(wobble);

        // restore the render state vanilla expects after renderLevel
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();

        if (!loggedFirstRender) {
            loggedFirstRender = true;
            LOGGER.info("first underwater wobble: {}x{}, wind={}, strength={}",
                    main.width, main.height,
                    String.format("%.2f", windLen), String.format("%.1f", strength));
        }
    }

    // ── GPU helpers (same pattern as GodRayRenderer) ──────────────────────

    private static void drawQuad(ShaderInstance shader) {
        quad.bind();   // drawWithShader never binds the VAO itself
        quad.drawWithShader(IDENTITY, IDENTITY, shader);
        VertexBuffer.unbind();
    }

    private static void ensureScratch(int width, int height) {
        if (scratch == null) {
            scratch = new TextureTarget(width, height, false, false);
        } else if (scratch.width != width || scratch.height != height) {
            scratch.resize(width, height, false);
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

    private static void uniform4(ShaderInstance shader, String name, float a, float b, float c, float d) {
        Uniform u = shader.getUniform(name);
        if (u != null) u.set(a, b, c, d);
    }

    private static void uniform4v(ShaderInstance shader, String name, float[] v) {
        Uniform u = shader.getUniform(name);
        if (u != null && v.length >= 4) u.set(v[0], v[1], v[2], v[3]);
    }
}
