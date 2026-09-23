package net.enderwish.TerraForma_Subpack.core.biome;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

import java.util.List;

public class GHBiomes {

    public static final ResourceKey<Biome> FROZEN_TUNDRA         = key("frozen_tundra");
    public static final ResourceKey<Biome> GLACIAL_PEAKS         = key("glacial_peaks");

    public static final ResourceKey<Biome> TEMPERATE_FOREST      = key("temperate_forest");
    public static final ResourceKey<Biome> HIGHLAND_ORCHARD      = key("highland_orchard");
    public static final ResourceKey<Biome> ROLLING_MEADOWS       = key("rolling_meadows");
    public static final ResourceKey<Biome> BIRCH_HIGHLANDS       = key("birch_highlands");
    public static final ResourceKey<Biome> WETLANDS              = key("wetlands");

    public static final ResourceKey<Biome> MEDITERRANEAN_SCRUBLAND = key("mediterranean_scrubland");
    public static final ResourceKey<Biome> COASTAL_CLIFFS         = key("coastal_cliffs");

    public static final ResourceKey<Biome> TROPICAL_RAINFOREST   = key("tropical_rainforest");
    public static final ResourceKey<Biome> MANGROVE_COAST        = key("mangrove_coast");
    public static final ResourceKey<Biome> VOLCANIC_LOWLANDS     = key("volcanic_lowlands");

    public static final ResourceKey<Biome> ASH_PLAINS            = key("ash_plains");
    public static final ResourceKey<Biome> CRACKED_BADLANDS      = key("cracked_badlands");
    public static final ResourceKey<Biome> OVERGROWN_RUINS       = key("overgrown_ruins");

    // ── Seas (NEW) ────────────────────────────────────────────────────────────
    public static final ResourceKey<Biome> FROZEN_SEA            = key("frozen_sea");
    public static final ResourceKey<Biome> TEMPERATE_SEA         = key("temperate_sea");
    public static final ResourceKey<Biome> TROPICAL_REEF_WATERS  = key("tropical_reef_waters");

    private static ResourceKey<Biome> key(String name) {
        return ResourceKey.create(Registries.BIOME,
                ResourceLocation.fromNamespaceAndPath("gh_terraforma", name));
    }

    public static List<ResourceKey<Biome>> getAll() {
        return List.of(
                FROZEN_TUNDRA, GLACIAL_PEAKS,
                TEMPERATE_FOREST, HIGHLAND_ORCHARD, ROLLING_MEADOWS, BIRCH_HIGHLANDS, WETLANDS,
                MEDITERRANEAN_SCRUBLAND, COASTAL_CLIFFS,
                TROPICAL_RAINFOREST, MANGROVE_COAST, VOLCANIC_LOWLANDS,
                ASH_PLAINS, CRACKED_BADLANDS, OVERGROWN_RUINS,
                FROZEN_SEA, TEMPERATE_SEA, TROPICAL_REEF_WATERS
        );
    }
}
