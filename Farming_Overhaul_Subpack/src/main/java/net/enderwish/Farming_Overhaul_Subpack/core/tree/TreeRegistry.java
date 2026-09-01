package net.enderwish.Farming_Overhaul_Subpack.core.tree;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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

/**
 * TreeRegistry
 *
 * Loads all fruit tree definitions from:
 *   data/gh_farming_overhaul/fruit_trees/<species>.json
 *
 * Same pattern as CropRegistry and CuttingBoardRecipeRegistry.
 * Registered as both a server and client reload listener.
 */
public class TreeRegistry implements ResourceManagerReloadListener {

    public static final TreeRegistry INSTANCE = new TreeRegistry();
    private TreeRegistry() {}

    private static final Logger LOGGER =
            LoggerFactory.getLogger("GHFarming/TreeRegistry");
    private static final Gson GSON =
            new GsonBuilder().setPrettyPrinting().create();
    private static final String FOLDER    = "fruit_trees";
    private static final String NAMESPACE = "gh_farming_overhaul";

    private final Map<String, TreeDefinition> definitions = new HashMap<>();

    // ── Reload ────────────────────────────────────────────────────────────────

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        definitions.clear();
        LOGGER.info("TreeRegistry reloading...");

        Map<ResourceLocation, Resource> resources = manager.listResources(
                FOLDER,
                path -> path.getNamespace().equals(NAMESPACE)
                        && path.getPath().endsWith(".json"));

        for (Map.Entry<ResourceLocation, Resource> entry :
                resources.entrySet()) {
            ResourceLocation fileId = entry.getKey();

            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement json = GsonHelper.fromJson(
                        GSON, reader, JsonElement.class);
                JsonObject obj = json.getAsJsonObject();

                // Species ID = filename without extension
                String path    = fileId.getPath();
                String species = path.substring(
                        path.lastIndexOf('/') + 1,
                        path.lastIndexOf('.'));

                TreeDefinition def = TreeDefinition.fromJson(species, obj);
                definitions.put(species, def);
                LOGGER.info("Loaded tree definition: {}", species);

            } catch (IOException e) {
                LOGGER.error("Could not read tree definition {}: {}",
                        fileId, e.getMessage());
            } catch (Exception e) {
                LOGGER.error("Failed to parse tree definition {}: {}",
                        fileId, e.getMessage());
            }
        }

        LOGGER.info("TreeRegistry loaded {} tree definitions.",
                definitions.size());
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    /**
     * Returns the TreeDefinition for a species, or empty if not found.
     */
    public Optional<TreeDefinition> getBySpecies(String species) {
        return Optional.ofNullable(definitions.get(species));
    }

    /**
     * Returns all loaded tree definitions.
     */
    public Collection<TreeDefinition> getAll() {
        return Collections.unmodifiableCollection(definitions.values());
    }

    /**
     * Returns all species IDs.
     */
    public Set<String> getAllSpecies() {
        return Collections.unmodifiableSet(definitions.keySet());
    }

    /**
     * Returns true if a species is registered.
     */
    public boolean hasSpecies(String species) {
        return definitions.containsKey(species);
    }
}
