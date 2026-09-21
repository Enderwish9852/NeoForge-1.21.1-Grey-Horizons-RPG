package net.enderwish.TerraForma_Subpack.core.biome;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.enderwish.TerraForma_Subpack.core.climate.ClimateMap;
import net.enderwish.TerraForma_Subpack.core.climate.ClimateZone;
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
 * Maps world position -> biome using ClimateMap's zone query.
 * First working version picks ONE representative biome per
 * ClimateZone (5 zones -> 5 of the 15 biomes) — not yet using
 * moisture to pick between e.g. WETLANDS vs ROLLING_MEADOWS within
 * TEMPERATE the way ClimateMap.isTemperateDry/isTemperateWet already
 * supports. That's the natural next pass once this compiles.
 *
 * Only WorldAPI (in this same subpack) is meant to call create() or
 * construct this directly — other subpacks go through WorldAPI.
 */
public class GHBiomeSource extends BiomeSource {

    public static final MapCodec<GHBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Biome.CODEC.listOf().fieldOf("biomes").forGetter(GHBiomeSource::getBiomeHolders)
            ).apply(instance, GHBiomeSource::new)
    );

    private final List<Holder<Biome>> biomeHolders;

    public GHBiomeSource(List<Holder<Biome>> biomeHolders) {
        this.biomeHolders = biomeHolders;
    }

    /**
     * Resolves GHBiomes' ResourceKeys into real Holder<Biome> instances
     * via the given RegistryAccess, and builds a GHBiomeSource from
     * them. Pattern confirmed directly from WorldDimensions.java itself
     * (registryAccess.registryOrThrow + getHolderOrThrow).
     */
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
        int blockY = QuartPos.toBlock(quartY);
        int blockZ = QuartPos.toBlock(quartZ);

        ClimateZone zone = ClimateMap.INSTANCE.getZone(blockX, blockY, blockZ);
        return findHolder(zoneToRepresentativeBiome(zone));
    }

    private ResourceKey<Biome> zoneToRepresentativeBiome(ClimateZone zone) {
        return switch (zone) {
            case ARCTIC        -> GHBiomes.FROZEN_TUNDRA;
            case TEMPERATE     -> GHBiomes.TEMPERATE_FOREST;
            case MEDITERRANEAN -> GHBiomes.MEDITERRANEAN_SCRUBLAND;
            case TROPICAL      -> GHBiomes.TROPICAL_RAINFOREST;
            case WASTELAND     -> GHBiomes.ASH_PLAINS;
        };
    }

    private Holder<Biome> findHolder(ResourceKey<Biome> key) {
        for (Holder<Biome> holder : biomeHolders) {
            if (holder.unwrapKey().map(k -> k.equals(key)).orElse(false)) {
                return holder;
            }
        }
        return biomeHolders.get(0); // should never happen if the list was built correctly
    }
}
