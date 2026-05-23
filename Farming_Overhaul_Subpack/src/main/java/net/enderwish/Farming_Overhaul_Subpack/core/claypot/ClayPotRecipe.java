package net.enderwish.Farming_Overhaul_Subpack.core.claypot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;

/**
 * ClayPotRecipe
 *
 * Defines a single clay pot recipe loaded from JSON.
 *
 * Shaped example:
 * {
 *   "shaped": true,
 *   "requires_water": true,
 *   "cook_time_ticks": 400,
 *   "spoil_reduction": 0.15,
 *   "pattern": ["A B", "   ", "   "],
 *   "keys": { "A": "minecraft:beef", "B": "minecraft:carrot" },
 *   "result": { "item": "minecraft:mushroom_stew", "count": 1 }
 * }
 *
 * Shapeless example:
 * {
 *   "shaped": false,
 *   "requires_water": false,
 *   "cook_time_ticks": 200,
 *   "spoil_reduction": 0.10,
 *   "ingredients": ["minecraft:beef", "minecraft:beef"],
 *   "result": { "item": "minecraft:cooked_beef", "count": 2 }
 * }
 */
public record ClayPotRecipe(
        boolean shaped,
        boolean requiresWater,
        int cookTimeTicks,
        float spoilReduction,
        List<String> pattern,       // 3 strings of length 3 for shaped, empty for shapeless
        Map<String, String> keys,   // char → item id mapping for shaped
        List<String> ingredients,   // item ids for shapeless
        RecipeResult result
) {

    // ── Codec ─────────────────────────────────────────────────────────────────

    public static final Codec<ClayPotRecipe> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.BOOL.fieldOf("shaped").forGetter(ClayPotRecipe::shaped),
                    Codec.BOOL.fieldOf("requires_water").forGetter(ClayPotRecipe::requiresWater),
                    Codec.INT.fieldOf("cook_time_ticks").forGetter(ClayPotRecipe::cookTimeTicks),
                    Codec.FLOAT.fieldOf("spoil_reduction").forGetter(ClayPotRecipe::spoilReduction),
                    Codec.STRING.listOf().optionalFieldOf("pattern", List.of())
                            .forGetter(ClayPotRecipe::pattern),
                    Codec.unboundedMap(Codec.STRING, Codec.STRING)
                            .optionalFieldOf("keys", Map.of())
                            .forGetter(ClayPotRecipe::keys),
                    Codec.STRING.listOf().optionalFieldOf("ingredients", List.of())
                            .forGetter(ClayPotRecipe::ingredients),
                    RecipeResult.CODEC.fieldOf("result").forGetter(ClayPotRecipe::result)
            ).apply(instance, ClayPotRecipe::new)
    );

    // ── Matching ──────────────────────────────────────────────────────────────

    /**
     * Checks if the given 9-slot ingredient grid matches this recipe.
     * grid[0] = top-left, grid[8] = bottom-right.
     */
    public boolean matches(List<ItemStack> grid, int waterLevel) {
        if (requiresWater && waterLevel <= 0) return false;

        if (shaped) {
            return matchesShaped(grid);
        } else {
            return matchesShapeless(grid);
        }
    }

    private boolean matchesShaped(List<ItemStack> grid) {
        if (pattern.isEmpty() || keys.isEmpty()) return false;

        for (int row = 0; row < 3; row++) {
            String patternRow = row < pattern.size() ? pattern.get(row) : "   ";
            // Pad to length 3
            while (patternRow.length() < 3) patternRow = patternRow + " ";

            for (int col = 0; col < 3; col++) {
                int slotIndex = row * 3 + col;
                char key = patternRow.charAt(col);
                ItemStack stack = grid.get(slotIndex);

                if (key == ' ') {
                    // Slot must be empty
                    if (!stack.isEmpty()) return false;
                } else {
                    // Slot must have the correct item
                    String expectedId = keys.get(String.valueOf(key));
                    if (expectedId == null) return false;
                    if (stack.isEmpty()) return false;
                    String actualId = BuiltInRegistries.ITEM
                            .getKey(stack.getItem()).toString();
                    if (!actualId.equals(expectedId)) return false;
                }
            }
        }
        return true;
    }

    private boolean matchesShapeless(List<ItemStack> grid) {
        if (ingredients.isEmpty()) return false;

        // Count required ingredients
        List<String> required = new java.util.ArrayList<>(ingredients);

        for (ItemStack stack : grid) {
            if (stack.isEmpty()) continue;
            String id = BuiltInRegistries.ITEM
                    .getKey(stack.getItem()).toString();
            if (!required.remove(id)) return false; // unexpected item
        }

        return required.isEmpty(); // all required items matched
    }

    // ── Result ────────────────────────────────────────────────────────────────

    /**
     * Returns the output ItemStack for this recipe.
     */
    public ItemStack getOutput() {
        return result.toItemStack();
    }

    // ── RecipeResult record ───────────────────────────────────────────────────

    public record RecipeResult(String item, int count) {

        public static final Codec<RecipeResult> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.STRING.fieldOf("item").forGetter(RecipeResult::item),
                        Codec.INT.optionalFieldOf("count", 1).forGetter(RecipeResult::count)
                ).apply(instance, RecipeResult::new)
        );

        public ItemStack toItemStack() {
            ResourceLocation id = ResourceLocation.parse(item);
            return new ItemStack(
                    BuiltInRegistries.ITEM.get(id),
                    count
            );
        }
    }
}