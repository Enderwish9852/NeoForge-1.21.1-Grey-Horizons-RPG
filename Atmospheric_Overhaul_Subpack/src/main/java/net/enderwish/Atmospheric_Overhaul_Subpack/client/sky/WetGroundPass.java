package net.enderwish.Atmospheric_Overhaul_Subpack.client.sky;

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
import net.enderwish.Atmospheric_Overhaul_Subpack.client.water.WaterPostShaderRegistry;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL30C;

/**
 * WetGroundPass
 *
 * NEW -- puddles on the ground when it rains.
 *
 * This is a POST pass, at the very end of the frame, and that is the
 * whole point. A puddle is a shallow pool of water standing on the
 * surface, and what you mostly see in it is not the ground but the
 * REFLECTION of everything around it -- the trees above you, the sky,
 * the light. The terrain is drawn long before the trees are, so a
 * reflection computed while the terrain draws could never show them.
 * Doing it here, at the end, can.
 *
 * Two passes:
 *
 * 1. the march. For each pixel the world position and the surface
 *    normal come from the depth buffer, the puddles from a hard
 *    threshold (a soft gradient over a wide area reads as a FLOOD, not
 *    as puddles), and the reflected view ray is marched against the
 *    depth buffer the same way the water's own reflection pass does.
 *    The output is WHERE the reflection is, and how strongly it shows.
 *
 * 2. the composite. The reflection is mixed over the scene with the
 *    normal alpha blend, so the water is almost transparent looking
 *    straight down and a mirror at a grazing angle.
 *
 * The pass never reads the frame it is helping to build: the march
 * only reads the DEPTH, and the composite only reads the march's
 * result. That is what makes it safe to draw straight onto the frame.
 */
public final class WetGroundPass {

    private WetGroundPass() {}

    private static final Matrix4f IDENTITY = new Matrix4f();

    private static VertexBuffer quad;
    private static TextureTarget reflectionTarget;

    /**
     * Called at the very end of the frame, after the world, the
     * entities and the god rays are all drawn.
     */
    public static void render(Camera camera, Matrix4f viewMatrix, Matrix4f projectionMatrix,
                              float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        ShaderInstance march = WaterPostShaderRegistry.wetGroundShader;
        ShaderInstance composite = WaterPostShaderRegistry.wetCompositeShader;
        if (march == null || composite == null) return;

        float rain = rainLevel(mc.level, partialTick);
        if (rain <= 0.01f) return;   // nothing to reflect

        RenderTarget main = mc.getMainRenderTarget();
        ensureTargets(main.width, main.height);
        ensureQuad();

        Matrix4f invProj = new Matrix4f(projectionMatrix).invert();
        Matrix4f invView = new Matrix4f(viewMatrix).invert();
        Vec3 cam = camera.getPosition();
        boolean shadowUsable = SunShadowMap.isValid();
        float time = (mc.level.getGameTime() % 100000L) + partialTick * 0.05f;

        int[] boundFbo = new int[1];
        GL30C.glGetIntegerv(GL30C.GL_DRAW_FRAMEBUFFER_BINDING, boundFbo);
        int prevFbo = boundFbo[0];

        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        // ── 1. the march ────────────────────────────────────────────────
        reflectionTarget.bindWrite(true);
        march.setSampler("MainDepth", main.getDepthTextureId());
        march.setSampler("ghShadowMap", SunShadowMap.depthTextureId());
        uniformMat(march, "RayProjMat", projectionMatrix);
        uniformMat(march, "RayViewMat", viewMatrix);
        uniformMat(march, "RayInvProjMat", invProj);
        uniformMat(march, "RayInvViewMat", invView);
        uniformMat(march, "SunClipMat", SunShadowMap.viewProjection());
        uniform3(march, "CameraPos", (float) cam.x, (float) cam.y, (float) cam.z);
        uniform2(march, "ScreenSize", main.width, main.height);
        uniform1(march, "Time", time);
        uniform1(march, "RainWetness", rain);
        uniform1(march, "ShadowValid", shadowUsable ? 1.0f : 0.0f);
        drawQuad(march);

        // ── 2. the composite, straight onto the frame ───────────────────
        GlStateManager._glBindFramebuffer(GL30C.GL_FRAMEBUFFER, prevFbo);
        GlStateManager._viewport(0, 0, main.width, main.height);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        composite.setSampler("ReflectionSampler", reflectionTarget.getColorTextureId());
        composite.setSampler("DiffuseSampler", main.getColorTextureId());
        drawQuad(composite);

        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
    }

    /** How hard it is raining, 0..1. */
    public static float rainLevel(ClientLevel level, float partialTick) {
        float rain = level.getRainLevel(partialTick);
        if (net.enderwish.Atmospheric_Overhaul_Subpack.client.ClientSeasonState.isPrecipitating()) {
            rain = Math.max(rain,
                    net.enderwish.Atmospheric_Overhaul_Subpack.client.ClientSeasonState.getIntensity());
        }
        return Mth.clamp(rain, 0.0f, 1.0f);
    }

    // ── GPU helpers ───────────────────────────────────────────────────────

    private static void drawQuad(ShaderInstance shader) {
        quad.bind();
        quad.drawWithShader(IDENTITY, IDENTITY, shader);
        VertexBuffer.unbind();
    }

    private static void ensureTargets(int width, int height) {
        int hw = Math.max(1, width / 2);
        int hh = Math.max(1, height / 2);
        if (reflectionTarget == null) {
            reflectionTarget = new TextureTarget(hw, hh, false, false);
        } else if (reflectionTarget.width != hw || reflectionTarget.height != hh) {
            reflectionTarget.resize(hw, hh, false);
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

    private static void uniform3(ShaderInstance shader, String name, float a, float b, float c) {
        Uniform u = shader.getUniform(name);
        if (u != null) u.set(a, b, c);
    }

    private static void uniformMat(ShaderInstance shader, String name, Matrix4f m) {
        Uniform u = shader.getUniform(name);
        if (u != null) u.set(m);
    }
}
