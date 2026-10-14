package net.enderwish.TerraForma_Subpack.core.worldgen;

import net.enderwish.TerraForma_Subpack.core.biome.GHBiomes;
import net.enderwish.TerraForma_Subpack.core.worldgen.noise.SurfacePalette;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;

import java.util.HashMap;
import java.util.Map;

/**
 * SurfacePaletteRegistry
 *
 * Gives each land biome its own surface/subsurface/deep blocks instead of
 * the old hardcoded grass/dirt/stone everywhere. Keyed the same way as
 * BiomeTerrainRegistry.
 */
public final class SurfacePaletteRegistry {

    private SurfacePaletteRegistry() {}

    private static final SurfacePalette DEFAULT = SurfacePalette.of(
            Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.DIRT.defaultBlockState(), 4, Blocks.STONE.defaultBlockState());

    private static final Map<ResourceKey<Biome>, SurfacePalette> PALETTES = new HashMap<>();

    static {
        // Arctic
        register(GHBiomes.FROZEN_TUNDRA, Blocks.SNOW_BLOCK, Blocks.DIRT, 4, Blocks.STONE);
        register(GHBiomes.GLACIAL_PEAKS, Blocks.SNOW_BLOCK, Blocks.PACKED_ICE, 3, Blocks.STONE);

        // Temperate -- grass/dirt/stone reads fine for all of these, left as DEFAULT.
        register(GHBiomes.TEMPERATE_FOREST, Blocks.GRASS_BLOCK, Blocks.DIRT, 4, Blocks.STONE);
        register(GHBiomes.HIGHLAND_ORCHARD, Blocks.GRASS_BLOCK, Blocks.DIRT, 4, Blocks.STONE);
        register(GHBiomes.ROLLING_MEADOWS, Blocks.GRASS_BLOCK, Blocks.DIRT, 4, Blocks.STONE);
        register(GHBiomes.BIRCH_HIGHLANDS, Blocks.GRASS_BLOCK, Blocks.DIRT, 4, Blocks.STONE);
        register(GHBiomes.WETLANDS, Blocks.MUD, Blocks.MUD, 4, Blocks.STONE);

        // Mediterranean
        register(GHBiomes.MEDITERRANEAN_SCRUBLAND, Blocks.COARSE_DIRT, Blocks.COARSE_DIRT, 3, Blocks.STONE);
        register(GHBiomes.COASTAL_CLIFFS, Blocks.GRASS_BLOCK, Blocks.STONE, 1, Blocks.STONE);

        // Tropical
        register(GHBiomes.TROPICAL_RAINFOREST, Blocks.GRASS_BLOCK, Blocks.DIRT, 4, Blocks.STONE);
        register(GHBiomes.MANGROVE_COAST, Blocks.MUD, Blocks.MUD, 4, Blocks.STONE);
        register(GHBiomes.VOLCANIC_LOWLANDS, Blocks.BLACKSTONE, Blocks.BASALT, 3, Blocks.STONE);

        // Wasteland
        register(GHBiomes.ASH_PLAINS, Blocks.SAND, Blocks.SANDSTONE, 4, Blocks.STONE);
        register(GHBiomes.CRACKED_BADLANDS, Blocks.RED_SAND, Blocks.RED_SANDSTONE, 4, Blocks.STONE);
        register(GHBiomes.OVERGROWN_RUINS, Blocks.PODZOL, Blocks.DIRT, 4, Blocks.STONE);
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
