package net.enderwish.Belliarium_Monstrarium_Subpack.client.hud;

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
import net.enderwish.Atmospheric_Overhaul_Subpack.client.ClientSeasonState;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FogType;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL30C;

/**
 * LensSnowRenderer
 *
 * NEW -- snow on the camera lens, the companion to the rain on the
 * lens.
 *
 * While it is snowing, flakes land on the glass and sit there. When
 * the snow lets up they melt off, each one at its own pace, so the
 * lens clears unevenly rather than all at once.
 *
 * It is a screen space pass, so it happens after the world is drawn
 * and before the GUI.
 *
 * The lens belongs to the camera, so it only draws in first person.
 */
public final class LensSnowRenderer {

    private LensSnowRenderer() {}

    /** How quickly the snow gathers on the glass. */
    private static final float BUILD_RATE = 0.10f;
    /** How quickly it melts off again. */
    private static final float MELT_RATE = 0.07f;
    /** How much of the lens the snow can cover. */
    private static final float MAX_SNOW = 0.85f;

    private static final Matrix4f IDENTITY = new Matrix4f();

    private static VertexBuffer quad;
    private static TextureTarget scratch;

    /** How much snow is on the lens right now, 0 to 1. */
    private static float snowOnLens;
    private static long lastFrameNanos;

    public static void render(Camera camera, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        // the lens belongs to the camera
        if (!mc.options.getCameraType().isFirstPerson()) return;
        if (camera.getFluidInCamera() == FogType.WATER) return;   // underwater is its own thing

        ShaderInstance shader = LensSnowShaderRegistry.lensSnowShader;
        ShaderInstance copy = LensSnowShaderRegistry.copyShader;
        if (shader == null || copy == null) return;

        float dt = frameDelta();

        // is it actually snowing on the player?
        boolean snowing = ClientSeasonState.isSnowing()
                && ClientSeasonState.isPrecipitating()
                && mc.level.canSeeSky(mc.player.blockPosition());

        if (snowing) {
            snowOnLens = Math.min(MAX_SNOW, snowOnLens + BUILD_RATE * dt);
        } else {
            snowOnLens = Math.max(0.0f, snowOnLens - MELT_RATE * dt);
        }
        if (snowOnLens < 0.004f) {
            return;   // nothing on the lens
        }

        RenderTarget main = mc.getMainRenderTarget();
        ensureQuad();
        ensureScratch(main.width, main.height);

        int[] boundFbo = new int[1];
        GL30C.glGetIntegerv(GL30C.GL_DRAW_FRAMEBUFFER_BINDING, boundFbo);
        int prevFbo = boundFbo[0];

        // a fragment shader must never sample the buffer it writes to,
        // so the frame goes into a scratch and is copied back
        scratch.bindWrite(true);

        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        shader.setSampler("DiffuseSampler", main.getColorTextureId());
        uniform2(shader, "ScreenSize", main.width, main.height);
        uniform1(shader, "Time", (float) (mc.level.getGameTime() % 100000L) / 20.0f + partialTick / 20.0f);
        uniform1(shader, "SnowAmount", snowOnLens);
        // while it is still snowing the flakes are not melting yet
        uniform1(shader, "Melt", snowing ? 0.0f : 1.0f);
        uniform1(shader, "Aspect", (float) main.width / main.height);
        drawQuad(shader);

        // copy the frame back
        GlStateManager._glBindFramebuffer(GL30C.GL_FRAMEBUFFER, prevFbo);
        GlStateManager._viewport(0, 0, main.width, main.height);
        copy.setSampler("DiffuseSampler", scratch.getColorTextureId());
        uniform2(copy, "ScreenSize", main.width, main.height);
        drawQuad(copy);

        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
    }

    /** The real time since the last frame, in seconds. */
    private static float frameDelta() {
        long now = System.nanoTime();
        float dt = lastFrameNanos == 0L ? 1.0f / 60.0f : (now - lastFrameNanos) / 1.0e9f;
        lastFrameNanos = now;
        return Math.min(0.1f, Math.max(0.0f, dt));
    }

    // ── GPU helpers ───────────────────────────────────────────────────────

    private static void drawQuad(ShaderInstance shader) {
        quad.bind();
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

    private static void uniform1(ShaderInstance shader, String name, float v) {
        Uniform u = shader.getUniform(name);
        if (u != null) u.set(v);
    }

    private static void uniform2(ShaderInstance shader, String name, float a, float b) {
        Uniform u = shader.getUniform(name);
        if (u != null) u.set(a, b);
    }
}
