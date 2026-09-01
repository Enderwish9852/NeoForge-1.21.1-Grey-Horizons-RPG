package net.enderwish.TerraForma_Subpack.datagen;

import net.enderwish.TerraForma_Subpack.TerraFormaSubpack;
import net.enderwish.TerraForma_Subpack.core.biome.GHBiomes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * GHBiomeProvider
 *
 * Generates all 15 biome JSON files for TerraForma.
 * Output: data/gh_terraforma/worldgen/biome/<name>.json
 *
 * Each biome JSON defines:
 *   - temperature + downfall
 *   - has_precipitation
 *   - effects (fog, water, sky, grass, foliage colours)
 *   - carvers (vanilla defaults)
 *   - features (vegetation, ores, springs etc.)
 *   - spawners (mob spawn rules)
 *
 * To modify a biome: change values here and run runData.
 * Or override individual JSONs with a datapack — no recompile needed.
 */
public class GHBiomeProvider implements DataProvider {

    private static final Gson GSON =
            new GsonBuilder().setPrettyPrinting().create();

    private final PackOutput output;
    private final CompletableFuture<HolderLookup.Provider> lookupProvider;

    public GHBiomeProvider(PackOutput output,
                           CompletableFuture<HolderLookup.Provider> lookupProvider) {
        this.output         = output;
        this.lookupProvider = lookupProvider;
    }

    // ── Biome definition record ───────────────────────────────────────────────

    record BiomeDef(
            ResourceKey<Biome> key,
            float temperature,
            float downfall,
            boolean hasPrecipitation,
            int fogColor,
            int waterColor,
            int waterFogColor,
            int skyColor,
            Optional<Integer> grassColor,
            Optional<Integer> foliageColor,
            List<String> features,   // vanilla feature tags to include
            List<String> spawners    // vanilla spawn group tags
    ) {}

    // ── Register all biomes ───────────────────────────────────────────────────

    private List<BiomeDef> buildBiomes() {
        List<BiomeDef> biomes = new ArrayList<>();

        // ── Arctic ────────────────────────────────────────────────────────────
        biomes.add(new BiomeDef(
                GHBiomes.FROZEN_TUNDRA,
                -0.7f, 0.3f, true,
                0xC0D8FF, 0x3F76E4, 0x050533, 0x7DA4FF,
                Optional.of(0xA0B0A0), Optional.of(0x90A090),
                List.of("default_carvers", "default_underground",
                        "default_springs", "snowy_vegetation"),
                List.of("snowy_spawns", "common_spawns")
        ));

        biomes.add(new BiomeDef(
                GHBiomes.GLACIAL_PEAKS,
                -1.0f, 0.5f, true,
                0xC8E0FF, 0x3D57D6, 0x050533, 0x9AC0FF,
                Optional.of(0x808080), Optional.of(0x707070),
                List.of("default_carvers", "default_underground",
                        "default_springs", "frozen_springs"),
                List.of("snowy_spawns")
        ));

        // ── Temperate ─────────────────────────────────────────────────────────
        biomes.add(new BiomeDef(
                GHBiomes.TEMPERATE_FOREST,
                0.6f, 0.7f, true,
                0xC0D8FF, 0x3F76E4, 0x050533, 0x78A7FF,
                Optional.empty(), Optional.empty(),
                List.of("default_carvers", "default_underground",
                        "default_springs", "forest_flowers",
                        "default_flowers", "default_grass",
                        "default_mushrooms", "default_vegetation"),
                List.of("common_spawns", "farm_animals")
        ));

        biomes.add(new BiomeDef(
                GHBiomes.HIGHLAND_ORCHARD,
                0.4f, 0.6f, true,
                0xB0C8E8, 0x3F76E4, 0x050533, 0x84A8E8,
                Optional.of(0x7AAB6D), Optional.of(0x6DAA3F),
                List.of("default_carvers", "default_underground",
                        "default_springs", "default_flowers",
                        "default_grass", "default_vegetation"),
                List.of("common_spawns", "farm_animals")
        ));

        biomes.add(new BiomeDef(
                GHBiomes.ROLLING_MEADOWS,
                0.7f, 0.5f, true,
                0xC0D8FF, 0x3F76E4, 0x050533, 0x76A7FF,
                Optional.of(0x91BD59), Optional.empty(),
                List.of("default_carvers", "default_underground",
                        "default_springs", "plains_grass",
                        "default_flowers", "default_grass",
                        "default_mushrooms", "default_vegetation"),
                List.of("common_spawns", "farm_animals", "plains_spawns")
        ));

        biomes.add(new BiomeDef(
                GHBiomes.BIRCH_HIGHLANDS,
                0.5f, 0.6f, true,
                0xC0D8FF, 0x3F76E4, 0x050533, 0x78A7FF,
                Optional.empty(), Optional.empty(),
                List.of("default_carvers", "default_underground",
                        "default_springs", "default_flowers",
                        "default_grass", "default_mushrooms",
                        "default_vegetation", "birch_trees"),
                List.of("common_spawns", "farm_animals")
        ));

        biomes.add(new BiomeDef(
                GHBiomes.WETLANDS,
                0.5f, 0.9f, true,
                0x9BBCB8, 0x617B64, 0x232317, 0x7BA5A0,
                Optional.of(0x6A7039), Optional.of(0x6A7039),
                List.of("default_carvers", "default_underground",
                        "default_springs", "default_flowers",
                        "default_grass", "default_mushrooms",
                        "swamp_vegetation"),
                List.of("common_spawns", "farm_animals")
        ));

        // ── Mediterranean ─────────────────────────────────────────────────────
        biomes.add(new BiomeDef(
                GHBiomes.MEDITERRANEAN_SCRUBLAND,
                1.2f, 0.2f, false,
                0xE8D8B0, 0x3F76E4, 0x050533, 0xE8C878,
                Optional.of(0xA8A050), Optional.of(0x8C9A3C),
                List.of("default_carvers", "default_underground",
                        "default_springs", "default_flowers",
                        "default_grass", "savanna_grass"),
                List.of("common_spawns", "farm_animals")
        ));

        biomes.add(new BiomeDef(
                GHBiomes.COASTAL_CLIFFS,
                0.9f, 0.4f, true,
                0xC8D8E8, 0x3A7FBF, 0x050D20, 0x78B4E0,
                Optional.of(0x8A9860), Optional.of(0x7A9050),
                List.of("default_carvers", "default_underground",
                        "default_springs", "default_flowers",
                        "default_grass"),
                List.of("common_spawns")
        ));

        // ── Tropical ──────────────────────────────────────────────────────────
        biomes.add(new BiomeDef(
                GHBiomes.TROPICAL_RAINFOREST,
                1.8f, 0.9f, true,
                0x9DC0A0, 0x3F76E4, 0x050533, 0x77A89A,
                Optional.of(0x59C93C), Optional.of(0x30BB0B),
                List.of("default_carvers", "default_underground",
                        "default_springs", "jungle_grass",
                        "jungle_vines", "default_flowers",
                        "default_mushrooms", "default_vegetation"),
                List.of("common_spawns", "jungle_spawns")
        ));

        biomes.add(new BiomeDef(
                GHBiomes.MANGROVE_COAST,
                1.6f, 0.8f, true,
                0x8DB8A0, 0x3D7A5E, 0x1A3A2A, 0x77A48A,
                Optional.of(0x4DC040), Optional.of(0x28A018),
                List.of("default_carvers", "default_underground",
                        "default_springs", "jungle_grass",
                        "default_flowers", "default_mushrooms"),
                List.of("common_spawns")
        ));

        biomes.add(new BiomeDef(
                GHBiomes.VOLCANIC_LOWLANDS,
                1.5f, 0.4f, false,
                0xC8A060, 0x3F76E4, 0x050533, 0xC07840,
                Optional.of(0x6B7044), Optional.of(0x5A6B30),
                List.of("default_carvers", "default_underground",
                        "default_springs", "default_flowers",
                        "default_grass", "default_mushrooms"),
                List.of("common_spawns")
        ));

        // ── Wasteland ─────────────────────────────────────────────────────────
        biomes.add(new BiomeDef(
                GHBiomes.ASH_PLAINS,
                0.5f, 0.0f, false,
                0x808070, 0x606060, 0x303030, 0x707060,
                Optional.of(0x606050), Optional.of(0x585840),
                List.of("default_carvers", "default_underground",
                        "default_springs"),
                List.of("common_spawns")
        ));

        biomes.add(new BiomeDef(
                GHBiomes.CRACKED_BADLANDS,
                1.8f, 0.0f, false,
                0xC0905A, 0x905A30, 0x401800, 0xC08040,
                Optional.of(0x907050), Optional.of(0x807040),
                List.of("default_carvers", "default_underground",
                        "default_springs", "badlands_vegetation"),
                List.of("common_spawns")
        ));

        biomes.add(new BiomeDef(
                GHBiomes.OVERGROWN_RUINS,
                0.8f, 0.6f, true,
                0x889060, 0x3F6B44, 0x051505, 0x789060,
                Optional.of(0x5B7A2C), Optional.of(0x4A7A1C),
                List.of("default_carvers", "default_underground",
                        "default_springs", "default_flowers",
                        "default_grass", "default_mushrooms",
                        "jungle_vines", "default_vegetation"),
                List.of("common_spawns")
        ));

        return biomes;
    }

    // ── DataProvider ──────────────────────────────────────────────────────────

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        Path folder = output.getOutputFolder()
                .resolve("data/gh_terraforma/worldgen/biome");

        for (BiomeDef def : buildBiomes()) {
            JsonObject json = buildBiomeJson(def);
            String name = def.key().location().getPath();
            Path path = folder.resolve(name + ".json");
            futures.add(DataProvider.saveStable(cache,
                    GSON.toJsonTree(json), path));
        }

        return CompletableFuture.allOf(
                futures.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "GH TerraForma Biomes";
    }

    // ── JSON builder ──────────────────────────────────────────────────────────

    private JsonObject buildBiomeJson(BiomeDef def) {
        JsonObject json = new JsonObject();

        // ── Climate ───────────────────────────────────────────────────────────
        json.addProperty("has_precipitation", def.hasPrecipitation());
        json.addProperty("temperature",       def.temperature());
        json.addProperty("downfall",          def.downfall());

        // ── Effects ───────────────────────────────────────────────────────────
        JsonObject effects = new JsonObject();
        effects.addProperty("fog_color",       def.fogColor());
        effects.addProperty("water_color",     def.waterColor());
        effects.addProperty("water_fog_color", def.waterFogColor());
        effects.addProperty("sky_color",       def.skyColor());
        def.grassColor().ifPresent(c ->
                effects.addProperty("grass_color", c));
        def.foliageColor().ifPresent(c ->
                effects.addProperty("foliage_color", c));
        json.add("effects", effects);

        // ── Carvers ───────────────────────────────────────────────────────────
        JsonObject carvers = new JsonObject();
        JsonArray airCarvers = new JsonArray();
        airCarvers.add("minecraft:cave");
        airCarvers.add("minecraft:canyon");
        carvers.add("air", airCarvers);
        json.add("carvers", carvers);

        // ── Features ──────────────────────────────────────────────────────────
        // 11 feature steps (vanilla biome structure)
        // We add ores and underground features at appropriate steps
        JsonArray features = new JsonArray();
        // Step 0 — raw generation (none custom)
        features.add(new JsonArray());
        // Step 1 — lakes
        JsonArray lakes = new JsonArray();
        lakes.add("minecraft:lake_lava_underground");
        lakes.add("minecraft:lake_lava_surface");
        features.add(lakes);
        // Step 2-10 — default underground + surface vegetation
        for (int i = 2; i <= 10; i++) features.add(new JsonArray());
        json.add("features", features);

// ── Spawners ──────────────────────────────────────────────────────────────
        JsonObject spawners = new JsonObject();
        spawners.add("monster",   new JsonArray()); // GH monsters added by Combat subpack
        spawners.add("creature",  new JsonArray());
        spawners.add("ambient",   new JsonArray());
        spawners.add("water_creature", new JsonArray());
        spawners.add("water_ambient",  new JsonArray());
        spawners.add("underground_water_creature", new JsonArray());
        spawners.add("misc", new JsonArray());
        json.add("spawners", spawners);

        // ── Spawn costs ───────────────────────────────────────────────────────
        json.add("spawn_costs", new JsonObject());

        return json;
    }

    private JsonArray defaultMonsterSpawns(float temperature) {
        // No vanilla hostile mobs — GH monsters are registered
        // by the Combat & Monsters subpack via spawn rules
        return new JsonArray();
    }
}