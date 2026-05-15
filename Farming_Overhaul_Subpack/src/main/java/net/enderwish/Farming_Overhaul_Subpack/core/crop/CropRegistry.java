package net.enderwish.Farming_Overhaul_Subpack.core.crop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.GsonHelper;

import java.io.IOException;
import java.io.Reader;
import java.util.*;

/**
 * CropRegistry
 *
 * Loads all crop definitions from JSON at resource reload.
 * Mirrors the WeatherRegistry pattern from Atmospheric subpack.
 *
 * JSON files live at:
 *   src/main/resources/data/gh_farming_overhaul/crops/spring_crops.json
 *   src/main/resources/data/gh_farming_overhaul/crops/summer_crops.json
 *   src/main/resources/data/gh_farming_overhaul/crops/autumn_crops.json
 *   src/main/resources/data/gh_farming_overhaul/crops/winter_crops.json
 *
 * Each file contains a map of crop ID → CropDefinition.
 * e.g. { "wheat": { "growth_type": "GROUND", ... } }
 *
 * Other classes never read JSON directly — always go through CropRegistry.INSTANCE.
 */
public class CropRegistry implements ResourceManagerReloadListener {

    // ── Singleton ─────────────────────────────────────────────────────────────

    public static final CropRegistry INSTANCE = new CropRegistry();
    private CropRegistry() {}

    // ── Storage ───────────────────────────────────────────────────────────────

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private static final String FOLDER = "crops";
    private static final String NAMESPACE = "gh_farming_overhaul";

    private final Map<String, CropDefinition> definitions = new HashMap<>();

    // ── Load ──────────────────────────────────────────────────────────────────

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        definitions.clear();

        Map<ResourceLocation, Resource> resources = manager.listResources(
                FOLDER,
                path -> path.getNamespace().equals(NAMESPACE)
                        && path.getPath().endsWith(".json")
        );

        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            ResourceLocation fileId = entry.getKey();

            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement fileJson = GsonHelper.fromJson(GSON, reader, JsonElement.class);

                // Each file is a JSON object of cropId → CropDefinition
                if (!fileJson.isJsonObject()) {
                    System.err.println("[GHFarming] Expected JSON object in " + fileId);
                    continue;
                }

                for (Map.Entry<String, JsonElement> cropEntry
                        : fileJson.getAsJsonObject().entrySet()) {

                    String cropId = cropEntry.getKey();
                    JsonElement cropJson = cropEntry.getValue();

                    CropDefinition.CODEC.parse(JsonOps.INSTANCE, cropJson)
                            .resultOrPartial(error ->
                                    System.err.println("[GHFarming] Failed to parse crop '"
                                            + cropId + "' in " + fileId + ": " + error)
                            )
                            .ifPresent(def -> {
                                definitions.put(cropId, def);
                                System.out.println("[GHFarming] Loaded crop: " + cropId);
                            });
                }

            } catch (IOException e) {
                System.err.println("[GHFarming] Could not read crop JSON "
                        + fileId + ": " + e.getMessage());
            }
        }

        System.out.println("[GHFarming] CropRegistry loaded "
                + definitions.size() + " crop definitions.");
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    /** Returns the CropDefinition for the given crop ID, or the fallback if not found. */
    public CropDefinition getByName(String cropId) {
        return definitions.getOrDefault(cropId, getFallback());
    }

    /** Returns true if the crop ID is registered. */
    public boolean isRegistered(String cropId) {
        return definitions.containsKey(cropId);
    }

    /** Returns all registered crop IDs. */
    public Set<String> getAllNames() {
        return Collections.unmodifiableSet(definitions.keySet());
    }

    /** Returns all definitions for crops that can grow in the given season. */
    public List<CropDefinition> getForSeason(String seasonName) {
        List<CropDefinition> result = new ArrayList<>();
        for (CropDefinition def : definitions.values()) {
            if (def.canGrowIn(seasonName)) result.add(def);
        }
        return result;
    }

    /** Returns true if at least one crop definition is loaded. */
    public boolean isLoaded() {
        return !definitions.isEmpty();
    }

    // ── Fallback ──────────────────────────────────────────────────────────────

    /**
     * Returns a safe default definition used when a crop ID is not found.
     * Prevents null pointer exceptions in handlers.
     */
    public CropDefinition getFallback() {
        return new CropDefinition(
                CropDefinition.GrowthType.GROUND,
                10,       // growth_days
                7,        // spoil_days
                0.1f,     // weight_kg
                64,       // stack_size
                1,        // nutrition
                0,        // hydration
                true,     // year_round — fallback grows any time
                List.of() // no seasons needed since year_round is true
        );
    }
}