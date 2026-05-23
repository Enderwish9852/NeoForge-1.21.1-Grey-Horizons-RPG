package net.enderwish.Farming_Overhaul_Subpack.api;

import net.enderwish.Farming_Overhaul_Subpack.core.crop.CropDefinition;
import net.enderwish.Farming_Overhaul_Subpack.core.crop.CropRegistry;
import net.enderwish.Farming_Overhaul_Subpack.core.food.FoodRegistry;
import net.enderwish.Farming_Overhaul_Subpack.core.spoilage.ModDataComponents;
import net.enderwish.Farming_Overhaul_Subpack.core.spoilage.SpoilageComponent;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

/**
 * FarmingAPI
 *
 * The ONLY class other subpacks should import from Farming_Overhaul_Subpack.
 * A static facade over the farming internals.
 *
 * Other subpacks never import CropRegistry, CropDefinition,
 * SpoilageComponent, or ModDataComponents directly.
 *
 * Usage example (from Hydration subpack):
 *   import net.enderwish.Farming_Overhaul_Subpack.api.FarmingAPI;
 *
 *   int hydration = FarmingAPI.getHydration(stack);
 *   float weight = FarmingAPI.getWeight(stack);
 *   boolean fresh = FarmingAPI.isFresh(stack);
 */
public final class FarmingAPI {

    private FarmingAPI() {}

    // ── Crop queries ──────────────────────────────────────────────────────────

    /** Returns true if the given crop ID is registered in the crop system. */
    public static boolean isCropRegistered(String cropId) {
        return CropRegistry.INSTANCE.isRegistered(cropId);
    }

    /** Returns all registered crop IDs. */
    public static Set<String> getAllCropIds() {
        return CropRegistry.INSTANCE.getAllNames();
    }

    /**
     * Returns true if the crop can grow in the given season.
     * seasonName should come from SeasonsAPI.getSeason(level).name()
     * e.g. "SPRING", "SUMMER", "AUTUMN", "WINTER"
     */
    public static boolean canGrowInSeason(String cropId, String seasonName) {
        return CropRegistry.INSTANCE.getByName(cropId).canGrowIn(seasonName);
    }

    /**
     * Returns the season tooltip string for a crop.
     * e.g. "Spring, Summer" or "Year Round"
     * Use this for HUD subpack display.
     */
    public static String getSeasonTooltip(String cropId) {
        return CropRegistry.INSTANCE.getByName(cropId).getSeasonTooltip();
    }

    /**
     * Returns the growth type of a crop.
     * e.g. GROUND, ROOT, VINE, BUSH, TREE etc.
     */
    public static CropDefinition.GrowthType getGrowthType(String cropId) {
        return CropRegistry.INSTANCE.getByName(cropId).growthType();
    }

    /**
     * Returns the number of in-game days this crop takes to fully grow.
     */
    public static int getGrowthDays(String cropId) {
        return CropRegistry.INSTANCE.getByName(cropId).growthDays();
    }

    // ── Spoilage queries (item stack) ─────────────────────────────────────────

    /**
     * Returns true if the item stack has a spoilage component attached.
     * Use this to check before calling any other spoilage method.
     */
    public static boolean isSpoilable(ItemStack stack) {
        return stack.has(ModDataComponents.SPOILAGE.get());
    }

    /**
     * Returns the spoilage progress as a 0.0 - 1.0 fraction.
     * 0.0 = perfectly fresh, 1.0 = fully spoiled.
     * Returns 0.0 if the item has no spoilage component.
     */
    public static float getSpoilProgress(ItemStack stack) {
        SpoilageComponent comp = stack.get(ModDataComponents.SPOILAGE.get());
        return comp == null ? 0.0f : comp.getProgress();
    }

    /**
     * Returns true if the item is fresh (progress below 75%).
     * Returns true if the item has no spoilage component.
     */
    public static boolean isFresh(ItemStack stack) {
        SpoilageComponent comp = stack.get(ModDataComponents.SPOILAGE.get());
        return comp == null || comp.isFresh();
    }

    /**
     * Returns true if the item is stale (progress between 75% and 100%).
     * Returns false if the item has no spoilage component.
     */
    public static boolean isStale(ItemStack stack) {
        SpoilageComponent comp = stack.get(ModDataComponents.SPOILAGE.get());
        return comp != null && comp.isStale();
    }

    /**
     * Returns true if the item is fully spoiled (progress at 100%+).
     * Returns false if the item has no spoilage component.
     */
    public static boolean isSpoiled(ItemStack stack) {
        SpoilageComponent comp = stack.get(ModDataComponents.SPOILAGE.get());
        return comp != null && comp.isSpoiled();
    }

    // ── Weight queries (item stack) ───────────────────────────────────────────

    /**
     * Returns the weight per item in kg for a registered crop.
     * Returns 0.0 if the crop is not registered.
     * Use this in the Weight subpack to calculate carry weight.
     */
    public static float getWeightKg(String cropId) {
        if (!CropRegistry.INSTANCE.isRegistered(cropId)) return 0.0f;
        return CropRegistry.INSTANCE.getByName(cropId).weightKg();
    }

    /**
     * Returns the total weight of an item stack in kg.
     * Accounts for stack size.
     * Returns 0.0 if the crop is not registered.
     */
    public static float getStackWeightKg(ItemStack stack) {
        String itemId = stack.getItem()
                .builtInRegistryHolder()
                .key().location().getPath();
        if (!CropRegistry.INSTANCE.isRegistered(itemId)) return 0.0f;
        return CropRegistry.INSTANCE.getByName(itemId).weightKg() * stack.getCount();
    }

    // ── Nutrition and hydration queries ───────────────────────────────────────

    /**
     * Returns the nutrition value of a crop.
     * Use this in a future nutrition subpack.
     * Returns 0 if the crop is not registered.
     */
    public static int getNutrition(String cropId) {
        if (!CropRegistry.INSTANCE.isRegistered(cropId)) return 0;
        return CropRegistry.INSTANCE.getByName(cropId).nutrition();
    }

    /**
     * Returns the hydration value of a crop.
     * Use this in the Hydration subpack (subpack 3).
     * Returns 0 if the crop is not registered.
     */
    public static int getHydration(String cropId) {
        if (!CropRegistry.INSTANCE.isRegistered(cropId)) return 0;
        return CropRegistry.INSTANCE.getByName(cropId).hydration();
    }

    // ── Food queries ──────────────────────────────────────────────────────────

    /**
     * Returns true if the given item ID is registered as a spoilable food.
     * Checks both FoodRegistry and CropRegistry.
     * Use this before calling any food query method.
     */
    public static boolean isFoodRegistered(String itemId) {
        return FoodRegistry.INSTANCE.isRegistered(itemId)
                || CropRegistry.INSTANCE.isRegistered(itemId);
    }

    /**
     * Returns the spoil days for a food item.
     * Checks FoodRegistry first then CropRegistry.
     * Returns 0 if not registered in either.
     */
    public static int getSpoilDays(String itemId) {
        if (FoodRegistry.INSTANCE.isRegistered(itemId)) {
            return FoodRegistry.INSTANCE.getByName(itemId).spoilDays();
        }
        if (CropRegistry.INSTANCE.isRegistered(itemId)) {
            return CropRegistry.INSTANCE.getByName(itemId).spoilDays();
        }
        return 0;
    }

    /**
     * Returns the weight in kg for a food item.
     * Checks FoodRegistry first then CropRegistry.
     * Returns 0.0 if not registered in either.
     */
    public static float getFoodWeightKg(String itemId) {
        if (FoodRegistry.INSTANCE.isRegistered(itemId)) {
            return FoodRegistry.INSTANCE.getByName(itemId).weightKg();
        }
        if (CropRegistry.INSTANCE.isRegistered(itemId)) {
            return CropRegistry.INSTANCE.getByName(itemId).weightKg();
        }
        return 0.0f;
    }

    /**
     * Returns the total stack weight in kg for any food item stack.
     * Accounts for stack size.
     * Returns 0.0 if not registered in either registry.
     */
    public static float getFoodStackWeightKg(ItemStack stack) {
        String itemId = stack.getItem()
                .builtInRegistryHolder()
                .key().location().getPath();
        return getFoodWeightKg(itemId) * stack.getCount();
    }
}