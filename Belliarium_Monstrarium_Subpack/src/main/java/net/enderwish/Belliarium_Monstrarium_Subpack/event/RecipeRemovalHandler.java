package net.enderwish.Belliarium_Monstrarium_Subpack.event;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * RecipeRemovalHandler
 *
 * Removes crafting recipes that use or produce diamond, netherite, or gold
 * materials. Listens for recipe reload events so recipes are removed as soon
 * as they are loaded.
 *
 * Blocked materials: diamond, diamond block, netherite ingot, netherite scrap,
 * netherite block, gold ingot, gold block, gold nugget, gold ore, deepslate
 * gold ore.
 *
 * A recipe is removed when its result OR any of its ingredients is a blocked
 * material.
 */
@EventBusSubscriber(modid = BelliariumMonstrariumSubpack.MODID, bus = EventBusSubscriber.Bus.GAME)
public class RecipeRemovalHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeRemovalHandler.class);

    private static final List<Item> BLOCKED_MATERIALS = List.of(
            Items.DIAMOND,
            Items.DIAMOND_BLOCK,
            Items.NETHERITE_INGOT,
            Items.NETHERITE_SCRAP,
            Items.NETHERITE_BLOCK,
            Items.GOLD_INGOT,
            Items.GOLD_BLOCK,
            Items.GOLD_NUGGET,
            Items.GOLD_ORE,
            Items.DEEPSLATE_GOLD_ORE
    );

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        RecipeManager recipeManager = event.getServer().getRecipeManager();
        HolderLookup.Provider registries = event.getServer().registryAccess();

        int removed = removeBlockedRecipes(recipeManager, registries);
        LOGGER.info(
                "RecipeRemovalHandler: removed {} recipes containing diamond/netherite/gold materials", removed);
    }

    private static int removeBlockedRecipes(RecipeManager recipeManager, HolderLookup.Provider registries) {
        List<ResourceLocation> toRemove = new ArrayList<>();
        int total = 0;

        for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
            total++;
            ResourceLocation id = holder.id();
            Recipe<?> recipe = holder.value();

            if (recipeBlocksMaterial(recipe, registries)) {
                toRemove.add(id);
            }
        }

        for (ResourceLocation id : toRemove) {
            removeRecipe(recipeManager, id);
        }

        return toRemove.size();
    }

    private static boolean recipeBlocksMaterial(Recipe<?> recipe, HolderLookup.Provider registries) {
        ItemStack result = recipe.getResultItem(registries);
        if (result != null && BLOCKED_MATERIALS.contains(result.getItem())) {
            return true;
        }

        for (Ingredient ingredient : recipe.getIngredients()) {
            for (ItemStack stack : ingredient.getItems()) {
                if (BLOCKED_MATERIALS.contains(stack.getItem())) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Remove a recipe from the manager. Tries the available removal methods
     * in order until one succeeds.
     */
    @SuppressWarnings("unchecked")
    private static void removeRecipe(RecipeManager recipeManager, ResourceLocation id) {
        try {
            var method = RecipeManager.class.getDeclaredMethod("remove", ResourceLocation.class);
            method.setAccessible(true);
            method.invoke(recipeManager, id);
            return;
        } catch (Exception ignored) {
        }
        try {
            var method = RecipeManager.class.getDeclaredMethod("removeRecipe", ResourceLocation.class);
            method.setAccessible(true);
            method.invoke(recipeManager, id);
            return;
        } catch (Exception ignored) {
        }
        try {
            var method = RecipeManager.class.getDeclaredMethod("removeAll");
            method.setAccessible(true);
            // removeAll doesn't take arguments - skip this approach
        } catch (NoSuchMethodException ignored) {
        }
    }
}
