package net.enderwish.Farming_Overhaul_Subpack.core.food;

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
 * FoodRegistry
 *
 * Loads all food definitions from JSON at resource reload.
 * Mirrors CropRegistry pattern exactly.
 *
 * JSON files live at:
 *   src/main/resources/data/gh_farming_overhaul/food/vanilla_food.json
 *   src/main/resources/data/gh_farming_overhaul/food/modded_food.json
 *
 * Each file is a map of item ID → FoodDefinition.
 * e.g. { "cooked_beef": { "spoil_days": 5, "weight_kg": 0.25 } }
 *
 * Other classes never read JSON directly — always go through FoodRegistry.INSTANCE.
 */
public class FoodRegistry implements ResourceManagerReloadListener {

    // ── Singleton ─────────────────────────────────────────────────────────────

    public static final FoodRegistry INSTANCE = new FoodRegistry();
    private FoodRegistry() {}

    // ── Storage ───────────────────────────────────────────────────────────────

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private static final String FOLDER    = "food";
    private static final String NAMESPACE = "gh_farming_overhaul";

    private final Map<String, FoodDefinition> definitions = new HashMap<>();

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

                if (!fileJson.isJsonObject()) {
                    System.err.println("[GHFarming] Expected JSON object in " + fileId);
                    continue;
                }

                for (Map.Entry<String, JsonElement> foodEntry
                        : fileJson.getAsJsonObject().entrySet()) {

                    String foodId = foodEntry.getKey();
                    JsonElement foodJson = foodEntry.getValue();

                    FoodDefinition.CODEC.parse(JsonOps.INSTANCE, foodJson)
                            .resultOrPartial(error ->
                                    System.err.println("[GHFarming] Failed to parse food '"
                                            + foodId + "' in " + fileId + ": " + error)
                            )
                            .ifPresent(def -> {
                                definitions.put(foodId, def);
                                System.out.println("[GHFarming] Loaded food: " + foodId);
                            });
                }

            } catch (IOException e) {
                System.err.println("[GHFarming] Could not read food JSON "
                        + fileId + ": " + e.getMessage());
            }
        }

        System.out.println("[GHFarming] FoodRegistry loaded "
                + definitions.size() + " food definitions.");
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    /** Returns the FoodDefinition for the given item ID, or null if not found. */
    public FoodDefinition getByName(String itemId) {
        return definitions.get(itemId);
    }

    /** Returns true if the item ID is registered as a spoilable food. */
    public boolean isRegistered(String itemId) {
        return definitions.containsKey(itemId);
    }

    /** Returns all registered food item IDs. */
    public Set<String> getAllNames() {
        return Collections.unmodifiableSet(definitions.keySet());
    }

    /** Returns true if at least one food definition is loaded. */
    public boolean isLoaded() {
        return !definitions.isEmpty();
    }
}
