package net.enderwish.Atmospheric_Overhaul_Subpack.client.water;

import com.mojang.math.Axis;
import net.enderwish.Atmospheric_Overhaul_Subpack.client.ClientSeasonState;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * WaterShaderUniforms — per-frame push of the gh_atmospheric water
 * uniforms into the overridden rendertype_translucent core shader.
 *
 * The water gate is exact: the still/flow sprite rectangles inside the
 * block atlas are read off the real stitched sprites every frame, so
 * only water pixels ever take the wave path — glass, ice, portals and
 * every other translucent block keep the bit-for-bit vanilla path.
 *
 * ANIMATION CLOCK (crest-twitch fix): the four wave phases are
 * accumulated HERE, tick by tick off the continuous game time, and
 * pushed pre-wrapped to [0, 2pi) as WavePhase. That replaces vanilla's
 * GameTime uniform, which (a) wraps once per in-game day — a one-frame
 * snap of the entire wave field every 20 real minutes — and (b) feeds
 * the shader raw tick-scale phases that sin() must range-reduce. The
 * shaders mod-wrap every argument to 2*pi, so sin() never sees a huge
 * number (GPU range-reduction loss at large world coordinates also
 * reads as crest/valley twitching).
 *
 * WIND SMOOTHING: the server evolves wind per tick but syncs snapshots
 * once a second (WindSyncPacket every 20 ticks), and direction shifts
 * in 45-degree steps — a raw step instantly re-arranges the whole wave
 * field. The water renders from an exponential moving average (~0.5 s)
 * of the live wind, so sync steps and direction rotations become a
 * gentle drift. Particles keep using the raw wind.
 *
 * Disable everything with -Dgh.water=false.
 */
public final class WaterShaderUniforms {

    public static final boolean ENABLED =
            !"false".equalsIgnoreCase(System.getProperty("gh.water", "true"));

    /** Wave amplitude scale (peak displacement ~0.08 blocks). */
    private static final float AMPLITUDE = 1.0f;
    /** Sun-glint strength. */
    private static final float GLINT = 1.0f;
    /** Grazing-angle opacity boost. */
    private static final float OPACITY = 1.0f;

    private static final float TAU = (float) (Math.PI * 2.0);

    /**
     * Per-tick phase rates of the four wave trains — must stay in lockstep
     * with the spatial frequencies in the shaders (0.55/1.10/2.10/4.20).
     */
    private static final float[] PHASE_RATES = {0.030f, 0.039f, 0.051f, 0.066f};

    /** Continuous, pre-wrapped wave phases (radians, [0, TAU)). */
    private static final float[] WAVE_PHASE = new float[4];

    /** EMA of the live wind — the wind the water actually renders with. */
    private static float windDx = 0.0f, windDz = 0.0f;

    /** Last continuous tick time; NaN until the first frame in a world. */
    private static float lastTickTime = Float.NaN;

    /**
     * Game-time wrap for the animation clock. 80 in-game days (~26.7 real
     * hours): keeps the float tick time precise, and the one-frame freeze
     * at the (rare) wrap is imperceptible, unlike a phase snap.
     */
    private static final long CLOCK_WRAP = 1_920_000L;

    private static final ResourceLocation WATER_STILL =
            ResourceLocation.withDefaultNamespace("block/water_still");
    private static final ResourceLocation ICE =
            ResourceLocation.withDefaultNamespace("block/ice");
    private static final ResourceLocation WATER_FLOW =
            ResourceLocation.withDefaultNamespace("block/water_flow");

    // ── Read access for the underwater-wobble post pass ───────────────────

    /**
     * The continuously accumulated wave phases (radians, [0, 2pi)) --
     * the SAME clock the water surface renders from, so the wobble
     * pass and the surface can never disagree. Read-only.
     */
    public static float[] getWavePhases() {
        return WAVE_PHASE;
    }

    /** Smoothed wind X the water renders with (WindSyncPacket EMA). */
    public static float getSmoothWindDx() {
        return windDx;
    }

    /** Smoothed wind Z the water renders with (WindSyncPacket EMA). */
    public static float getSmoothWindDz() {
        return windDz;
    }

    /**
     * Switches the overridden translucent core shader into MASK MODE:
     * water faces render flat opaque white, all other translucent
     * blocks (glass, ice, portals) discard. Used by the reflections
     * mask pass, which re-renders the translucent section layer into
     * a scratch target. Must be turned off again right after.
     */
    public static void setMaskMode(boolean on) {
        ShaderInstance shader = GameRenderer.getRendertypeTranslucentShader();
        if (shader == null) return;
        var uniform = shader.getUniform("MaskMode");
        if (uniform != null) uniform.set(on ? 1.0f : 0.0f);
    }

    private WaterShaderUniforms() {}

    /** Called every frame from LevelRendererWaterUniformsMixin (renderLevel HEAD). */
    public static void push(Camera camera, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        ShaderInstance shader = GameRenderer.getRendertypeTranslucentShader();
        if (shader == null) return;

        // ── Animation clock: continuous, per-frame tick time ─────────────
        float now = (mc.level.getGameTime() % CLOCK_WRAP) + partialTick;
        float dt = 0.0f;
        if (!Float.isNaN(lastTickTime)) {
            dt = now - lastTickTime;
            if (dt < 0.0f || dt > 600.0f) dt = 0.0f;   // clock jump / world swap
        }
        lastTickTime = now;

        // ── Wind EMA (time-based, frame-rate independent) ─────────────────
        float rawDx = ClientSeasonState.getWindDx();
        float rawDz = ClientSeasonState.getWindDz();
        float k = 1.0f - (float) Math.exp(-dt * 0.10f);   // ~0.5 s convergence
        windDx += (rawDx - windDx) * k;
        windDz += (rawDz - windDz) * k;

        // ── Phase accumulation (rates match the shader's wave trains) ────
        float windLen = Math.min(
                (float) Math.sqrt(windDx * windDx + windDz * windDz), 1.0f);
        float speed = 0.35f + 2.65f * windLen;
        for (int i = 0; i < 4; i++) {
            WAVE_PHASE[i] = (WAVE_PHASE[i] + PHASE_RATES[i] * speed * dt) % TAU;
        }

        if (!ENABLED) {
            // gate can never be true: push the rects out of range
            set(shader, "WaterStillUV", -1, -1, -1, -1);
            set(shader, "WaterFlowUV", -1, -1, -1, -1);
            return;
        }

        // exact water sprite rectangles in the stitched block atlas
        TextureAtlas atlas = mc.getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS);
        TextureAtlasSprite still = atlas.getSprite(WATER_STILL);
        TextureAtlasSprite flow = atlas.getSprite(WATER_FLOW);
        set(shader, "WaterStillUV",
                still.getU0(), still.getV0(), still.getU1(), still.getV1());
        set(shader, "WaterFlowUV",
                flow.getU0(), flow.getV0(), flow.getU1(), flow.getV1());

        // the ice gets its own rectangle: it is a mirror, not a wave
        TextureAtlasSprite ice = atlas.getSprite(ICE);
        set(shader, "IceUV",
                ice.getU0(), ice.getV0(), ice.getU1(), ice.getV1());

        // smoothed wind drives wave direction/speed/choppiness
        set(shader, "WindVec", windDx, windDz);

        // camera anchor for the world-space wave field
        Vec3 pos = camera.getPosition();
        set(shader, "CameraPos", (float) pos.x, (float) pos.y, (float) pos.z);

        // sun/moon direction in world space — the same construction the
        // god-ray renderer uses (GodRayRenderer.render)
        float dayT = mc.level.getTimeOfDay(partialTick);
        Matrix4f sunMat = new Matrix4f()
                .rotate(Axis.YP.rotationDegrees(-90.0f))
                .rotate(Axis.XP.rotationDegrees(dayT * 360.0f));
        Vector3f sun = sunMat.transformDirection(new Vector3f(0.0f, 1.0f, 0.0f));
        set(shader, "SunDir", sun.x, sun.y, sun.z);

        set(shader, "WaterParams", AMPLITUDE, GLINT, OPACITY, 0.0f);
        set(shader, "WavePhase", WAVE_PHASE[0], WAVE_PHASE[1], WAVE_PHASE[2], WAVE_PHASE[3]);
    }

    private static void set(ShaderInstance shader, String name, float... values) {
        var uniform = shader.getUniform(name);
        if (uniform == null) return;   // shader json not ours / not loaded yet
        switch (values.length) {
            case 2 -> uniform.set(values[0], values[1]);
            case 3 -> uniform.set(values[0], values[1], values[2]);
            case 4 -> uniform.set(values[0], values[1], values[2], values[3]);
            default -> uniform.set(values[0]);
        }
    }
}
