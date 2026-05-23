package net.enderwish.Farming_Overhaul_Subpack.core.claypot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.util.*;

/**
 * ClayPotRecipeRegistry
 *
 * Loads all clay pot recipes from JSON at resource reload.
 * Files live at:
 *   data/gh_farming_overhaul/clay_pot_recipes/*.json
 *
 * Each file is one recipe.
 */
public class ClayPotRecipeRegistry implements ResourceManagerReloadListener {

    public static final ClayPotRecipeRegistry INSTANCE = new ClayPotRecipeRegistry();
    private ClayPotRecipeRegistry() {}

    private static final Logger LOGGER = LoggerFactory.getLogger("GHFarming");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FOLDER = "clay_pot_recipes";
    private static final String NAMESPACE = "gh_farming_overhaul";

    private final List<ClayPotRecipe> recipes = new ArrayList<>();

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        recipes.clear();
        LOGGER.info("ClayPotRecipeRegistry reloading...");

        Map<ResourceLocation, Resource> resources = manager.listResources(
                FOLDER,
                path -> path.getNamespace().equals(NAMESPACE)
                        && path.getPath().endsWith(".json")
        );

        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            ResourceLocation fileId = entry.getKey();

            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement json = GsonHelper.fromJson(GSON, reader, JsonElement.class);
                ClayPotRecipe.CODEC.parse(JsonOps.INSTANCE, json)
                        .resultOrPartial(error ->
                                LOGGER.error("Failed to parse clay pot recipe {}: {}", fileId, error)
                        )
                        .ifPresent(recipe -> {
                            recipes.add(recipe);
                            LOGGER.info("Loaded clay pot recipe: {}", fileId);
                        });
            } catch (IOException e) {
                LOGGER.error("Could not read clay pot recipe {}: {}", fileId, e.getMessage());
            }
        }

        LOGGER.info("ClayPotRecipeRegistry loaded {} recipes.", recipes.size());
    }

    /**
     * Finds a matching recipe for the given ingredient grid and water level.
     * Returns empty if no match found.
     */
    public Optional<ClayPotRecipe> findMatch(List<ItemStack> grid, int waterLevel) {
        for (ClayPotRecipe recipe : recipes) {
            if (recipe.matches(grid, waterLevel)) return Optional.of(recipe);
        }
        return Optional.empty();
    }

    public List<ClayPotRecipe> getAllRecipes() {
        return Collections.unmodifiableList(recipes);
    }

    public boolean isLoaded() {
        return !recipes.isEmpty();
    }
}
