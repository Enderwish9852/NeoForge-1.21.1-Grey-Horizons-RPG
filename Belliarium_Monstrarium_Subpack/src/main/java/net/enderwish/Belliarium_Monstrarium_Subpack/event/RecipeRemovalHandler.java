package net.enderwish.Belliarium_Monstrarium_Subpack.event;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
 * matches a blocked item ID. Server-side only, runs on level load.
 *
 * Blocked materials: diamond, diamond block, netherite ingot, netherite scrap,
 * netherite block, gold ingot, gold block, gold nugget, gold ore, deepslate
 * gold ore.
 *
 * Only the result item is checked -- ingredients are ignored. The item
 * itself still exists and can be obtained via /give, loot, trading, etc.
 *
 * Uses RecipeManager.replaceRecipes() to rebuild the recipe registry
 * without blocked items, since NeoForge 1.21.1 has no public removal API
 * and RecipeManager's internal maps are immutable.
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

        java.util.List<RecipeHolder<?>> kept = new java.util.ArrayList<>();
        for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
            total++;
            Recipe<?> recipe = holder.value();
            ResourceLocation id = holder.id();

            ItemStack result = recipe.getResultItem(registries);
            if (result != null && BLOCKED_MATERIALS.contains(result.getItem())) {
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
            LOGGER.info("RecipeRemovalHandler: {} recipes total, 0 removed",
                    total);
        }
    }
}
