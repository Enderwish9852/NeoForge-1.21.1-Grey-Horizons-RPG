package net.enderwish.TerraForma_Subpack.core.worldgen;

import net.enderwish.TerraForma_Subpack.core.biome.GHBiomes;
import net.enderwish.TerraForma_Subpack.core.worldgen.noise.BiomeTerrainProfile;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

import java.util.HashMap;
import java.util.Map;

/**
 * BiomeTerrainRegistry
 *
 * RETUNED this round: roughness lowered for the biomes meant to read
 * as genuinely flat (Frozen Tundra, Rolling Meadows, Wetlands,
 * Mangrove Coast, Ash Plains). At the old values, surfaceDetail's raw
 * +-8 block amplitude times even a 0.3-0.5 multiplier still left
 * 2-4 blocks of small-scale bump/pit noise -- on a "flat" biome that's
 * enough to expose the dirt layer under grass in a jittery checkerboard
 * pattern, which is exactly what showed up in your Ash Plains
 * screenshots. Hilly/mountainous biomes untouched.
 */
public final class BiomeTerrainRegistry {

    private BiomeTerrainRegistry() {}

    private static final Map<ResourceKey<Biome>, BiomeTerrainProfile> PROFILES = new HashMap<>();

    static {
        register(GHBiomes.FROZEN_TUNDRA,           0.15f, 0.4f,  0.15f);
        register(GHBiomes.GLACIAL_PEAKS,           1.6f,  2.2f, -0.35f);
        register(GHBiomes.TEMPERATE_FOREST,        0.8f,  0.9f,  0.0f);
        register(GHBiomes.HIGHLAND_ORCHARD,        1.0f,  1.1f, -0.05f);
        register(GHBiomes.ROLLING_MEADOWS,         0.25f, 0.8f,  0.05f);
        register(GHBiomes.BIRCH_HIGHLANDS,         1.3f,  1.5f, -0.15f);
        register(GHBiomes.WETLANDS,                0.08f, 0.3f,  0.25f);
        register(GHBiomes.MEDITERRANEAN_SCRUBLAND, 1.2f,  1.4f, -0.10f);
        register(GHBiomes.COASTAL_CLIFFS,          1.1f,  1.6f, -0.10f);
        register(GHBiomes.TROPICAL_RAINFOREST,     0.9f,  1.0f,  0.0f);
        register(GHBiomes.MANGROVE_COAST,          0.10f, 0.3f,  0.20f);
        register(GHBiomes.VOLCANIC_LOWLANDS,       1.8f,  2.4f, -0.40f);
        register(GHBiomes.ASH_PLAINS,              0.15f, 0.5f,  0.15f);
        register(GHBiomes.CRACKED_BADLANDS,        1.0f,  1.1f, -0.05f);
        register(GHBiomes.OVERGROWN_RUINS,         0.9f,  1.0f,  0.0f);
    }

    private static void register(ResourceKey<Biome> key, float roughness, float hilliness, float erosionBias) {
        PROFILES.put(key, new BiomeTerrainProfile(roughness, hilliness, erosionBias));
    }

    public static BiomeTerrainProfile get(ResourceKey<Biome> biome) {
        return PROFILES.getOrDefault(biome, BiomeTerrainProfile.NEUTRAL);
    }
}
