package net.enderwish.Belliarium_Monstrarium_Subpack.event;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent.Load;

import java.util.List;

/**
 * RecipeRemovalHandler
 *
 * Chald Recipe Blocker style: removes crafting recipes whose RESULT item
 * or any ingredient matches a blocked material. Server-side only, runs on level load.
 *
 * Blocked materials: iron, gold, lapis lazuli, redstone, glowstone, copper, diamond.
 * Any recipe whose result or any ingredient is made from these materials is removed.
 * The items themselves still exist and can be obtained via /give, loot, trading, etc.
 *
 * Uses RecipeManager.replaceRecipes() to rebuild the registry without blocked items,
 * since NeoForge 1.21.1 RecipeManager's internal maps are immutable.
 */
@EventBusSubscriber(modid = BelliariumMonstrariumSubpack.MODID, bus = EventBusSubscriber.Bus.GAME)
public class RecipeRemovalHandler {

    private static final org.slf4j.Logger LOGGER =
            org.slf4j.LoggerFactory.getLogger(RecipeRemovalHandler.class);

    /** Material keywords whose items should be removed from crafting recipes. */
    private static final List<String> BLOCKED_MATERIALS = List.of(
            "iron",
            "gold",
            "lapis",
            "redstone",
            "glowstone",
            "copper",
            "diamond"
    );

    @SubscribeEvent
    public static void onLevelLoad(Load event) {
        LevelAccessor levelAccessor = event.getLevel();
        if (!(levelAccessor instanceof Level level)) {
            return;
        }

        // Only run on server side - client level has no server
        if (level.isClientSide()) {
            return;
        }

        RecipeManager recipeManager = level.getServer().getRecipeManager();
        HolderLookup.Provider registries = level.registryAccess();

        LOGGER.info("RecipeRemovalHandler: LevelEvent.Load - RecipeManager class: {}",
                recipeManager.getClass().getName());

        int total = 0;
        int removed = 0;
        List<RecipeHolder<?>> kept = new java.util.ArrayList<>();

        for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
            total++;
            Recipe<?> recipe = holder.value();
            ResourceLocation id = holder.id();

            ItemStack result = recipe.getResultItem(registries);
            boolean resultBlocked = result != null && isBlocked(result.getItem());
            boolean ingredientBlocked = recipe.getIngredients().stream()
                    .flatMap(ingredient -> java.util.List.of(ingredient.getItems()).stream())
                    .anyMatch(stack -> !stack.isEmpty() && isBlocked(stack.getItem()));

            if (resultBlocked || ingredientBlocked) {
                LOGGER.debug("RecipeRemovalHandler: removing recipe {}: {}",
                        id, recipe.getClass().getSimpleName());
                removed++;
            } else {
                kept.add(holder);
            }
        }

        if (removed > 0) {
            recipeManager.replaceRecipes(kept);
            LOGGER.info("RecipeRemovalHandler: {} recipes total, {} removed",
                    total, removed);
        } else {
            LOGGER.info("RecipeRemovalHandler: {} recipes total, 0 removed", total);
        }
    }

    /** Check if an item's description ID contains any blocked material keyword. */
    private static boolean isBlocked(net.minecraft.world.item.Item item) {
        String name = item.getDescriptionId().toLowerCase();
        for (String material : BLOCKED_MATERIALS) {
            if (name.contains(material)) {
                return true;
            }
        }
        return false;
    }
}
