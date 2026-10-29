package net.enderwish.TerraForma_Subpack.core.biome;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

import java.util.List;

public class GHBiomes {

    // ── ARCTIC Zone ───────────────────────────────────────────────────────────
    public static final ResourceKey<Biome> FROZEN_TUNDRA         = key("frozen_tundra");
    public static final ResourceKey<Biome> GLACIAL_PEAKS         = key("glacial_peaks");
    public static final ResourceKey<Biome> POLAR_ICE_CAP         = key("polar_ice_cap");
    public static final ResourceKey<Biome> BOREAL_FOREST         = key("boreal_forest");

    // ── TEMPERATE Zone ────────────────────────────────────────────────────────
    public static final ResourceKey<Biome> TEMPERATE_FOREST      = key("temperate_forest");
    public static final ResourceKey<Biome> TEMPERATE_DECIDUOUS_FOREST = key("temperate_deciduous_forest");
    public static final ResourceKey<Biome> PINE_FOREST           = key("pine_forest");
    public static final ResourceKey<Biome> TEMPERATE_GRASSLAND   = key("temperate_grassland");
    public static final ResourceKey<Biome> CHERRY_GROVE          = key("cherry_grove");
    public static final ResourceKey<Biome> HIGHLAND_ORCHARD      = key("highland_orchard");
    public static final ResourceKey<Biome> ROLLING_MEADOWS       = key("rolling_meadows");
    public static final ResourceKey<Biome> BIRCH_HIGHLANDS       = key("birch_highlands");
    public static final ResourceKey<Biome> WETLANDS              = key("wetlands");
    public static final ResourceKey<Biome> SWAMP                 = key("swamp");

    // ── MEDITERRANEAN Zone ────────────────────────────────────────────────────
    public static final ResourceKey<Biome> MEDITERRANEAN_SCRUBLAND = key("mediterranean_scrubland");
    public static final ResourceKey<Biome> OLIVE_GROVE           = key("olive_grove");
    public static final ResourceKey<Biome> CYPRESS_FOREST        = key("cypress_forest");
    public static final ResourceKey<Biome> COASTAL_CLIFFS         = key("coastal_cliffs");
    public static final ResourceKey<Biome> SEA_CLIFFS            = key("sea_cliffs");
    public static final ResourceKey<Biome> LAVENDER_FIELDS       = key("lavender_fields");

    // ── TROPICAL Zone ─────────────────────────────────────────────────────────
    public static final ResourceKey<Biome> TROPICAL_RAINFOREST   = key("tropical_rainforest");
    public static final ResourceKey<Biome> JUNGLE_HILLS          = key("jungle_hills");
    public static final ResourceKey<Biome> MANGROVE_COAST        = key("mangrove_coast");
    public static final ResourceKey<Biome> PALM_OASIS            = key("palm_oasis");
    public static final ResourceKey<Biome> VOLCANIC_LOWLANDS     = key("volcanic_lowlands");
    public static final ResourceKey<Biome> VOLCANIC_HIGHLANDS    = key("volcanic_highlands");
    public static final ResourceKey<Biome> TROPICAL_SAVANNA      = key("tropical_savanna");

    // ── WASTELAND Zone ────────────────────────────────────────────────────────
    public static final ResourceKey<Biome> ASH_PLAINS            = key("ash_plains");
    public static final ResourceKey<Biome> SALT_FLATS            = key("salt_flats");
    public static final ResourceKey<Biome> CRACKED_BADLANDS      = key("cracked_badlands");
    public static final ResourceKey<Biome> RED_WASTES            = key("red_wastes");
    public static final ResourceKey<Biome> PETRIFIED_FOREST      = key("petrified_forest");
    public static final ResourceKey<Biome> OVERGROWN_RUINS       = key("overgrown_ruins");

    // ── SEAS Zone ─────────────────────────────────────────────────────────────
    public static final ResourceKey<Biome> FROZEN_SEA            = key("frozen_sea");
    public static final ResourceKey<Biome> TEMPERATE_SEA         = key("temperate_sea");
    public static final ResourceKey<Biome> TROPICAL_REEF_WATERS  = key("tropical_reef_waters");
    public static final ResourceKey<Biome> KELP_FOREST           = key("kelp_forest");
    public static final ResourceKey<Biome> ABYSSAL_PLAIN         = key("abyssal_plain");
    public static final ResourceKey<Biome> MANGROVE_SWAMP_COAST  = key("mangrove_swamp_coast");

    private static ResourceKey<Biome> key(String name) {
        return ResourceKey.create(Registries.BIOME,
                ResourceLocation.fromNamespaceAndPath("gh_terraforma", name));
    }

    public static List<ResourceKey<Biome>> getAll() {
        return List.of(
                // Arctic
                FROZEN_TUNDRA, GLACIAL_PEAKS, POLAR_ICE_CAP, BOREAL_FOREST,
                // Temperate
                TEMPERATE_FOREST, TEMPERATE_DECIDUOUS_FOREST, PINE_FOREST,
                TEMPERATE_GRASSLAND, CHERRY_GROVE, HIGHLAND_ORCHARD,
                ROLLING_MEADOWS, BIRCH_HIGHLANDS, WETLANDS, SWAMP,
                // Mediterranean
                MEDITERRANEAN_SCRUBLAND, OLIVE_GROVE, CYPRESS_FOREST,
                COASTAL_CLIFFS, SEA_CLIFFS, LAVENDER_FIELDS,
                // Tropical
                TROPICAL_RAINFOREST, JUNGLE_HILLS, MANGROVE_COAST,
                PALM_OASIS, VOLCANIC_LOWLANDS, VOLCANIC_HIGHLANDS,
                TROPICAL_SAVANNA,
                // Wasteland
                ASH_PLAINS, SALT_FLATS, CRACKED_BADLANDS, RED_WASTES,
                PETRIFIED_FOREST, OVERGROWN_RUINS,
                // Seas
                FROZEN_SEA, TEMPERATE_SEA, TROPICAL_REEF_WATERS,
                KELP_FOREST, ABYSSAL_PLAIN, MANGROVE_SWAMP_COAST
        );
    }
}
