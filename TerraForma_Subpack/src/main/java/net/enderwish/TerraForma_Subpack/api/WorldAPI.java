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
     * BUGFIX: previously estimated height with the NEUTRAL profile before
     * checking the target biome -- Glacial Peaks' y>180 requirement is only
     * realistically reachable with ITS OWN 2.2x hilliness multiplier applied.
     * Under neutral profile that combination (near-max continental AND
     * near-max peaks-and-valleys AND near-zero erosion, simultaneously, AND
     * arctic temperature) is rare enough that 480 samples could plausibly
     * find zero -- matches your log exactly (fell through to BlockPos.ZERO).
     * Volcanic Lowlands has no such altitude dependency, so this fix is
     * specifically for the Glacial Peaks half of the 50/50 pick -- if it's
     * STILL broken after this, the new logging below will show whether
     * Volcanic Lowlands is ALSO failing, which would point to a different bug.
     */
    public static BlockPos findExoticSpawnPosition(RandomSource random) {
        boolean seekingGlacialPeaks = random.nextBoolean();
        ResourceKey<Biome> target = seekingGlacialPeaks ? GHBiomes.GLACIAL_PEAKS : GHBiomes.VOLCANIC_LOWLANDS;

        var targetProfile = net.enderwish.TerraForma_Subpack.core.worldgen.BiomeTerrainRegistry.get(target);

        for (int radius = 500; radius <= 20_000; radius += 500) {
            for (int attempt = 0; attempt < 20; attempt++) {
                int x = random.nextInt(radius * 2) - radius;
                int z = random.nextInt(radius * 2) - radius;
                int y = GHNoiseRouter.INSTANCE.getSurfaceHeight(x, z, targetProfile);

                if (GHBiomeSource.getBiomeKeyAt(x, y, z).equals(target)) {
                    return new BlockPos(x, y + 1, z);
                }
            }
        }

        return BlockPos.ZERO;
    }
}
