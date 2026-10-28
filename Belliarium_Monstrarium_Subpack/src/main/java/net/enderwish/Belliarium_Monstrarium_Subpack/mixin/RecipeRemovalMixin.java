package net.enderwish.Belliarium_Monstrarium_Subpack.mixin;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * RecipeRemovalMixin
 *
 * Adds a public removeRecipe(ResourceLocation) method to RecipeManager
 * so mods can remove recipes at runtime. RecipeManager has no public
 * removal API in NeoForge 1.21.1, so we expose one via mixin.
 *
 * The internal recipes map is byName (Map<ResourceLocation, RecipeHolder<?>>).
 */
@Mixin(RecipeManager.class)
public class RecipeRemovalMixin {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeRemovalMixin.class);

    /**
     * Remove a recipe from the manager by its resource location.
     * Called from RecipeRemovalHandler.
     */
    public void ghBelliarium$removeRecipe(ResourceLocation id) {
        try {
            Field field = RecipeManager.class.getDeclaredField("byName");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<ResourceLocation, RecipeHolder<?>> recipes =
                    (Map<ResourceLocation, RecipeHolder<?>>) field.get(this);
            if (recipes != null) {
                boolean removed = recipes.remove(id) != null;
                if (removed) {
                    LOGGER.debug("RecipeRemovalMixin: removed recipe {}", id);
                }
            }
        } catch (Exception e) {
            LOGGER.warn("RecipeRemovalMixin: failed to remove recipe {}", id, e);
        }
    }
}
