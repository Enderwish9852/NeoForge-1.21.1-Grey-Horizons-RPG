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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.util.*;

public class FoodRegistry implements ResourceManagerReloadListener {

    public static final FoodRegistry INSTANCE = new FoodRegistry();
    private FoodRegistry() {}

    private static final Logger LOGGER = LoggerFactory.getLogger("GHFarming");

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private static final String FOLDER    = "food";
    private static final String NAMESPACE = "gh_farming_overhaul";

    private final Map<String, FoodDefinition> definitions = new HashMap<>();

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        definitions.clear();
        LOGGER.info("FoodRegistry reloading... scanning folder: {}", FOLDER);

        Map<ResourceLocation, Resource> resources = manager.listResources(
                FOLDER,
                path -> path.getNamespace().equals(NAMESPACE)
                        && path.getPath().endsWith(".json")
        );

        LOGGER.info("FoodRegistry found {} files.", resources.size());

        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            ResourceLocation fileId = entry.getKey();

            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement fileJson = GsonHelper.fromJson(GSON, reader, JsonElement.class);

                if (!fileJson.isJsonObject()) {
                    LOGGER.error("Expected JSON object in {}", fileId);
                    continue;
                }

                for (Map.Entry<String, JsonElement> foodEntry
                        : fileJson.getAsJsonObject().entrySet()) {

                    String foodId = foodEntry.getKey();
                    JsonElement foodJson = foodEntry.getValue();

                    FoodDefinition.CODEC.parse(JsonOps.INSTANCE, foodJson)
                            .resultOrPartial(error ->
                                    LOGGER.error("Failed to parse food '{}' in {}: {}", foodId, fileId, error)
                            )
                            .ifPresent(def -> {
                                definitions.put(foodId, def);
                                LOGGER.info("Loaded food: {}", foodId);
                            });
                }

            } catch (IOException e) {
                LOGGER.error("Could not read food JSON {}: {}", fileId, e.getMessage());
            }
        }

        LOGGER.info("FoodRegistry loaded {} food definitions.", definitions.size());
    }

    public FoodDefinition getByName(String itemId) {
        return definitions.get(itemId);
    }

    public boolean isRegistered(String itemId) {
        return definitions.containsKey(itemId);
    }

    public Set<String> getAllNames() {
        return Collections.unmodifiableSet(definitions.keySet());
    }

    public boolean isLoaded() {
        return !definitions.isEmpty();
    }
}