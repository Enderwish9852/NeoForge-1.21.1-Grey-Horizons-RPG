package net.enderwish.TerraForma_Subpack.api;

import net.enderwish.TerraForma_Subpack.core.biome.GHBiomeSource;
import net.enderwish.TerraForma_Subpack.core.climate.ClimateMap;
import net.enderwish.TerraForma_Subpack.core.climate.ClimateZone;
import net.enderwish.TerraForma_Subpack.core.worldgen.GHChunkGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;


/**
 * WorldAPI
 *
 * The ONLY class other subpacks should import from TerraForma.
 * A static facade over the world generation internals.
 *
 * Other subpacks never import ClimateMap, GHBiomeSource,
 * GHChunkGenerator, or GHBiomes directly.
 *
 * Usage example (from Farming subpack):
 *   import net.enderwish.TerraForma_Subpack.api.WorldAPI;
 *
 *   ClimateZone zone = WorldAPI.getClimateZone(level, pos);
 *   if (WorldAPI.isTropical(level, pos)) { ... }
 *
 * Usage example (from Frontier subpack, world creation):
 *   registryAccess -> WorldAPI.createWorldDimensions(registryAccess)
 */
public final class WorldAPI {

    private WorldAPI() {}

    // ── Climate zone queries ──────────────────────────────────────────────────

    /**
     * Returns the ClimateZone at the given position.
     * Drives biome selection, crop viability, and weather patterns.
     */
    public static ClimateZone getClimateZone(ServerLevel level, BlockPos pos) {
        return ClimateMap.INSTANCE.getZone(pos);
    }

    /** Returns true if the position is in the Arctic climate zone. */
    public static boolean isArctic(ServerLevel level, BlockPos pos) {
        return getClimateZone(level, pos) == ClimateZone.ARCTIC;
    }

    /** Returns true if the position is in the Temperate climate zone. */
    public static boolean isTemperate(ServerLevel level, BlockPos pos) {
        return getClimateZone(level, pos) == ClimateZone.TEMPERATE;
    }

    /** Returns true if the position is in the Mediterranean climate zone. */
    public static boolean isMediterranean(ServerLevel level, BlockPos pos) {
        return getClimateZone(level, pos) == ClimateZone.MEDITERRANEAN;
    }

    /** Returns true if the position is in the Tropical climate zone. */
    public static boolean isTropical(ServerLevel level, BlockPos pos) {
        return getClimateZone(level, pos) == ClimateZone.TROPICAL;
    }

    /** Returns true if the position is in the Wasteland climate zone. */
    public static boolean isWasteland(ServerLevel level, BlockPos pos) {
        return getClimateZone(level, pos) == ClimateZone.WASTELAND;
    }

    // ── Temperature + moisture ────────────────────────────────────────────────

    /**
     * Returns the raw climate temperature at a position.
     * Range: -1.0 (arctic) to +1.0 (tropical).
     * Note: this is CLIMATE temperature, not the "feels like" temperature
     * from Atmospheric. Combine them for full player body temperature.
     */
    public static float getClimateTemperature(ServerLevel level, BlockPos pos) {
        return ClimateMap.INSTANCE.getTemperature(pos);
    }

    /**
     * Returns the moisture level at a position.
     * Range: 0.0 (arid) to 1.0 (saturated).
     * Drives biome selection within climate zones.
     */
    public static float getMoisture(ServerLevel level, BlockPos pos) {
        return ClimateMap.INSTANCE.getMoisture(pos);
    }

    // ── Farming viability ─────────────────────────────────────────────────────

    /**
     * Returns true if farming is possible at this position.
     * Arctic and Wasteland zones cannot support crops.
     */
    public static boolean isFarmingPossible(ServerLevel level, BlockPos pos) {
        return getClimateZone(level, pos).farmingPossible;
    }

    // ── Wasteland ─────────────────────────────────────────────────────────────

    /**
     * Returns the raw wasteland noise value at a position.
     * Use for gradual wasteland effects (more corrupted near epicentre).
     */
    public static float getWastelandIntensity(ServerLevel level, BlockPos pos) {
        return ClimateMap.INSTANCE.getWastelandNoise(pos.getX(), pos.getZ());
    }

    // ── World generation ──────────────────────────────────────────────────────

    /**
     * Builds a WorldDimensions using GHChunkGenerator/GHBiomeSource for
     * the overworld, leaving vanilla's nether/end entries untouched.
     * This is the ONLY thing another subpack should call to get a world
     * using TerraForma's custom generation — GHChunkGenerator and
     * GHBiomeSource are never imported outside this file.
     *
     * Returns a real vanilla WorldDimensions — fine to expose directly,
     * it's not a TerraForma-internal type.
     */
    public static WorldDimensions createWorldDimensions(RegistryAccess registryAccess) {
        GHBiomeSource biomeSource = GHBiomeSource.create(registryAccess);
        GHChunkGenerator chunkGenerator = new GHChunkGenerator(biomeSource);
        return WorldPresets.createNormalWorldDimensions(registryAccess)
                .replaceOverworldGenerator(registryAccess, chunkGenerator);
    }
}
