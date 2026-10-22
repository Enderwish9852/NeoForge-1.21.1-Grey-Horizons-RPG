package net.enderwish.Atmospheric_Overhaul_Subpack.client.sky;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL30C;

/**
 * NightDarkeningRenderer
 *
 * NEW -- the night, darker than vanilla.
 *
 * Vanilla's night is bright: the sky light level stays high enough to
 * read the ground comfortably, which is the right call for a game
 * people play at night, and the wrong one for a world with moonlight
 * in it. A moon's shafts are only worth drawing if there is darkness
 * for them to be drawn against.
 *
 * This multiplies the world by a factor that falls as the sun goes
 * down, and it runs BEFORE the god rays are added, so the rays keep
 * their full brightness. Dark sky, cold shafts: the contrast the user
 * asked for.
 */
public final class NightDarkeningRenderer {

    private NightDarkeningRenderer() {}

    private static final Matrix4f IDENTITY = new Matrix4f();

    private static VertexBuffer quad;

    /**
     * Called at renderLevel TAIL, before the shafts are added.
     *
     * @param partialTick the frame's interpolation
     */
    public static void render(float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        ShaderInstance shader = SunShaderRegistry.nightShader;
        if (shader == null) return;

        float dayT = mc.level.getTimeOfDay(partialTick);
        float night = CelestialLight.nightAmount(dayT);
        float factor = 1.0f - CelestialLight.NIGHT_DARKEN * night;
        if (factor > 0.999f) return;   // daylight: nothing to do

        RenderTarget main = mc.getMainRenderTarget();
        ensureQuad();

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

        // the MAIN target: in Fabulous the terrain lives there
        main.bindWrite(true);

        Uniform u = shader.getUniform("NightFactor");
        if (u != null) u.set(factor);
        drawQuad(shader);

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
}
