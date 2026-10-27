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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * RecipeRemovalHandler
 *
 * Removes crafting recipes that use or produce diamond, netherite, or gold
 * materials. Listens for server start and recipe reload events.
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

    private static final org.slf4j.Logger LOGGER =
            org.slf4j.LoggerFactory.getLogger(RecipeRemovalHandler.class);

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

        LOGGER.info("RecipeRemovalHandler: ServerStartingEvent - RecipeManager class: {}",
                recipeManager.getClass().getName());

        int total = 0;
        int blocked = 0;
        int removed = 0;

        List<ResourceLocation> toRemove = new ArrayList<>();
        for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
            total++;
            ResourceLocation id = holder.id();
            Recipe<?> recipe = holder.value();

            if (recipeBlocksMaterial(recipe, registries)) {
                blocked++;
                toRemove.add(id);
                LOGGER.debug("RecipeRemovalHandler: blocking recipe {}: {}", id, recipe.getClass().getSimpleName());
            }
        }

        for (ResourceLocation id : toRemove) {
            if (removeRecipe(recipeManager, id)) {
                removed++;
            }
        }

        LOGGER.info("RecipeRemovalHandler: {} recipes total, {} blocked ({}%), {} removed",
                total, blocked, total > 0 ? (blocked * 100 / total) : 0, removed);

        if (LOGGER.isDebugEnabled()) {
            for (Field field : RecipeManager.class.getDeclaredFields()) {
                LOGGER.debug("RecipeManager field: {} (type={})",
                        field.getName(), field.getType().getSimpleName());
            }
        }
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        LOGGER.debug("RecipeRemovalHandler: AddReloadListenerEvent - recipe reload may happen next");
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
     * Remove a recipe from the manager via reflection.
     * Tries: remove(ResourceLocation), removeRecipe(ResourceLocation),
     * and direct field access to the internal recipes map.
     */
    @SuppressWarnings("unchecked")
    private static boolean removeRecipe(RecipeManager recipeManager, ResourceLocation id) {
        // Try remove(ResourceLocation)
        try {
            var method = RecipeManager.class.getDeclaredMethod("remove", ResourceLocation.class);
            method.setAccessible(true);
            method.invoke(recipeManager, id);
            return true;
        } catch (Exception ignored) {
        }

        // Try removeRecipe(ResourceLocation)
        try {
            var method = RecipeManager.class.getDeclaredMethod("removeRecipe", ResourceLocation.class);
            method.setAccessible(true);
            method.invoke(recipeManager, id);
            return true;
        } catch (Exception ignored) {
        }

        // Try direct field access to internal recipes map
        try {
            for (Field field : RecipeManager.class.getDeclaredFields()) {
                if (Map.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    Map<ResourceLocation, RecipeHolder<?>> recipes =
                            (Map<ResourceLocation, RecipeHolder<?>>) field.get(recipeManager);
                    if (recipes != null && recipes.remove(id) != null) {
                        return true;
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return false;
    }
}
