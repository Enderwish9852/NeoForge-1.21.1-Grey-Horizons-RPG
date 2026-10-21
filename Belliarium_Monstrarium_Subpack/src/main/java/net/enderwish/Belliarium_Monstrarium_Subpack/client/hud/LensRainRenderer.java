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
import net.enderwish.Belliarium_Monstrarium_Subpack.core.ModAttachments;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.survival.SurvivalCapability;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FogType;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL30C;

/**
 * LensRainRenderer
 *
 * NEW -- water on the camera lens.
 *
 * Two things put water on it:
 *
 * 1. THE RAIN. When it is raining and the wind blows into the player,
 *    the rain lands on the lens and runs DOWN it, and the world bends
 *    behind every drop. When the wind blows the other way the rain
 *    never reaches the face, and all you get is the odd small splash.
 *
 * 2. COMING OUT OF THE WATER. Surfacing puts a sheet of water on the
 *    lens that runs off over a couple of seconds, and then it settles
 *    into the ordinary drops.
 *
 * How much is on the lens follows the player's BODY WETNESS, which is
 * the survival system's own number: the body is soaked in the rain,
 * and it dries out again over minutes. Below half dry the lens is
 * clear, and between half dry and soaked the drops get fewer and
 * smaller as the player dries -- so the effect fades out on its own
 * as the weather passes.
 *
 * The wetness arrives from the server every ten ticks, so it is
 * smoothed here; otherwise the drops would pop twice a second.
 *
 * The lens belongs to the camera, so it is first person only.
 */
public final class LensRainRenderer {

    private LensRainRenderer() {}

    // ── Tuning ─────────────────────────────────────────────────────────────

    /** How long the water runs off when you surface, in seconds. */
    private static final float SLUSH_SECONDS = 2.2f;
    /** The wetness below which the lens is clear. */
    private static final float WET_THRESHOLD = 50.0f;
    /** How quickly the smoothed wetness catches up with the synced one. */
    private static final float WET_SMOOTHING = 4.0f;

    // ── State ──────────────────────────────────────────────────────────────

    /** Whether the camera was in the water last frame. */
    private static boolean wasInWater;
    /** Counts down after the player surfaces. */
    private static float slushTimer;
    /** The synced wetness, smoothed. */
    private static float smoothWetness;
    private static long lastFrameNanos;

    private static final Matrix4f IDENTITY = new Matrix4f();

    private static VertexBuffer quad;
    private static TextureTarget scratch;

    /** The scratch the distorted frame is written into before it is copied back. */
    private static void ensureScratch(int width, int height) {
        if (scratch == null) {
            scratch = new TextureTarget(width, height, false, false);
        } else if (scratch.width != width || scratch.height != height) {
            scratch.resize(width, height, false);
        }
    }

    /**
     * Called at renderLevel TAIL, after the world is drawn and before
     * the GUI.
     */
    public static void render(Camera camera, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        // the lens belongs to the camera
        if (!mc.options.getCameraType().isFirstPerson()) return;
        ShaderInstance shader = LensRainShaderRegistry.lensRainShader;
        ShaderInstance copy = LensRainShaderRegistry.copyShader;
        if (shader == null || copy == null) return;

        float dt = frameDelta();

        // ── the slush: you have just come up out of the water ───────────
        boolean inWater = camera.getFluidInCamera() == FogType.WATER;
        if (wasInWater && !inWater) {
            slushTimer = SLUSH_SECONDS;
        }
        wasInWater = inWater;
        if (slushTimer > 0.0f) {
            slushTimer = Math.max(0.0f, slushTimer - dt);
        }
        float slush = Mth.clamp(slushTimer / SLUSH_SECONDS, 0.0f, 1.0f);

        // ── how wet the lens is ───────────────────────────────────────────
        float wetness = 0.0f;
        SurvivalCapability survival = mc.player.getData(ModAttachments.SURVIVAL);
        if (survival != null) {
            wetness = survival.getWetness();
        }
        // the sync arrives every ten ticks, so smooth it
        smoothWetness += (wetness - smoothWetness) * Math.min(1.0f, dt * WET_SMOOTHING);
        if (smoothWetness < 0.05f && slush < 0.01f) {
            return;
        }

        // the drops fade out as the player dries
        float lens = Mth.clamp((smoothWetness - WET_THRESHOLD) / (100.0f - WET_THRESHOLD), 0.0f, 1.0f);

        // ── the wind decides the character of the rain ───────────────────
        // The wind's velocity is the direction it blows TOWARD. If that
        // is the way the player is facing, the wind is at their back
        // and the rain never reaches the lens; if it is the other way,
        // the rain comes straight at the face and runs down the lens.
        Vector3f look = camera.getLookVector();
        float fx = look.x();
        float fz = look.z();
        float flen = (float) Math.sqrt(fx * fx + fz * fz);
        if (flen > 1.0e-4f) {
            fx /= flen;
            fz /= flen;
        }
        float windX = ClientSeasonState.getWindDx();
        float windZ = ClientSeasonState.getWindDz();
        float dot = windX * fx + windZ * fz;
        boolean intoPlayer = dot < 0.0f;
        float stream = intoPlayer ? 1.0f : 0.2f;

        // the rain itself, and only if it can actually reach the player
        boolean raining = (mc.level.isRaining() || ClientSeasonState.isPrecipitating())
                && mc.level.canSeeSky(mc.player.blockPosition());
        if (raining) {
            float rain = Mth.clamp(mc.level.getRainLevel(partialTick), 0.0f, 1.0f);
            float fromRain = intoPlayer ? 0.85f * rain : 0.30f * rain;
            lens = Math.max(lens, fromRain);
        }

        if (lens < 0.01f && slush < 0.01f) {
            return;   // nothing on the lens
        }

        // ── the pass ──────────────────────────────────────────────────────
        RenderTarget main = mc.getMainRenderTarget();
        ensureQuad();

        int[] boundFbo = new int[1];
        GL30C.glGetIntegerv(GL30C.GL_DRAW_FRAMEBUFFER_BINDING, boundFbo);
        int prevFbo = boundFbo[0];

        // a fragment shader must never sample the buffer it writes to,
        // so the distorted frame goes into a scratch and is copied back
        ensureScratch(main.width, main.height);
        scratch.bindWrite(true);

        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        shader.setSampler("DiffuseSampler", main.getColorTextureId());
        uniform2(shader, "ScreenSize", main.width, main.height);
        uniform1(shader, "Time", (float) (mc.level.getGameTime() % 100000L) / 20.0f + partialTick / 20.0f);
        uniform1(shader, "DropletAmount", lens);
        // the size of a drop, as a fraction of a grid cell: a drop is a
        // few pixels across, not a quarter of the screen
        uniform1(shader, "DropletSize", 0.35f + 0.45f * lens);
        uniform1(shader, "StreamBias", stream);
        uniform1(shader, "Slush", slush);
        uniform1(shader, "Aspect", (float) main.width / main.height);
        drawQuad(shader);

        // copy the distorted frame back
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
