package net.enderwish.Atmospheric_Overhaul_Subpack.client.sky;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.math.Axis;
import net.enderwish.Atmospheric_Overhaul_Subpack.client.ClientSeasonState;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * WetShaderUniforms
 *
 * NEW -- per-frame push of the rain's wet look into the overridden
 * terrain core shaders (rendertype_solid and rendertype_cutout_mipped).
 *
 * The terrain shaders are vanilla's, with the wet look added: the
 * faces that point up go dark and glossy when the rain is on them,
 * with a sheen and a sun glint.
 *
 * "IS THE SKY OPEN OVER HERE?" is answered by the sun's shadow map,
 * which the mod already keeps for the god rays. A roof or a canopy
 * puts the ground in shadow in exactly the same way it keeps the rain
 * off it, so the shadow test IS the exposure test, and the ground
 * under a roof stays dry.
 *
 * The map is rendered later in the frame than the terrain, so the
 * terrain reads the previous frame's -- a frame of lag on a shadow
 * map that is itself only updated every other frame, which is well
 * under what the eye can pick out.
 */
public final class WetShaderUniforms {

    private WetShaderUniforms() {}

    /** Called every frame, alongside the water uniforms. */
    public static void push(Camera camera, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        // how hard it is raining
        float rain = mc.level.getRainLevel(partialTick);
        if (ClientSeasonState.isPrecipitating()) {
            rain = Math.max(rain, ClientSeasonState.getIntensity());
        }
        rain = Math.max(0.0f, Math.min(1.0f, rain));

        // the sun, in world space
        float dayT = mc.level.getTimeOfDay(partialTick);
        Vector3f sun = new Matrix4f()
                .rotate(Axis.YP.rotationDegrees(-90.0f))
                .rotate(Axis.XP.rotationDegrees(dayT * 360.0f))
                .transformDirection(new Vector3f(0.0f, 1.0f, 0.0f));

        Vec3 pos = camera.getPosition();
        boolean shadowUsable = SunShadowMap.isValid();

        ShaderInstance solid = GameRenderer.getRendertypeSolidShader();
        ShaderInstance cutout = GameRenderer.getRendertypeCutoutMippedShader();
        pushTo(solid, rain, sun, pos, shadowUsable);
        pushTo(cutout, rain, sun, pos, shadowUsable);
    }

    private static void pushTo(ShaderInstance shader, float rain, Vector3f sun, Vec3 pos,
                               boolean shadowUsable) {
        if (shader == null) return;   // not loaded yet, or not ours
        set(shader, "ghRainWetness", rain);
        set(shader, "ghShadowValid", shadowUsable ? 1.0f : 0.0f);
        set(shader, "ghCameraPos", (float) pos.x, (float) pos.y, (float) pos.z);
        set(shader, "ghSunDir", sun.x, sun.y, sun.z);
        Uniform u = shader.getUniform("ghSunClip");
        if (u != null) u.set(SunShadowMap.viewProjection());
        shader.setSampler("ghShadowMap", SunShadowMap.depthTextureId());
    }

    private static void set(ShaderInstance shader, String name, float... values) {
        var uniform = shader.getUniform(name);
        if (uniform == null) return;   // the shader json is not ours
        switch (values.length) {
            case 2 -> uniform.set(values[0], values[1]);
            case 3 -> uniform.set(values[0], values[1], values[2]);
            case 4 -> uniform.set(values[0], values[1], values[2], values[3]);
            default -> uniform.set(values[0]);
        }
    }
}
