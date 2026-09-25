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
     * position in that biome.
     *
     * Called from Frontier_Subpack's ExoticSpawnHandler, itself
     * triggered by LevelEvent.CreateSpawnPosition -- which fires after
     * GHChunkGenerator.createState(...) has already seeded both
     * ClimateMap.INSTANCE and GHNoiseRouter.INSTANCE with the real
     * world seed, so the old "must already be seeded" timing worry
     * from a few rounds back is resolved: by the time anything can be
     * asking for a spawn position, the chunk generator handling that
     * request necessarily already exists and has been through
     * createState.
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

        return BlockPos.ZERO;
    }
}
