package net.enderwish.Farming_Overhaul_Subpack.core.cutting_board;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record CuttingBoardRecipe(
        boolean shaped,
        String toolType,
        String containerType,
        String category,
        int chopTimeTicks,
        float spoilReduction,
        List<String> pattern,
        Map<String, String> keys,
        List<String> ingredients,
        RecipeResult result,
        Optional<RecipeResult> secondaryResult,
        int secondaryMin,
        int secondaryMax
) {

    public static final Codec<CuttingBoardRecipe> CODEC =
            RecordCodecBuilder.create(instance ->
                    instance.group(
                            Codec.BOOL.fieldOf("shaped")
                                    .forGetter(CuttingBoardRecipe::shaped),
                            Codec.STRING.optionalFieldOf("tool_type", "any")
                                    .forGetter(CuttingBoardRecipe::toolType),
                            Codec.STRING.optionalFieldOf("container_type", "none")
                                    .forGetter(CuttingBoardRecipe::containerType),
                            Codec.STRING.optionalFieldOf("category", "CHOP")
                                    .forGetter(CuttingBoardRecipe::category),
                            Codec.INT.fieldOf("chop_time_ticks")
                                    .forGetter(CuttingBoardRecipe::chopTimeTicks),
                            Codec.FLOAT.optionalFieldOf("spoil_reduction", 0.0f)
                                    .forGetter(CuttingBoardRecipe::spoilReduction),
                            Codec.STRING.listOf()
                                    .optionalFieldOf("pattern", List.of())
                                    .forGetter(CuttingBoardRecipe::pattern),
                            Codec.unboundedMap(Codec.STRING, Codec.STRING)
                                    .optionalFieldOf("keys", Map.of())
                                    .forGetter(CuttingBoardRecipe::keys),
                            Codec.STRING.listOf()
                                    .optionalFieldOf("ingredients", List.of())
                                    .forGetter(CuttingBoardRecipe::ingredients),
                            RecipeResult.CODEC.fieldOf("result")
                                    .forGetter(CuttingBoardRecipe::result),
                            RecipeResult.CODEC.optionalFieldOf("secondary_result")
                                    .forGetter(CuttingBoardRecipe::secondaryResult),
                            Codec.INT.optionalFieldOf("secondary_min", 1)
                                    .forGetter(CuttingBoardRecipe::secondaryMin),
                            Codec.INT.optionalFieldOf("secondary_max", 1)
                                    .forGetter(CuttingBoardRecipe::secondaryMax)
                    ).apply(instance, CuttingBoardRecipe::new)
            );

    // ── Category ──────────────────────────────────────────────────────────────

    public enum CBCategory {
        CHOP, PEEL, MEAT, SALAD;

        public String displayName() {
            return switch (this) {
                case CHOP  -> "Chop";
                case PEEL  -> "Peel";
                case MEAT  -> "Meat";
                case SALAD -> "Salad";
            };
        }
    }

    public CBCategory getCategory() {
        try {
            return CBCategory.valueOf(category);
        } catch (IllegalArgumentException e) {
            return CBCategory.CHOP;
        }
    }

    // ── Tool Match ────────────────────────────────────────────────────────────

    public enum ToolMatch { PERFECT, CROSS_TOOL, NONE }

    public ToolMatch getToolMatch(ItemStack tool) {
        if (toolType.equals("any")) return ToolMatch.PERFECT;
        if (tool.isEmpty()) return ToolMatch.NONE;

        String id = BuiltInRegistries.ITEM
                .getKey(tool.getItem()).toString();
        boolean isKnife   = id.contains("knife");
        boolean isCleaver = id.contains("cleaver");

        return switch (toolType) {
            case "knife"   -> isKnife   ? ToolMatch.PERFECT
                    : (isCleaver ? ToolMatch.CROSS_TOOL : ToolMatch.NONE);
            case "cleaver" -> isCleaver ? ToolMatch.PERFECT
                    : (isKnife ? ToolMatch.CROSS_TOOL : ToolMatch.NONE);
            default        -> ToolMatch.NONE;
        };
    }

    // ── Container Match ───────────────────────────────────────────────────────

    public boolean matchesContainer(ItemStack container) {
        return switch (containerType) {
            case "none"   -> true;
            case "bowl"   -> !container.isEmpty() && container.is(Items.BOWL);
            case "bundle" -> !container.isEmpty()
                    && BuiltInRegistries.ITEM
                    .getKey(container.getItem())
                    .toString().contains("bundle");
            case "any"    -> !container.isEmpty();
            default       -> false;
        };
    }

    // ── Grid Matching ─────────────────────────────────────────────────────────

    public boolean matchesGrid(List<ItemStack> grid) {
        return shaped ? matchesShaped(grid) : matchesShapeless(grid);
    }

    private boolean matchesShaped(List<ItemStack> grid) {
        if (pattern.isEmpty() || keys.isEmpty()) return false;
        for (int row = 0; row < 3; row++) {
            String patternRow = row < pattern.size()
                    ? pattern.get(row) : "   ";
            while (patternRow.length() < 3) patternRow += " ";
            for (int col = 0; col < 3; col++) {
                int slotIndex = row * 3 + col;
                char key = patternRow.charAt(col);
                ItemStack stack = grid.get(slotIndex);
                if (key == ' ') {
                    if (!stack.isEmpty()) return false;
                } else {
                    String expected = keys.get(String.valueOf(key));
                    if (expected == null || stack.isEmpty()) return false;
                    if (!BuiltInRegistries.ITEM
                            .getKey(stack.getItem())
                            .toString().equals(expected)) return false;
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

    // ── Outputs ───────────────────────────────────────────────────────────────

    public ItemStack getOutput() { return result.toItemStack(); }

    public ItemStack getSecondaryOutput(boolean isCrossTool,
                                        net.minecraft.util.RandomSource random) {
        return secondaryResult.map(r -> {
            int count = isCrossTool
                    ? secondaryMin
                    : secondaryMin + random.nextInt(
                    Math.max(1, secondaryMax - secondaryMin + 1));
            return r.toItemStackWithCount(count);
        }).orElse(ItemStack.EMPTY);
    }

    // ── RecipeResult ──────────────────────────────────────────────────────────

    public record RecipeResult(String item, int count) {

        public static final Codec<RecipeResult> CODEC =
                RecordCodecBuilder.create(instance ->
                        instance.group(
                                Codec.STRING.fieldOf("item")
                                        .forGetter(RecipeResult::item),
                                Codec.INT.optionalFieldOf("count", 1)
                                        .forGetter(RecipeResult::count)
                        ).apply(instance, RecipeResult::new));

        public ItemStack toItemStack() {
            return new ItemStack(
                    BuiltInRegistries.ITEM.get(
                            ResourceLocation.parse(item)), count);
        }

        public ItemStack toItemStackWithCount(int overrideCount) {
            return new ItemStack(
                    BuiltInRegistries.ITEM.get(
                            ResourceLocation.parse(item)), overrideCount);
        }
    }
}