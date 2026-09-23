package net.enderwish.TerraForma_Subpack.api;

import net.enderwish.TerraForma_Subpack.core.biome.GHBiomeSource;
import net.enderwish.TerraForma_Subpack.core.biome.GHBiomes;
import net.enderwish.TerraForma_Subpack.core.climate.ClimateMap;
import net.enderwish.TerraForma_Subpack.core.climate.ClimateZone;
import net.enderwish.TerraForma_Subpack.core.worldgen.GHChunkGenerator;
import net.enderwish.TerraForma_Subpack.core.worldgen.noise.GHNoiseRouter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

/**
 * WorldAPI
 *
 * The ONLY class other subpacks should import from TerraForma.
 *
 * NEW -- findExoticSpawnPosition(RandomSource): 50/50 between Volcanic
 * Lowlands and Glacial Peaks, searches outward from the origin for a
 * real match. This only FINDS the position -- actually calling
 * ServerLevel.setDefaultSpawnPos(BlockPos, float) with it, at the
 * right moment during fresh-world creation, is the piece still open
 * (see this reply's question).
 */
public final class WorldAPI {

    private WorldAPI() {}

    public static ClimateZone getClimateZone(ServerLevel level, BlockPos pos) {
        return ClimateMap.INSTANCE.getZone(pos);
    }

    public static boolean isArctic(ServerLevel level, BlockPos pos) {
        return getClimateZone(level, pos) == ClimateZone.ARCTIC;
    }

    public static boolean isTemperate(ServerLevel level, BlockPos pos) {
        return getClimateZone(level, pos) == ClimateZone.TEMPERATE;
    }

    public static boolean isMediterranean(ServerLevel level, BlockPos pos) {
        return getClimateZone(level, pos) == ClimateZone.MEDITERRANEAN;
    }

    public static boolean isTropical(ServerLevel level, BlockPos pos) {
        return getClimateZone(level, pos) == ClimateZone.TROPICAL;
    }

    public static boolean isWasteland(ServerLevel level, BlockPos pos) {
        return getClimateZone(level, pos) == ClimateZone.WASTELAND;
    }

    public static float getClimateTemperature(ServerLevel level, BlockPos pos) {
        return ClimateMap.INSTANCE.getTemperature(pos);
    }

    public static float getMoisture(ServerLevel level, BlockPos pos) {
        return ClimateMap.INSTANCE.getMoisture(pos);
    }

    public static boolean isFarmingPossible(ServerLevel level, BlockPos pos) {
        return getClimateZone(level, pos).farmingPossible;
    }

    public static float getWastelandIntensity(ServerLevel level, BlockPos pos) {
        return ClimateMap.INSTANCE.getWastelandNoise(pos.getX(), pos.getZ());
    }

    public static WorldDimensions createWorldDimensions(RegistryAccess registryAccess) {
        GHBiomeSource biomeSource = GHBiomeSource.create(registryAccess);
        GHChunkGenerator chunkGenerator = new GHChunkGenerator(biomeSource);
        return WorldPresets.createNormalWorldDimensions(registryAccess)
                .replaceOverworldGenerator(registryAccess, chunkGenerator);
    }

    /**
     * Picks 50/50 between Volcanic Lowlands and Glacial Peaks and
     * searches outward from the origin in growing rings for a real
     * position in that biome. Both should be common enough to resolve
     * well within the search cap below.
     *
     * TIMING CAVEAT: GHNoiseRouter/ClimateMap must already be seeded
     * with the real world seed (via GHChunkGenerator.createState) for
     * this to match what the actual generated world has at that spot.
     * If this runs before that seeding happens, it'll search against
     * the fallback seed instead and could hand back a position the
     * real world doesn't actually have as Volcanic/Glacial. See this
     * reply's question about exactly when to call this.
     */
    public static BlockPos findExoticSpawnPosition(RandomSource random) {
        ResourceKey<Biome> target = random.nextBoolean()
                ? GHBiomes.VOLCANIC_LOWLANDS
                : GHBiomes.GLACIAL_PEAKS;

        for (int radius = 500; radius <= 20_000; radius += 500) {
            for (int attempt = 0; attempt < 12; attempt++) {
                int x = random.nextInt(radius * 2) - radius;
                int z = random.nextInt(radius * 2) - radius;
                int roughY = GHNoiseRouter.INSTANCE.getSurfaceHeight(x, z);

                if (GHBiomeSource.getBiomeKeyAt(x, roughY, z).equals(target)) {
                    int y = GHNoiseRouter.INSTANCE.getSurfaceHeight(x, z);
                    return new BlockPos(x, y + 1, z);
                }
            }
        }

        return BlockPos.ZERO; // shouldn't happen -- both biomes exist well within 20,000 blocks
    }
}
