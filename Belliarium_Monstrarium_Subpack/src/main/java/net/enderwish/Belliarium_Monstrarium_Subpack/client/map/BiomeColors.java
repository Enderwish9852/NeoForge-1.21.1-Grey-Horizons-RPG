package net.enderwish.Belliarium_Monstrarium_Subpack.client.map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

/**
 * BiomeColors
 *
 * Xaero-style biome color palette: each biome family gets a flat color,
 * so the map reads as a top-down terrain view without per-block noise.
 *
 * Fluids are drawn as their own colors when they sit on top of the surface.
 */
public class BiomeColors {

    // ── Water / fluids ────────────────────────────────────────────────────

    public static final int WATER = 0xFF3B82F6;     // blue
    public static final int LAVA = 0xFFF97316;      // orange-red
    public static final int SWAMP_WATER = 0xFF2F6B3A; // dark green-brown

    // ── Land biomes ───────────────────────────────────────────────────────

    public static final int OCEAN = 0xFF1E6091;     // deep ocean blue
    public static final int PLAINS = 0xFF8BC34A;    // light green
    public static final int FOREST = 0xFF4CAF50;    // green
    public static final int DESERT = 0xFFD4A373;    // tan
    public static final int MOUNTAIN = 0xFF9E9E9E;  // gray
    public static final int SNOW = 0xFFECEFF1;      // near-white
    public static final int JUNGLE = 0xFF2E7D32;    // dark green
    public static final int SWAMP = 0xFF6B8E23;     // olive
    public static final int TAIGA = 0xFF556B2F;     // dark olive
    public static final int SAVANNA = 0xFFC5A059;   // light tan
    public static final int MUSHROOM = 0xFF9C27B0;  // purple
    public static final int BADLANDS = 0xFF8D6E63;  // brown
    public static final int BEACH = 0xFFE8D3A5;     // sand
    public static final int TUNDRA = 0xFFCFD8DC;    // pale gray-blue
    public static final int DEFAULT = 0xFF7C9E4C;   // fallback green

    /** Look up the Xaero-style color for a biome. Uses base temperature and water color. */
    public static int colorFor(Holder<Biome> biome) {
        if (biome == null) return DEFAULT;

        float temp = biome.value().getBaseTemperature();

        // Very cold -> snow
        if (temp < 0.15f) return SNOW;
        // Cold -> taiga/tundra
        if (temp < 0.5f) return TAIGA;
        // Temperate -> plains/forest
        if (temp < 0.8f) return FOREST;
        // Warm -> desert/savanna
        if (temp < 1.2f) return DESERT;
        // Hot -> badlands
        return BADLANDS;
    }

    /** Sample the top-most solid (non-air) block height at a position. */
    public static int topSolidHeight(Level level, int x, int z) {
        return level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
    }

    /** Sample the fluid color at a position, or 0 if none. */
    public static int fluidColorAt(Level level, int x, int y, int z) {
        FluidState fluidState = level.getFluidState(new BlockPos(x, y, z));
        if (fluidState.isEmpty()) return 0;

        var fluidType = fluidState.getType();
        if (fluidType == Fluids.WATER) {
            Holder<Biome> biome = level.getBiome(new BlockPos(x, y, z));
            float temp = biome.value().getBaseTemperature();
            if (temp < 0.5f) return SWAMP_WATER;
            return WATER;
        }
        if (fluidType == Fluids.LAVA) return LAVA;
        return 0;
    }

    private BiomeColors() {}
}
