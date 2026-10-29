package net.enderwish.TerraForma_Subpack.core.worldgen;

import net.enderwish.TerraForma_Subpack.core.biome.GHBiomes;
import net.enderwish.TerraForma_Subpack.core.worldgen.noise.SurfacePalette;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;

/**
 * SurfacePaletteRegistry
 *
 * Gives each biome its own surface/subsurface/deep blocks instead of
 * the old hardcoded grass/dirt/stone everywhere. Keyed the same way as
 * BiomeTerrainRegistry.
 */
public final class SurfacePaletteRegistry {

    private SurfacePaletteRegistry() {}

    private static final SurfacePalette DEFAULT = SurfacePalette.of(
            Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.DIRT.defaultBlockState(), 4, Blocks.STONE.defaultBlockState());

    private static final Map<ResourceKey<Biome>, SurfacePalette> PALETTES = new HashMap<>();

    static {
        // ── ARCTIC ─────────────────────────────────────────────────────────
        register(GHBiomes.FROZEN_TUNDRA, Blocks.SNOW_BLOCK, Blocks.DIRT, 4, Blocks.STONE);
        register(GHBiomes.GLACIAL_PEAKS, Blocks.SNOW_BLOCK, Blocks.PACKED_ICE, 3, Blocks.STONE);
        register(GHBiomes.POLAR_ICE_CAP, Blocks.SNOW_BLOCK, Blocks.ICE, 2, Blocks.STONE);
        register(GHBiomes.BOREAL_FOREST, Blocks.PODZOL, Blocks.DIRT, 4, Blocks.STONE);

        // ── TEMPERATE ────────────────────────────────────────────────────────
        register(GHBiomes.TEMPERATE_FOREST, Blocks.GRASS_BLOCK, Blocks.DIRT, 4, Blocks.STONE);
        register(GHBiomes.TEMPERATE_DECIDUOUS_FOREST, Blocks.GRASS_BLOCK, Blocks.COARSE_DIRT, 4, Blocks.STONE);
        register(GHBiomes.PINE_FOREST, Blocks.PODZOL, Blocks.DIRT, 4, Blocks.STONE);
        register(GHBiomes.TEMPERATE_GRASSLAND, Blocks.GRASS_BLOCK, Blocks.DIRT, 4, Blocks.STONE);
        register(GHBiomes.CHERRY_GROVE, Blocks.GRASS_BLOCK, Blocks.DIRT, 4, Blocks.STONE);
        register(GHBiomes.HIGHLAND_ORCHARD, Blocks.GRASS_BLOCK, Blocks.DIRT, 4, Blocks.STONE);
        register(GHBiomes.ROLLING_MEADOWS, Blocks.GRASS_BLOCK, Blocks.DIRT, 4, Blocks.STONE);
        register(GHBiomes.BIRCH_HIGHLANDS, Blocks.GRASS_BLOCK, Blocks.DIRT, 4, Blocks.STONE);
        register(GHBiomes.WETLANDS, Blocks.MUD, Blocks.MUD, 4, Blocks.STONE);
        register(GHBiomes.SWAMP, Blocks.MUD, Blocks.MUD, 5, Blocks.STONE);

        // ── MEDITERRANEAN ────────────────────────────────────────────────────
        register(GHBiomes.MEDITERRANEAN_SCRUBLAND, Blocks.COARSE_DIRT, Blocks.COARSE_DIRT, 3, Blocks.STONE);
        register(GHBiomes.OLIVE_GROVE, Blocks.COARSE_DIRT, Blocks.COARSE_DIRT, 3, Blocks.STONE);
        register(GHBiomes.CYPRESS_FOREST, Blocks.GRASS_BLOCK, Blocks.DIRT, 4, Blocks.STONE);
        register(GHBiomes.COASTAL_CLIFFS, Blocks.GRASS_BLOCK, Blocks.STONE, 1, Blocks.STONE);
        register(GHBiomes.SEA_CLIFFS, Blocks.STONE, Blocks.STONE, 2, Blocks.STONE);
        register(GHBiomes.LAVENDER_FIELDS, Blocks.GRASS_BLOCK, Blocks.DIRT, 3, Blocks.STONE);

        // ── TROPICAL ─────────────────────────────────────────────────────────
        register(GHBiomes.TROPICAL_RAINFOREST, Blocks.GRASS_BLOCK, Blocks.DIRT, 4, Blocks.STONE);
        register(GHBiomes.JUNGLE_HILLS, Blocks.GRASS_BLOCK, Blocks.DIRT, 4, Blocks.STONE);
        register(GHBiomes.MANGROVE_COAST, Blocks.MUD, Blocks.MUD, 4, Blocks.STONE);
        register(GHBiomes.PALM_OASIS, Blocks.SAND, Blocks.SANDSTONE, 4, Blocks.STONE);
        register(GHBiomes.VOLCANIC_LOWLANDS, Blocks.BLACKSTONE, Blocks.BASALT, 3, Blocks.STONE);
        register(GHBiomes.VOLCANIC_HIGHLANDS, Blocks.BLACKSTONE, Blocks.BASALT, 3, Blocks.STONE);
        register(GHBiomes.TROPICAL_SAVANNA, Blocks.SAND, Blocks.SANDSTONE, 4, Blocks.STONE);

        // ── WASTELAND ────────────────────────────────────────────────────────
        register(GHBiomes.ASH_PLAINS, Blocks.SAND, Blocks.SANDSTONE, 4, Blocks.STONE);
        register(GHBiomes.SALT_FLATS, Blocks.SAND, Blocks.SANDSTONE, 2, Blocks.STONE);
        register(GHBiomes.CRACKED_BADLANDS, Blocks.RED_SAND, Blocks.RED_SANDSTONE, 4, Blocks.STONE);
        register(GHBiomes.RED_WASTES, Blocks.RED_SAND, Blocks.RED_SANDSTONE, 4, Blocks.STONE);
        register(GHBiomes.PETRIFIED_FOREST, Blocks.STONE, Blocks.STONE, 3, Blocks.STONE);
        register(GHBiomes.OVERGROWN_RUINS, Blocks.PODZOL, Blocks.DIRT, 4, Blocks.STONE);

        // ── SEAS ─────────────────────────────────────────────────────────────
        register(GHBiomes.FROZEN_SEA, Blocks.ICE, Blocks.ICE, 2, Blocks.STONE);
        register(GHBiomes.TEMPERATE_SEA, Blocks.SAND, Blocks.SAND, 3, Blocks.STONE);
        register(GHBiomes.TROPICAL_REEF_WATERS, Blocks.SAND, Blocks.SAND, 2, Blocks.STONE);
        register(GHBiomes.KELP_FOREST, Blocks.SAND, Blocks.SAND, 2, Blocks.STONE);
        register(GHBiomes.ABYSSAL_PLAIN, Blocks.GRAVEL, Blocks.GRAVEL, 5, Blocks.STONE);
        register(GHBiomes.MANGROVE_SWAMP_COAST, Blocks.MUD, Blocks.MUD, 4, Blocks.STONE);
    }

    private static void register(ResourceKey<Biome> key, net.minecraft.world.level.block.Block top,
                                  net.minecraft.world.level.block.Block sub, int subDepth,
                                  net.minecraft.world.level.block.Block deep) {
        PALETTES.put(key, SurfacePalette.of(top.defaultBlockState(), sub.defaultBlockState(), subDepth, deep.defaultBlockState()));
    }

    public static SurfacePalette get(ResourceKey<Biome> biome) {
        return PALETTES.getOrDefault(biome, DEFAULT);
    }
}
