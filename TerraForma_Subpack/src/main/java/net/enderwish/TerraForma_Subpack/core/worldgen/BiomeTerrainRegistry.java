package net.enderwish.TerraForma_Subpack.core.worldgen;

import net.enderwish.TerraForma_Subpack.core.biome.GHBiomes;
import net.enderwish.TerraForma_Subpack.core.worldgen.noise.BiomeTerrainProfile;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

import java.util.HashMap;
import java.util.Map;

/**
 * BiomeTerrainRegistry — maps each biome to its terrain profile.
 *
 * Flat biomes (roughness < 0.3) stay flat.
 * Hilly biomes (hilliness > 1.0) get real terrain variation.
 * Mountain biomes (hilliness > 2.0) get dramatic elevation swings.
 */
public final class BiomeTerrainRegistry {

    private BiomeTerrainRegistry() {}

    private static final Map<ResourceKey<Biome>, BiomeTerrainProfile> PROFILES = new HashMap<>();

    static {
        // ── ARCTIC ─────────────────────────────────────────────────────────
        register(GHBiomes.FROZEN_TUNDRA,           0.05f, 0.2f,  0.25f);   // Flat tundra
        register(GHBiomes.GLACIAL_PEAKS,           2.0f,  2.5f, -0.35f);   // Jagged peaks
        register(GHBiomes.POLAR_ICE_CAP,           0.02f, 0.1f,  0.30f);   // Dead flat ice
        register(GHBiomes.BOREAL_FOREST,           0.9f,  1.2f,  0.0f);    // Rolling coniferous

        // ── TEMPERATE ────────────────────────────────────────────────────────
        register(GHBiomes.TEMPERATE_FOREST,        0.8f,  0.9f,  0.0f);    // Rolling forest
        register(GHBiomes.TEMPERATE_DECIDUOUS_FOREST, 1.1f, 1.3f, -0.05f); // Hilly forest
        register(GHBiomes.PINE_FOREST,              1.4f, 1.7f, -0.20f);   // Coniferous mountains
        register(GHBiomes.TEMPERATE_GRASSLAND,      0.10f,0.5f,  0.10f);   // Flat grassland
        register(GHBiomes.CHERRY_GROVE,             1.0f, 1.2f, -0.05f);   // Hilly orchard
        register(GHBiomes.HIGHLAND_ORCHARD,         1.0f, 1.1f, -0.05f);   // Rolling orchard
        register(GHBiomes.ROLLING_MEADOWS,          0.10f,0.5f,  0.05f);   // Flat meadows
        register(GHBiomes.BIRCH_HIGHLANDS,          1.3f, 1.5f, -0.15f);   // Birch hills
        register(GHBiomes.WETLANDS,                 0.05f,0.3f,  0.25f);   // Flat wetlands
        register(GHBiomes.SWAMP,                    0.03f,0.1f,  0.30f);   // Flat swamp

        // ── MEDITERRANEAN ───────────────────────────────────────────────────
        register(GHBiomes.MEDITERRANEAN_SCRUBLAND, 1.2f, 1.4f, -0.10f);   // Dry scrub
        register(GHBiomes.OLIVE_GROVE,              0.8f, 1.0f, -0.05f);   // Gentle hills
        register(GHBiomes.CYPRESS_FOREST,           1.0f, 1.3f, -0.10f);   // Hilly cypress
        register(GHBiomes.COASTAL_CLIFFS,           1.5f, 2.0f, -0.20f);   // Steep cliffs
        register(GHBiomes.SEA_CLIFFS,               1.8f, 2.2f, -0.25f);   // Extreme cliffs
        register(GHBiomes.LAVENDER_FIELDS,          0.15f,0.5f,  0.10f);   // Flat fields

        // ── TROPICAL ────────────────────────────────────────────────────────
        register(GHBiomes.TROPICAL_RAINFOREST,      0.9f,  1.0f,  0.0f);   // Jungle
        register(GHBiomes.JUNGLE_HILLS,             1.3f, 1.6f, -0.15f);   // Hilly jungle
        register(GHBiomes.MANGROVE_COAST,           0.05f,0.3f,  0.20f);   // Flat coastal
        register(GHBiomes.PALM_OASIS,               0.02f,0.1f,  0.30f);   // Flat oasis
        register(GHBiomes.VOLCANIC_LOWLANDS,        1.8f,  2.4f, -0.40f);   // Volcanic hills
        register(GHBiomes.VOLCANIC_HIGHLANDS,       2.0f,  2.8f, -0.50f);   // High volcanic
        register(GHBiomes.TROPICAL_SAVANNA,         0.4f,  0.7f,  0.05f);   // Flat savanna

        // ── WASTELAND ────────────────────────────────────────────────────────
        register(GHBiomes.ASH_PLAINS,               0.05f,0.5f,  0.15f);   // Flat ash
        register(GHBiomes.SALT_FLATS,               0.01f,0.05f, 0.35f);   // Dead flat
        register(GHBiomes.CRACKED_BADLANDS,         1.0f,  1.1f, -0.05f);   // Cracked earth
        register(GHBiomes.RED_WASTES,               1.2f,  1.3f, -0.10f);   // Hilly red
        register(GHBiomes.PETRIFIED_FOREST,         0.3f,  0.4f,  0.20f);   // Flat stone trees
        register(GHBiomes.OVERGROWN_RUINS,          0.9f,  1.0f,  0.0f);   // Ruins

        // ── SEAS (flat ocean floor) ──────────────────────────────────────────
        register(GHBiomes.FROZEN_SEA,               0.01f,0.05f, 0.25f);   // Flat
        register(GHBiomes.TEMPERATE_SEA,            0.02f,0.1f,  0.20f);   // Flat
        register(GHBiomes.TROPICAL_REEF_WATERS,     0.03f,0.1f,  0.15f);   // Flat
        register(GHBiomes.KELP_FOREST,              0.03f,0.1f,  0.20f);   // Flat
        register(GHBiomes.ABYSSAL_PLAIN,            0.01f,0.05f, 0.30f);   // Dead flat deep
        register(GHBiomes.MANGROVE_SWAMP_COAST,     0.02f,0.1f,  0.25f);   // Flat
    }

    private static void register(ResourceKey<Biome> key, float roughness, float hilliness, float erosionBias) {
        PROFILES.put(key, new BiomeTerrainProfile(roughness, hilliness, erosionBias));
    }

    public static BiomeTerrainProfile get(ResourceKey<Biome> biome) {
        return PROFILES.getOrDefault(biome, BiomeTerrainProfile.NEUTRAL);
    }
}
