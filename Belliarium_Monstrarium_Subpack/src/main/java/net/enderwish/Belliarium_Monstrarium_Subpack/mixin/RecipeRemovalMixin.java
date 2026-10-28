package net.enderwish.Belliarium_Monstrarium_Subpack.mixin;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/**
 * RecipeRemovalMixin
 *
 * Adds a public removeRecipe(ResourceLocation) method to RecipeManager
 * so mods can remove recipes at runtime. RecipeManager has no public
 * removal method in NeoForge 1.21.1, so we expose one via mixin.
 *
 * The internal recipes map is byName (Map<ResourceLocation, RecipeHolder<?>>).
 */
@Mixin(RecipeManager.class)
public class RecipeRemovalMixin {

    @SuppressWarnings("unchecked")
    public void ghBelliarium$removeRecipe(ResourceLocation id) {
        // Access the private byName field via reflection since RecipeManager
        // has no public removal API in NeoForge 1.21.1
        try {
            java.lang.reflect.Field field = RecipeManager.class.getDeclaredField("byName");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<ResourceLocation, RecipeHolder<?>> recipes =
                    (Map<ResourceLocation, RecipeHolder<?>>) field.get(this);
            if (recipes != null) {
                recipes.remove(id);
            }
        } catch (Exception e) {
            net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack.LOGGER
                    .warn("RecipeRemovalHandler: failed to remove recipe {} via reflection", id);
        }
    }

    // Helper method called from RecipeRemovalHandler to remove a recipe
    public static void removeRecipeViaMixin(RecipeManager manager, ResourceLocation id) {
        if (manager instanceof RecipeRemovalMixin mixin) {
            mixin.ghBelliarium$removeRecipe(id);
        }
    }
}
