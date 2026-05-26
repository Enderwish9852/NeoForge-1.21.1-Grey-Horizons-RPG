package net.enderwish.Farming_Overhaul_Subpack.core.claypot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;

public record ClayPotRecipe(
        boolean shaped,
        boolean requiresWater,
        int cookTimeTicks,
        float spoilReduction,
        List<String> pattern,
        Map<String, String> keys,
        List<String> ingredients,
        RecipeResult result
) {

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

    /**
     * Checks if the given 9-slot ingredient grid matches this recipe.
     * waterSlot is the ItemStack in the water input slot.
     */
    public boolean matches(List<ItemStack> grid, ItemStack waterSlot) {
        // Check water requirement
        if (requiresWater && !waterSlot.is(Items.WATER_BUCKET)) return false;

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
            while (patternRow.length() < 3) patternRow = patternRow + " ";

            for (int col = 0; col < 3; col++) {
                int slotIndex = row * 3 + col;
                char key = patternRow.charAt(col);
                ItemStack stack = grid.get(slotIndex);

                if (key == ' ') {
                    if (!stack.isEmpty()) return false;
                } else {
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

        List<String> required = new java.util.ArrayList<>(ingredients);

        for (ItemStack stack : grid) {
            if (stack.isEmpty()) continue;
            String id = BuiltInRegistries.ITEM
                    .getKey(stack.getItem()).toString();
            if (!required.remove(id)) return false;
        }

        return required.isEmpty();
    }

    public ItemStack getOutput() {
        return result.toItemStack();
    }

    public record RecipeResult(String item, int count) {

        public static final Codec<RecipeResult> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.STRING.fieldOf("item").forGetter(RecipeResult::item),
                        Codec.INT.optionalFieldOf("count", 1).forGetter(RecipeResult::count)
                ).apply(instance, RecipeResult::new)
        );

        public ItemStack toItemStack() {
            ResourceLocation id = ResourceLocation.parse(item);
            return new ItemStack(BuiltInRegistries.ITEM.get(id), count);
        }
    }
}