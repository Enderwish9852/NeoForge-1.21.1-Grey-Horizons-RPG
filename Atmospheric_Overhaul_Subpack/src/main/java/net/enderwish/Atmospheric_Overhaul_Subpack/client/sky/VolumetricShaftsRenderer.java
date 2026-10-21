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
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL30C;

/**
 * VolumetricShaftsRenderer
 *
 * NEW -- the true god rays: a ray march through the air, asking the
 * sun's shadow map at every step whether that bit of air sees the sun.
 *
 * This is the counterpart to GodRayRenderer, which is a screen-space
 * smear of the sun's glow. The smear is cheap and pretty but it only
 * exists where the sun is in frame, and it is a 2D trick: what it
 * draws cannot be occluded correctly and does not respond to the
 * scene. The march is the real thing: shafts from any angle, the sun
 * behind you included, and every occluder in the world casting a real
 * shadow into the light.
 *
 * Runs at half resolution (the shafts are soft, and a full resolution
 * march of 16 samples is a lot of texture fetches), then the result
 * is added to the frame with a bilinear upscale.
 *
 * The march is clipped to the scene depth, so a wall in front of the
 * camera does not end up with shafts drawn on it.
 */
public final class VolumetricShaftsRenderer {

    private VolumetricShaftsRenderer() {}

    // ── Tuning ─────────────────────────────────────────────────────────────

    /** How far along the view ray the march reaches, in blocks. */
    private static final float MAX_DISTANCE = 80.0f;
    /**
     * How strongly the light scatters towards the eye. Higher means
     * the shafts blaze when you look at the sun and are a faint glow
     * when you look away; lower means they are more even.
     */
    private static final float PHASE_G = 0.45f;
    private static final float STRENGTH = 0.90f;

    /**
     * How strongly the sun's light counts against the sky's, at the
     * zenith. A clear noon shadow is strong -- around a third of the
     * sunlit brightness -- and this is the number that puts it there.
     */
    private static final float SUN_STRENGTH = 0.65f;

    private static final Matrix4f IDENTITY = new Matrix4f();

    private static VertexBuffer quad;
    private static TextureTarget halfRes;
    private static int frameCounter;

    /**
     * Called after the sun shadow map has been rendered.
     */
    public static void render(Camera camera, Matrix4f viewMatrix, Matrix4f projectionMatrix,
                              float partialTick) {
        if (!SunShadowMap.isValid()) return;
        ShaderInstance shader = SunShaderRegistry.shadowShader;
        ShaderInstance copy = WaterPostShaderRegistry.wobbleShader;
        if (shader == null || copy == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        RenderTarget main = mc.getMainRenderTarget();
        ensureTargets(main.width, main.height);
        ensureQuad();

        Vector3f sun = SunRenderer.worldDirection(mc.level.getTimeOfDay(partialTick));
        if (sun.y <= 0.02f) return;

        // warm at the horizon, near-white higher up
        Vector3f sunColour = SunRenderer.color(sun);
        float tintR = sunColour.x;
        float tintG = sunColour.y;
        float tintB = sunColour.z;
        float rain = mc.level.getRainLevel(partialTick);
        float strength = STRENGTH * (1.0f - 0.75f * rain);

        // the walk-cycle head bob is applied to the projection matrix,
        // and the shafts are real light in real air: they should not
        // inherit a fake movement of the camera
        Matrix4f steadyProjection = SunRenderer.unbobbedProjection(projectionMatrix);
        Matrix4f invProj = new Matrix4f(steadyProjection).invert();
        Matrix4f invView = new Matrix4f(viewMatrix).invert();
        Vec3 cam = camera.getPosition();

        int[] boundFbo = new int[1];
        GL30C.glGetIntegerv(GL30C.GL_DRAW_FRAMEBUFFER_BINDING, boundFbo);
        int prevFbo = boundFbo[0];

        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        // ── the march, at half resolution ─────────────────────────────────
        halfRes.bindWrite(true);
        shader.setSampler("ShadowMap", SunShadowMap.depthTextureId());
        shader.setSampler("MainDepth", main.getDepthTextureId());
        uniformMat(shader, "SunViewProjMat", SunShadowMap.viewProjection());
        uniformMat(shader, "RayInvProjMat", invProj);
        uniformMat(shader, "RayInvViewMat", invView);
        uniform3(shader, "CameraPos", (float) cam.x, (float) cam.y, (float) cam.z);
        uniform3(shader, "SunDir", sun.x, sun.y, sun.z);
        uniform3(shader, "ShaftColor", tintR, tintG, tintB);
        uniform1(shader, "ShaftStrength", strength);
        uniform1(shader, "MaxDistance", MAX_DISTANCE);
        uniform1(shader, "PhaseG", PHASE_G);
        // The jitter walks the ray's start offset about each frame so the
        // discrete march turns into fine noise instead of rings. It must
        // NOT walk while the game is paused, or the shafts would keep
        // shimmering behind the pause screen.
        if (!mc.isPaused()) frameCounter++;
        uniform1(shader, "FrameJitter", (frameCounter % 64) * 0.618034f);
        uniform2(shader, "DepthRange", 0.05f, mc.gameRenderer.getDepthFar());
        drawQuad(shader);

        // ── add it to the frame ──────────────────────────────────────────
        GlStateManager._glBindFramebuffer(GL30C.GL_FRAMEBUFFER, prevFbo);
        GlStateManager._viewport(0, 0, main.width, main.height);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
        copy.setSampler("DiffuseSampler", halfRes.getColorTextureId());
        uniform4(copy, "WobbleParams", 0.0f, 0.0f, 0.0f, 0.0f);
        uniform2(copy, "ScreenSize", main.width, main.height);
        drawQuad(copy);

        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
    }

    /**
     * The sun's shadow ON THE SURFACES.
     *
     * Minecraft's lighting has no sun in it, so nothing in the world
     * is ever in shadow. This adds it: each pixel's world position is
     * found from the depth buffer, the shadow map is asked whether
     * the sun reaches it, and what the sun cannot reach is darkened.
     *
     * The map holds the terrain, the leaves and the clouds, so this
     * is the shadow of a tree, the shadow of a roof and the shadow of
     * a cloud all at once.
     *
     * The pass outputs a factor and the blend does the multiplying,
     * so the frame is never read back.
     */
    public static void renderGroundShadow(Camera camera, Matrix4f viewMatrix, Matrix4f projectionMatrix,
                                           float partialTick) {
        if (!SunShadowMap.isValid()) return;
        ShaderInstance shader = SunShaderRegistry.groundShadowShader;
        if (shader == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        RenderTarget main = mc.getMainRenderTarget();
        ensureQuad();

        Vector3f sun = SunRenderer.worldDirection(mc.level.getTimeOfDay(partialTick));
        if (sun.y <= 0.02f) return;

        // ── how much the sun is actually worth ──────────────────────────
        //
        // A surface in the open is lit by the sky AND by the sun; a
        // surface in shadow keeps only the sky's share. So what a
        // shadow has to multiply the surface by is
        //
        //     ambient / (ambient + sun)
        //
        // and that has to be done PER CHANNEL, because the two are
        // different colours: the sky's light is blue and the sun's is
        // warm. It comes out right on its own -- at noon the shadow is
        // strong and blue, and at sunrise the sun is so weak that
        // there is barely a shadow at all, just a faint blue one.
        //
        // The ambient is Minecraft's own sky light: a level, times the
        // blue tint LightTexture puts on it.
        float skyDarken = mc.level.getSkyDarken(1.0F);
        float ambientLevel = skyDarken * 0.95f + 0.05f;
        // LightTexture tints the sky light blue by mixing it 35% of the
        // way to white; the blue channel starts at 1 and stays there.
        float skyR = Mth.lerp(0.35f, skyDarken, 1.0f);
        float skyG = skyR;
        float skyB = 1.0f;

        // the sun: its irradiance on a horizontal surface, and its
        // colour -- white high up, deep orange at the horizon
        float rain = mc.level.getRainLevel(partialTick);
        float sunStrength = SUN_STRENGTH * Math.max(0.0f, sun.y) * (1.0f - 0.6f * rain);
        Vector3f sunColour = SunRenderer.color(sun);
        float sunR = sunColour.x;
        float sunG = sunColour.y;
        float sunB = sunColour.z;

        float ambientR = skyR * ambientLevel;
        float ambientG = skyG * ambientLevel;
        float ambientB = skyB * ambientLevel;

        float factorR = ambientR / Math.max(1.0e-4f, ambientR + sunStrength * sunR);
        float factorG = ambientG / Math.max(1.0e-4f, ambientG + sunStrength * sunG);
        float factorB = ambientB / Math.max(1.0e-4f, ambientB + sunStrength * sunB);
        if (factorR > 0.995f && factorG > 0.995f && factorB > 0.995f) {
            return;   // the sun is worth nothing: no shadow to cast
        }

        Matrix4f steadyProjection = SunRenderer.unbobbedProjection(projectionMatrix);
        Matrix4f invProj = new Matrix4f(steadyProjection).invert();
        Matrix4f invView = new Matrix4f(viewMatrix).invert();
        Vec3 cam = camera.getPosition();

        int[] boundFbo = new int[1];
        GL30C.glGetIntegerv(GL30C.GL_DRAW_FRAMEBUFFER_BINDING, boundFbo);
        int prevFbo = boundFbo[0];

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        // multiply: the shader outputs the factor, the blend applies it
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.DST_COLOR, GlStateManager.DestFactor.ZERO,
                GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);

        // The MAIN target, not whatever happens to be bound: in Fabulous
        // the terrain lives in the main target and the water in the
        // translucent one, and it is the terrain that has to be in the
        // cloud's shadow.
        main.bindWrite(true);

        shader.setSampler("MainDepth", main.getDepthTextureId());
        shader.setSampler("ShadowMap", SunShadowMap.depthTextureId());
        uniformMat(shader, "SunViewProjMat", SunShadowMap.viewProjection());
        uniformMat(shader, "RayInvProjMat", invProj);
        uniformMat(shader, "RayInvViewMat", invView);
        uniform3(shader, "CameraPos", (float) cam.x, (float) cam.y, (float) cam.z);
        uniform2(shader, "DepthRange", 0.05f, mc.gameRenderer.getDepthFar());
        uniform3(shader, "ShadowFactor", factorR, factorG, factorB);
        uniform1(shader, "ShadowTexel", 1.0f / SunShadowMap.SIZE);        drawQuad(shader);

        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();

        if (prevFbo != 0) {
            GlStateManager._glBindFramebuffer(GL30C.GL_FRAMEBUFFER, prevFbo);
            GlStateManager._viewport(0, 0, main.width, main.height);
        }
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
        if (halfRes == null) {
            halfRes = new TextureTarget(hw, hh, false, false);
            // bilinear, so the upscale to full resolution is smooth
            GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, halfRes.getColorTextureId());
            GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MIN_FILTER, GL11C.GL_LINEAR);
            GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MAG_FILTER, GL11C.GL_LINEAR);
            GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, 0);
        } else if (halfRes.width != hw || halfRes.height != hh) {
            halfRes.resize(hw, hh, false);
            GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, halfRes.getColorTextureId());
            GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MIN_FILTER, GL11C.GL_LINEAR);
            GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MAG_FILTER, GL11C.GL_LINEAR);
            GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, 0);
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

    private static void uniform4(ShaderInstance shader, String name, float a, float b, float c, float d) {
        Uniform u = shader.getUniform(name);
        if (u != null) u.set(a, b, c, d);
    }

    private static void uniformMat(ShaderInstance shader, String name, Matrix4f m) {
        Uniform u = shader.getUniform(name);
        if (u != null) u.set(m);
    }
}
