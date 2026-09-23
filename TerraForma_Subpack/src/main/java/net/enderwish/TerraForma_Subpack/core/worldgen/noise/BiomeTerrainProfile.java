package net.enderwish.TerraForma_Subpack.core.worldgen.noise;

/**
 * BiomeTerrainProfile
 *
 *   roughness   -- multiplies surfaceDetail (small-scale bumpiness).
 *   hilliness   -- multiplies peaksAndValleys (elevation swings).
 *   erosionBias -- shifts effective erosion; positive flattens further,
 *                  negative sharpens (less eroded, more raw amplitude).
 *
 * NEUTRAL matches the original pre-profile formula exactly.
 */
public record BiomeTerrainProfile(float roughness, float hilliness, float erosionBias) {
    public static final BiomeTerrainProfile NEUTRAL = new BiomeTerrainProfile(1.0f, 1.0f, 0.0f);
}
