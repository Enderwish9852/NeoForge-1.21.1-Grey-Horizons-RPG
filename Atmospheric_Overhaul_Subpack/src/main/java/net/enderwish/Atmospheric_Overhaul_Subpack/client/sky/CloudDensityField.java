package net.enderwish.Atmospheric_Overhaul_Subpack.client.sky;

import net.minecraft.util.Mth;

/**
 * CloudDensityField
 *
 * NEW -- the 3D density function behind the voxel clouds: a pure
 * function of (seed, x, y, z) in CLOUD coordinates (world minus drift),
 * so any tile can be meshed anywhere, at any time, and every tile edge
 * matches its neighbour cell-for-cell. No chunk storage, no state.
 *
 * The shape, read top to bottom, is a cumulus field:
 * 1. COVERAGE -- a low-frequency fbm mask smoothstepped into 0..1:
 *    where the sky holds cloud mass at all, and where it is clear.
 *    This is what broke the old engine: its "cluster" mask inverted the
 *    fill test (coverage EASED filling at the rim) and drew a solid
 *    tiled ceiling instead of distinct masses.
 * 2. VERTICAL PROFILE -- a flat-ish condensation BASE altitude with a
 *    gentle wave, and a TOP that per-clump billows 16..50 blocks above
 *    it; density ramps in fast at the base (flat bottoms) and falls off
 *    smoothly toward the cap (rounded tops).
 * 3. GRAIN -- a single value-noise octave scales the result so flanks
 *    break into rounded puffs instead of flat slabs.
 *
 * All noise is seeded lattice value noise with a splitmix-style
 * finalizer; octaves are offset and non-power lacunarity (2.02) so
 * octave grids never realign.
 */
public final class CloudDensityField {

    private CloudDensityField() {}

    // ── Layer geometry (cloud-space Y) ──────────────────────────────────

    /** bottom of the meshed band; tile vertices are local to this */
    public static final double LAYER_BOTTOM = 112.0;
    /** top of the meshed band (base + max top margin stay under this) */
    public static final double LAYER_TOP = 208.0;

    // ── Tuning (v1 constants; config exposure comes later) ───────────────

    private static final double COVER_SCALE = 1.0 / 220.0;  // weather systems, hundreds of blocks
    private static final double BASE_SCALE = 1.0 / 96.0;   // condensation-altitude waves
    private static final double SHAPE_SCALE = 1.0 / 44.0;  // clump silhouettes
    private static final double GRAIN_SCALE = 1.0 / 21.0;  // puff surface grain

    private static final double BASE_Y = 140.0;
    private static final double BASE_WAVE = 10.0;
    private static final double TOP_MIN = 16.0;
    private static final double TOP_VAR = 34.0;

    // ── The rain ──────────────────────────────────────────────────────
    // The coverage's thresholds, and how far they drop in a full
    // rainstorm -- which is how much more of the sky holds cloud, and
    // how much the separate cumulus masses join up.
    private static final double COVER_LO = 0.30;
    private static final double COVER_HI = 0.58;
    private static final double COVER_RAIN_SHIFT = 0.28;
    /** How much deeper a rain cloud is than a cumulus one. */
    private static final double TOP_RAIN = 26.0;
    /** How much denser a rain cloud is. */
    private static final double DENSITY_RAIN = 0.45;

    // ── The field ───────────────────────────────────────────────────────

    /**
     * Cloud density at (x, y, z) in cloud coordinates. Roughly [0, 1];
     * the mesher cuts cells above CloudMesher.THRESHOLD.
     *
     * @param rain 0 in clear weather, 1 in a full rainstorm. The rain
     *             spreads the coverage so that the separate cumulus
     *             masses join up into big weather systems, makes them
     *             deeper and denser, and drops their base.
     */
    public static double sample(long seed, double x, double y, double z, double rain) {
        // 1) coverage: where the sky holds cloud mass at all. In the
        //    rain the thresholds drop, so more of the sky holds cloud
        //    and the gaps between the masses close up.
        double coverLo = COVER_LO - COVER_RAIN_SHIFT * rain;
        double coverHi = COVER_HI - COVER_RAIN_SHIFT * rain;
        double cover = smoothstep(coverLo, coverHi,
                fbm2(seed ^ 0x1A2B3C4DL, x, z, COVER_SCALE, 4));
        if (cover <= 0.0) return 0.0;

        // 2) vertical profile: flat base, rounded billowing top. Rain
        //    clouds are much deeper and their base sits lower.
        double base = BASE_Y + BASE_WAVE * fbm2(seed ^ 0x5E6F7081L, x, z, BASE_SCALE, 3)
                - 4.0 * rain;
        double top = base + TOP_MIN
                + TOP_VAR * (0.5 + 0.5 * fbm2(seed ^ 0x9A0B1C2DL, x, z, SHAPE_SCALE, 3))
                + TOP_RAIN * rain;
        if (y <= base - 6.0 || y >= top + 6.0) return 0.0;

        double t = Mth.clamp((y - (base - 6.0)) / (top - base + 12.0), 0.0, 1.0);
        double belly = t < 0.22 ? t / 0.22 : 1.0;              // fast ramp: flat-ish bottoms
        double cap = 1.0 - smoothstep(0.45, 1.0, t);           // ease-out: rounded tops

        // 3) grain: break the flanks into puffs
        double grain = 0.68 + 0.32 * valueNoise2(seed ^ 0xC3D4E5F6L, x * GRAIN_SCALE, z * GRAIN_SCALE);

        // and the rain makes the whole thing denser
        return cover * belly * cap * grain * (1.0 + DENSITY_RAIN * rain);
    }

    // ── Noise ───────────────────────────────────────────────────────────

    /** Fractal value noise, centered around 0 (roughly [-1, 1]). */
    private static double fbm2(long seed, double x, double z, double scale, int octaves) {
        double sum = 0.0;
        double amp = 1.0;
        double norm = 0.0;
        double fx = x * scale;
        double fz = z * scale;
        for (int o = 0; o < octaves; o++) {
            sum += amp * (valueNoise2(seed + o * 1013L, fx, fz) * 2.0 - 1.0);
            norm += amp;
            amp *= 0.5;
            fx = fx * 2.02 + 13.7;
            fz = fz * 2.02 + 91.3;
        }
        return sum / norm;
    }

    /** Smoothly interpolated lattice value noise in [0, 1]. */
    private static double valueNoise2(long seed, double x, double z) {
        long xi = floorL(x), zi = floorL(z);
        double u = smoothFade(x - xi), v = smoothFade(z - zi);
        double a = lattice(seed, xi, zi);
        double b = lattice(seed, xi + 1, zi);
        double c = lattice(seed, xi, zi + 1);
        double d = lattice(seed, xi + 1, zi + 1);
        return Mth.lerp(v, Mth.lerp(u, a, b), Mth.lerp(u, c, d));
    }

    private static double smoothFade(double t) {
        return t * t * (3.0 - 2.0 * t);
    }

    /** Seeded lattice hash in [0, 1): splitmix-style finalizer. */
    private static double lattice(long seed, long xi, long zi) {
        long h = seed * 0x9E3779B97F4A7C15L;
        h += xi * 0x87C37B91114253D5L;
        h += zi * 0x4CF5AD432745937L;
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        h ^= h >>> 31;
        return (h & 0xFFFFFFFFL) / 4294967296.0;
    }

    private static double smoothstep(double edge0, double edge1, double x) {
        double t = Mth.clamp((x - edge0) / (edge1 - edge0), 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }

    private static long floorL(double v) {
        long l = (long) v;
        return v < l ? l - 1 : l;
    }
}
