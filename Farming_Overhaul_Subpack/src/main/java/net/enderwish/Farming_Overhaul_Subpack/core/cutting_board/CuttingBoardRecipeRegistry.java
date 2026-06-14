package net.enderwish.Farming_Overhaul_Subpack.core.cutting_board;

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
 * CuttingBoardRecipeRegistry
 *
 * Loads all cutting board recipes from:
 *   data/gh_farming_overhaul/cutting_board_recipes/*.json
 *
 * findMatch does two passes:
 *   Pass 1 — perfect tool match
 *   Pass 2 — cross-tool match (penalty applied in block entity)
 */
public class CuttingBoardRecipeRegistry implements ResourceManagerReloadListener {

    public static final CuttingBoardRecipeRegistry INSTANCE =
            new CuttingBoardRecipeRegistry();
    private CuttingBoardRecipeRegistry() {}

    private static final Logger LOGGER =
            LoggerFactory.getLogger("GHFarming");
    private static final Gson GSON =
            new GsonBuilder().setPrettyPrinting().create();
    private static final String FOLDER    = "cutting_board_recipes";
    private static final String NAMESPACE = "gh_farming_overhaul";

    private final List<CuttingBoardRecipe> recipes = new ArrayList<>();

    // ── Match Result ──────────────────────────────────────────────────────────

    public record MatchResult(CuttingBoardRecipe recipe, boolean isCrossTool) {}

    // ── Resource reload ───────────────────────────────────────────────────────

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        recipes.clear();
        LOGGER.info("CuttingBoardRecipeRegistry reloading...");

        Map<ResourceLocation, Resource> resources = manager.listResources(
                FOLDER,
                path -> path.getNamespace().equals(NAMESPACE)
                        && path.getPath().endsWith(".json")
        );

        for (Map.Entry<ResourceLocation, Resource> entry :
                resources.entrySet()) {
            ResourceLocation fileId = entry.getKey();

            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement json = GsonHelper.fromJson(
                        GSON, reader, JsonElement.class);
                CuttingBoardRecipe.CODEC.parse(JsonOps.INSTANCE, json)
                        .resultOrPartial(error ->
                                LOGGER.error(
                                        "Failed to parse cutting board recipe {}: {}",
                                        fileId, error))
                        .ifPresent(recipe -> {
                            recipes.add(recipe);
                            LOGGER.info("Loaded cutting board recipe: {}",
                                    fileId);
                        });
            } catch (IOException e) {
                LOGGER.error("Could not read cutting board recipe {}: {}",
                        fileId, e.getMessage());
            }
        }

        LOGGER.info("CuttingBoardRecipeRegistry loaded {} recipes.",
                recipes.size());
    }

    // ── Find Match ────────────────────────────────────────────────────────────

    /**
     * Two-pass matching:
     * Pass 1 — perfect tool match (full output)
     * Pass 2 — cross-tool match (penalty applied in CuttingBoardBlockEntity)
     */
    public Optional<MatchResult> findMatch(List<ItemStack> grid,
                                           ItemStack tool,
                                           ItemStack container) {
        // Pass 1 — perfect tool match
        for (CuttingBoardRecipe recipe : recipes) {
            if (recipe.getToolMatch(tool) == CuttingBoardRecipe.ToolMatch.PERFECT
                    && recipe.matchesContainer(container)
                    && recipe.matchesGrid(grid)) {
                return Optional.of(new MatchResult(recipe, false));
            }
        }

        // Pass 2 — cross-tool match
        for (CuttingBoardRecipe recipe : recipes) {
            if (recipe.getToolMatch(tool) == CuttingBoardRecipe.ToolMatch.CROSS_TOOL
                    && recipe.matchesContainer(container)
                    && recipe.matchesGrid(grid)) {
                return Optional.of(new MatchResult(recipe, true));
            }
        }

        return Optional.empty();
    }

    // ── All Recipes ───────────────────────────────────────────────────────────

    public List<CuttingBoardRecipe> getAllRecipes() {
        return Collections.unmodifiableList(recipes);
    }

    public List<CuttingBoardRecipe> getByCategory(
            CuttingBoardRecipe.CBCategory category) {
        return recipes.stream()
                .filter(r -> r.getCategory() == category)
                .collect(java.util.stream.Collectors.toList());
    }
}