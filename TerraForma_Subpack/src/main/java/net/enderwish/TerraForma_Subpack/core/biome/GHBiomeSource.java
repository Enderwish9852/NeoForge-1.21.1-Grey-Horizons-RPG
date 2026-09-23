package net.enderwish.TerraForma_Subpack.core.biome;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.enderwish.TerraForma_Subpack.core.climate.ClimateMap;
import net.enderwish.TerraForma_Subpack.core.climate.ClimateSettings;
import net.enderwish.TerraForma_Subpack.core.climate.ClimateZone;
import net.enderwish.TerraForma_Subpack.core.worldgen.noise.GHNoiseRouter;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * GHBiomeSource
 *
 * BUGFIX this round: getNoiseBiome() previously converted vanilla's
 * incoming quartY straight into a blockY and fed it into altitude
 * checks (ALPINE_HEIGHT, ClimateMap's own altitude-cooling term).
 * Vanilla samples biomes at MANY quartY layers per column, all the
 * way up through open sky and down underground, for its own internal
 * 3D biome-storage grid -- none of that reflects real terrain height.
 * Confirmed directly from your screenshots: same X/Z, biome flipped
 * from temperate_forest -> highland_orchard -> frozen_tundra purely
 * from flying upward through empty air, while the real generated
 * surface stayed at Y=83 the whole time.
 *
 * FIX: getNoiseBiome now derives the column's REAL surface height via
 * GHNoiseRouter.INSTANCE.getSurfaceHeight(x, z) itself and uses THAT
 * for every altitude decision, ignoring the incoming quartY entirely.
 * Biome is now a pure function of (x, z) -- the same column always
 * resolves to the same biome no matter what Y vanilla (or the player)
 * happens to be looking at. getBiomeKeyAt's own signature is
 * unchanged since its other two callers (GHChunkGenerator.profileAt,
 * WorldAPI.findExoticSpawnPosition) already pass a sensible height.
 *
 * Ocean placement and extreme-biome margin buffers unchanged from
 * last round.
 */
public class GHBiomeSource extends BiomeSource {

    private static final int GLACIAL_PEAKS_ALTITUDE_MARGIN = 20;
    private static final float VOLCANIC_DRYNESS_MARGIN = 0.10f;
    private static final float CRACKED_BADLANDS_MIN_TEMP = 0.45f;

    public static final MapCodec<GHBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Biome.CODEC.listOf().fieldOf("biomes").forGetter(GHBiomeSource::getBiomeHolders)
            ).apply(instance, GHBiomeSource::new)
    );

    private final List<Holder<Biome>> biomeHolders;

    public GHBiomeSource(List<Holder<Biome>> biomeHolders) {
        this.biomeHolders = biomeHolders;
    }

    public static GHBiomeSource create(RegistryAccess registryAccess) {
        Registry<Biome> biomeRegistry = registryAccess.registryOrThrow(Registries.BIOME);
        List<Holder<Biome>> holders = GHBiomes.getAll().stream()
                .map(biomeRegistry::getHolderOrThrow)
                .collect(Collectors.toList());
        return new GHBiomeSource(holders);
    }

    private List<Holder<Biome>> getBiomeHolders() {
        return biomeHolders;
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return biomeHolders.stream();
    }

    @Override
    public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler) {
        int blockX = QuartPos.toBlock(quartX);
        int blockZ = QuartPos.toBlock(quartZ);
        int surfaceY = GHNoiseRouter.INSTANCE.getSurfaceHeight(blockX, blockZ);
        return findHolder(getBiomeKeyAt(blockX, surfaceY, blockZ));
    }

    public static ResourceKey<Biome> getBiomeKeyAt(int x, int y, int z) {
        if (GHNoiseRouter.INSTANCE.isOcean(x, z)) {
            return selectSeaBiome(x, y, z);
        }
        ClimateZone zone = ClimateMap.INSTANCE.getZone(x, y, z);
        return selectBiome(zone, x, y, z);
    }

    private static ResourceKey<Biome> selectSeaBiome(int x, int y, int z) {
        float rawTemp = ClimateMap.INSTANCE.getTemperature(x, y, z);
        if (rawTemp < ClimateSettings.ARCTIC_THRESHOLD) return GHBiomes.FROZEN_SEA;
        if (rawTemp > ClimateSettings.MEDITERRANEAN_THRESHOLD) return GHBiomes.TROPICAL_REEF_WATERS;
        return GHBiomes.TEMPERATE_SEA;
    }

    private static ResourceKey<Biome> selectBiome(ClimateZone zone, int x, int y, int z) {
        float moisture = ClimateMap.INSTANCE.getMoisture(x, z);

        return switch (zone) {
            case ARCTIC -> y > ClimateSettings.ALPINE_HEIGHT + GLACIAL_PEAKS_ALTITUDE_MARGIN
                    ? GHBiomes.GLACIAL_PEAKS
                    : GHBiomes.FROZEN_TUNDRA;

            case TEMPERATE -> {
                if (moisture > ClimateSettings.WET_THRESHOLD) yield GHBiomes.WETLANDS;
                if (moisture < ClimateSettings.DRY_THRESHOLD) yield GHBiomes.ROLLING_MEADOWS;
                if (y > ClimateSettings.ALPINE_HEIGHT) yield GHBiomes.BIRCH_HIGHLANDS;
                if (y > ClimateSettings.SEA_LEVEL + 30) yield GHBiomes.HIGHLAND_ORCHARD;
                yield GHBiomes.TEMPERATE_FOREST;
            }

            case MEDITERRANEAN -> moisture > 0.5f
                    ? GHBiomes.COASTAL_CLIFFS
                    : GHBiomes.MEDITERRANEAN_SCRUBLAND;

            case TROPICAL -> {
                if (moisture < ClimateSettings.DRY_THRESHOLD - VOLCANIC_DRYNESS_MARGIN) yield GHBiomes.VOLCANIC_LOWLANDS;
                if (moisture > ClimateSettings.WET_THRESHOLD) yield GHBiomes.TROPICAL_RAINFOREST;
                yield GHBiomes.MANGROVE_COAST;
            }

            case WASTELAND -> {
                if (moisture > ClimateSettings.WET_THRESHOLD) yield GHBiomes.OVERGROWN_RUINS;
                float rawTemp = ClimateMap.INSTANCE.getTemperature(x, y, z);
                yield rawTemp > CRACKED_BADLANDS_MIN_TEMP ? GHBiomes.CRACKED_BADLANDS : GHBiomes.ASH_PLAINS;
            }
        };
    }

    private Holder<Biome> findHolder(ResourceKey<Biome> key) {
        for (Holder<Biome> holder : biomeHolders) {
            if (holder.unwrapKey().map(k -> k.equals(key)).orElse(false)) {
                return holder;
            }
        }
        return biomeHolders.get(0);
    }
}
