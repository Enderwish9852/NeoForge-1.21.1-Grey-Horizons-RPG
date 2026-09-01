package net.enderwish.TerraForma_Subpack.core.worldgen.noise;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * GHNoiseRouter
 *
 * The terrain shape system. Five noise layers combine
 * to produce realistic geography:
 *
 * Layer 1 — Continental (~20,000 block scale)
 *   Determines where land rises above sea level.
 *   High values = continental interior (mountains possible)
 *   Low values  = ocean floor
 *   Medium      = coastal lowlands, islands
 *
 * Layer 2 — Erosion (~5,000 block scale)
 *   Controls how "worn down" the terrain is.
 *   High erosion = flat plains, river valleys
 *   Low erosion  = sharp mountains, rocky terrain
 *
 * Layer 3 — Peaks and Valleys (~1,000 block scale)
 *   Creates mountain ranges and valley systems.
 *   Works WITH erosion — eroded mountains become hills.
 *
 * Layer 4 — Surface Detail (~100 block scale)
 *   Small-scale roughness: cliffs, rock outcrops,
 *   uneven ground, surface variation.
 *
 * Layer 5 — Cave Systems (3D, ~80 block scale)
 *   Underground cavity placement.
 *   Two noise fields that multiply — only creates
 *   caves where BOTH are near zero (Swiss cheese model).
 *
 * Final height formula:
 *   baseHeight = SEA_LEVEL + continental * (1 - erosion) * peaksAndValleys
 *   finalHeight = baseHeight + surfaceDetail
 *
 * All values are seeded from the world seed via GHBiomeSource.
 */
public class GHNoiseRouter {

    // ── Height constants ──────────────────────────────────────────────────────
    public static final int SEA_LEVEL        = 63;
    public static final int MIN_HEIGHT       = -64;
    public static final int MAX_HEIGHT       = 320;
    public static final int BEDROCK_HEIGHT   = MIN_HEIGHT + 5;

    // ── The five noise layers ─────────────────────────────────────────────────
    private final NoiseLayer continental;
    private final NoiseLayer erosion;
    private final NoiseLayer peaksAndValleys;
    private final NoiseLayer surfaceDetail;
    private final NoiseLayer caveA;  // cave system — field A
    private final NoiseLayer caveB;  // cave system — field B

    public GHNoiseRouter(RandomSource random) {
        continental    = new NoiseLayer("continental",
                RandomSource.create(random.nextLong()),
                20_000, 120, 4);

        erosion        = new NoiseLayer("erosion",
                RandomSource.create(random.nextLong()),
                5_000, 1, 3);   // amplitude 1 = normalized 0-1 range

        peaksAndValleys = new NoiseLayer("peaks_and_valleys",
                RandomSource.create(random.nextLong()),
                1_000, 80, 5);

        surfaceDetail  = new NoiseLayer("surface_detail",
                RandomSource.create(random.nextLong()),
                100, 8, 3);

        caveA          = new NoiseLayer("cave_a",
                RandomSource.create(random.nextLong()),
                80, 1, 2);

        caveB          = new NoiseLayer("cave_b",
                RandomSource.create(random.nextLong()),
                80, 1, 2);
    }

    // ── Surface height ────────────────────────────────────────────────────────

    /**
     * Returns the surface height (Y coordinate) at (x, z).
     * This is the block the player stands on.
     *
     * Range: MIN_HEIGHT to MAX_HEIGHT
     */
    public int getSurfaceHeight(int x, int z) {
        double cont = getContinentalValue(x, z);  // -120 to +120
        double eros = getErosionValue(x, z);       // 0.0 to 1.0
        double pv   = getPeaksAndValleysValue(x, z); // -80 to +80
        double det  = surfaceDetail.sample(x, z);  // -8 to +8

        // Continental sets the base land height above/below sea
        // Erosion flattens mountains (high erosion = flatter terrain)
        // Peaks and valleys add mountain ranges on top
        double baseHeight = SEA_LEVEL
                + cont * (1.0 - eros)
                + pv * (1.0 - eros * 0.7);

        double finalHeight = baseHeight + det;

        return (int) Mth.clamp(finalHeight, MIN_HEIGHT, MAX_HEIGHT);
    }

    /**
     * Returns true if the given position is ocean
     * (continental value below sea threshold).
     */
    public boolean isOcean(int x, int z) {
        return getContinentalValue(x, z) < -30;
    }

    /**
     * Returns true if the position is in a river valley
     * (low erosion, specific peaks and valleys range).
     */
    public boolean isRiver(int x, int z) {
        double pv = getPeaksAndValleysValue(x, z);
        double eros = getErosionValue(x, z);
        // Rivers form in valleys with moderate erosion
        return Math.abs(pv) < 5.0 && eros > 0.5 && !isOcean(x, z);
    }

    /**
     * Returns true if the position is mountainous
     * (high continental, low erosion, high peaks and valleys).
     */
    public boolean isMountain(int x, int z) {
        return getContinentalValue(x, z) > 40
                && getErosionValue(x, z) < 0.3
                && getPeaksAndValleysValue(x, z) > 20;
    }

    // ── Cave systems ──────────────────────────────────────────────────────────

    /**
     * Returns true if a cave exists at this 3D position.
     *
     * Uses two noise fields — caves only form where BOTH
     * are near zero (product near zero = cheese model).
     *
     * More generous near sea level, tighter deep underground.
     */
    public boolean isCave(int x, int y, int z) {
        if (y <= BEDROCK_HEIGHT) return false;
        if (y >= getSurfaceHeight(x, z) - 5) return false;

        double a = caveA.sample3D(x, y, z);
        double b = caveB.sample3D(x, y, z);

        // Cave threshold tightens with depth (deeper = fewer but larger caves)
        double depthFactor = 1.0 - ((double)(y - MIN_HEIGHT) / (SEA_LEVEL - MIN_HEIGHT));
        double threshold = 0.15 + depthFactor * 0.1;

        return Math.abs(a) < threshold && Math.abs(b) < threshold;
    }

    /**
     * Returns true if this is a large cave chamber
     * (both noise fields very close to zero — rarer).
     */
    public boolean isLargeChamber(int x, int y, int z) {
        if (y <= BEDROCK_HEIGHT || y >= SEA_LEVEL - 10) return false;

        double a = caveA.sample3D(x, y, z);
        double b = caveB.sample3D(x, y, z);

        return Math.abs(a) < 0.05 && Math.abs(b) < 0.05;
    }

    // ── Raw noise accessors ───────────────────────────────────────────────────

    /** Continental noise — range approx -120 to +120 */
    public double getContinentalValue(int x, int z) {
        return continental.sample(x, z);
    }

    /**
     * Erosion noise — remapped to 0.0 (no erosion) to 1.0 (fully eroded).
     * Raw noise is -1 to +1, we remap to 0-1.
     */
    public double getErosionValue(int x, int z) {
        double raw = erosion.sample(x, z); // -1 to +1
        return Mth.clamp((raw + 1.0) * 0.5, 0.0, 1.0);
    }

    /** Peaks and valleys noise — range approx -80 to +80 */
    public double getPeaksAndValleysValue(int x, int z) {
        return peaksAndValleys.sample(x, z);
    }

    /** Surface detail noise — range approx -8 to +8 */
    public double getSurfaceDetailValue(int x, int z) {
        return surfaceDetail.sample(x, z);
    }
}
