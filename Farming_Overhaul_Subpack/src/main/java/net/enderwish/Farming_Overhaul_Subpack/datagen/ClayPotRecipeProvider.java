package net.enderwish.Farming_Overhaul_Subpack.datagen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ClayPotRecipeProvider implements DataProvider {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final PackOutput output;

    public ClayPotRecipeProvider(PackOutput output) {
        this.output = output;
    }

    // ── Recipe builder ────────────────────────────────────────────────────────

    record CPRecipe(
            String name,
            boolean shaped,
            boolean requiresWater,
            int cookTimeTicks,
            float spoilReduction,
            String category,
            List<String> ingredients,
            List<String> pattern,
            java.util.Map<String, String> keys,
            String resultItem,
            int resultCount
    ) {}

    private final List<CPRecipe> recipes = new ArrayList<>();

    private void shapeless(String name, boolean water, int ticks,
                           float spoil, String category,
                           List<String> ingredients,
                           String result, int count) {
        recipes.add(new CPRecipe(name, false, water, ticks, spoil,
                category, ingredients, List.of(),
                java.util.Map.of(), result, count));
    }

    private void shaped(String name, boolean water, int ticks,
                        float spoil, String category,
                        List<String> pattern,
                        java.util.Map<String, String> keys,
                        String result, int count) {
        recipes.add(new CPRecipe(name, true, water, ticks, spoil,
                category, List.of(), pattern, keys, result, count));
    }

    // ── Register all recipes ──────────────────────────────────────────────────

    private void registerAll() {

        // ════════════════════════════════════════════════════════════════════
        // SOUPS
        // ════════════════════════════════════════════════════════════════════

        shapeless("vegetable_broth",  true, 300, 0.05f, "SOUP",
                List.of("gh_farming_overhaul:chopped_carrot"),
                "gh_farming_overhaul:vegetable_broth", 1);

        shapeless("carrot_soup",      true, 400, 0.05f, "SOUP",
                List.of("gh_farming_overhaul:chopped_carrot",
                        "gh_farming_overhaul:chopped_carrot"),
                "gh_farming_overhaul:carrot_soup", 1);

        shapeless("garlic_broth",     true, 300, 0.08f, "SOUP",
                List.of("gh_farming_overhaul:minced_garlic"),
                "gh_farming_overhaul:garlic_broth", 1);

        shapeless("potato_soup",      true, 500, 0.08f, "SOUP",
                List.of("gh_farming_overhaul:diced_potato",
                        "gh_farming_overhaul:diced_potato"),
                "gh_farming_overhaul:potato_soup", 1);

        shapeless("tomato_soup",      true, 500, 0.08f, "SOUP",
                List.of("gh_farming_overhaul:chopped_tomato",
                        "gh_farming_overhaul:chopped_tomato"),
                "gh_farming_overhaul:tomato_soup", 1);

        shapeless("mushroom_soup",    true, 500, 0.08f, "SOUP",
                List.of("minecraft:brown_mushroom",
                        "minecraft:brown_mushroom"),
                "gh_farming_overhaul:mushroom_soup", 1);

        shapeless("beetroot_soup",    true, 500, 0.08f, "SOUP",
                List.of("gh_farming_overhaul:chopped_beetroot",
                        "gh_farming_overhaul:chopped_beetroot"),
                "gh_farming_overhaul:beetroot_soup", 1);

        shapeless("cabbage_soup",     true, 600, 0.08f, "SOUP",
                List.of("gh_farming_overhaul:shredded_cabbage",
                        "gh_farming_overhaul:chopped_carrot"),
                "gh_farming_overhaul:cabbage_soup", 1);

        shapeless("sweet_potato_soup",true, 600, 0.08f, "SOUP",
                List.of("gh_farming_overhaul:cubed_sweet_potato",
                        "gh_farming_overhaul:cubed_sweet_potato"),
                "gh_farming_overhaul:sweet_potato_soup", 1);

        shapeless("onion_soup",       true, 700, 0.10f, "SOUP",
                List.of("gh_farming_overhaul:diced_onion",
                        "gh_farming_overhaul:diced_onion",
                        "gh_farming_overhaul:bread_crumbs"),
                "gh_farming_overhaul:onion_soup", 1);

        shapeless("bread_soup",       true, 300, 0.05f, "SOUP",
                List.of("minecraft:bread"),
                "gh_farming_overhaul:bread_soup", 1);

        shapeless("corn_chowder",     true, 700, 0.10f, "SOUP",
                List.of("gh_farming_overhaul:corn_kernels",
                        "gh_farming_overhaul:diced_potato"),
                "gh_farming_overhaul:corn_chowder", 1);

        shapeless("fish_soup",        true, 700, 0.10f, "SOUP",
                List.of("gh_farming_overhaul:fish_chunks",
                        "gh_farming_overhaul:chopped_carrot"),
                "gh_farming_overhaul:fish_soup", 1);

        shapeless("chicken_broth",    true, 800, 0.10f, "SOUP",
                List.of("gh_farming_overhaul:diced_chicken",
                        "gh_farming_overhaul:minced_garlic",
                        "gh_farming_overhaul:chopped_carrot"),
                "gh_farming_overhaul:chicken_broth", 1);

        shapeless("cucumber_gazpacho",false, 400, 0.05f, "SOUP",
                List.of("gh_farming_overhaul:sliced_cucumber",
                        "gh_farming_overhaul:chopped_tomato",
                        "gh_farming_overhaul:diced_onion"),
                "gh_farming_overhaul:cucumber_gazpacho", 1);

        shapeless("minestrone",       true, 1000, 0.12f, "SOUP",
                List.of("gh_farming_overhaul:chopped_tomato",
                        "gh_farming_overhaul:diced_potato",
                        "gh_farming_overhaul:chopped_carrot",
                        "gh_farming_overhaul:chopped_broccoli"),
                "gh_farming_overhaul:minestrone", 1);

        // ════════════════════════════════════════════════════════════════════
        // STEWS
        // ════════════════════════════════════════════════════════════════════

        shapeless("simple_meat_stew",  true, 600,  0.10f, "STEW",
                List.of("gh_farming_overhaul:cubed_beef",
                        "gh_farming_overhaul:diced_potato"),
                "gh_farming_overhaul:simple_meat_stew", 1);

        shapeless("pork_stew",         true, 700,  0.10f, "STEW",
                List.of("gh_farming_overhaul:cubed_pork",
                        "gh_farming_overhaul:diced_potato"),
                "gh_farming_overhaul:pork_stew", 1);

        shapeless("chicken_stew",      true, 700,  0.10f, "STEW",
                List.of("gh_farming_overhaul:diced_chicken",
                        "gh_farming_overhaul:diced_potato"),
                "gh_farming_overhaul:chicken_stew", 1);

        shapeless("fish_stew",         true, 700,  0.10f, "STEW",
                List.of("gh_farming_overhaul:fish_fillet",
                        "gh_farming_overhaul:diced_potato"),
                "gh_farming_overhaul:fish_stew", 1);

        shapeless("sweet_potato_stew", true, 800,  0.12f, "STEW",
                List.of("gh_farming_overhaul:cubed_sweet_potato",
                        "gh_farming_overhaul:cubed_beef"),
                "gh_farming_overhaul:sweet_potato_stew", 1);

        shapeless("mixed_veg_stew",    true, 800,  0.10f, "STEW",
                List.of("gh_farming_overhaul:chopped_carrot",
                        "gh_farming_overhaul:diced_potato",
                        "gh_farming_overhaul:chopped_tomato"),
                "gh_farming_overhaul:mixed_veg_stew", 1);

        shapeless("lamb_stew",         true, 900,  0.12f, "STEW",
                List.of("gh_farming_overhaul:cubed_mutton",
                        "gh_farming_overhaul:chopped_carrot",
                        "gh_farming_overhaul:diced_potato"),
                "gh_farming_overhaul:lamb_stew", 1);

        shapeless("beef_carrot_stew",  true, 900,  0.12f, "STEW",
                List.of("gh_farming_overhaul:cubed_beef",
                        "gh_farming_overhaul:chopped_carrot",
                        "gh_farming_overhaul:diced_potato"),
                "gh_farming_overhaul:beef_carrot_stew", 1);

        shapeless("broccoli_pork_stew",true, 900,  0.12f, "STEW",
                List.of("gh_farming_overhaul:cubed_pork",
                        "gh_farming_overhaul:chopped_broccoli",
                        "gh_farming_overhaul:diced_onion"),
                "gh_farming_overhaul:broccoli_pork_stew", 1);

        shapeless("corn_chicken_stew", true, 900,  0.12f, "STEW",
                List.of("gh_farming_overhaul:corn_kernels",
                        "gh_farming_overhaul:diced_chicken",
                        "gh_farming_overhaul:diced_onion"),
                "gh_farming_overhaul:corn_chicken_stew", 1);

        shapeless("enhanced_rabbit_stew",true,900, 0.12f, "STEW",
                List.of("gh_farming_overhaul:diced_rabbit",
                        "gh_farming_overhaul:diced_potato",
                        "gh_farming_overhaul:chopped_carrot"),
                "gh_farming_overhaul:enhanced_rabbit_stew", 1);

        shapeless("aubergine_stew",    true, 1000, 0.12f, "STEW",
                List.of("gh_farming_overhaul:aubergine",
                        "gh_farming_overhaul:chopped_tomato",
                        "gh_farming_overhaul:diced_onion"),
                "gh_farming_overhaul:aubergine_stew", 1);

        shapeless("tomato_beef_stew",  true, 1000, 0.15f, "STEW",
                List.of("gh_farming_overhaul:cubed_beef",
                        "gh_farming_overhaul:chopped_tomato",
                        "gh_farming_overhaul:diced_onion",
                        "gh_farming_overhaul:minced_garlic"),
                "gh_farming_overhaul:tomato_beef_stew", 1);

        shapeless("goulash",           true, 1100, 0.15f, "STEW",
                List.of("gh_farming_overhaul:cubed_beef",
                        "gh_farming_overhaul:chopped_bell_pepper",
                        "gh_farming_overhaul:diced_onion",
                        "gh_farming_overhaul:chopped_tomato"),
                "gh_farming_overhaul:goulash", 1);

        // Ratatouille — shaped recipe (layered arrangement)
        shaped("ratatouille", false, 1200, 0.15f, "STEW",
                List.of("AB ", "CD ", "   "),
                java.util.Map.of(
                        "A", "gh_farming_overhaul:sliced_courgette",
                        "B", "gh_farming_overhaul:chopped_tomato",
                        "C", "gh_farming_overhaul:chopped_bell_pepper",
                        "D", "gh_farming_overhaul:diced_onion"),
                "gh_farming_overhaul:ratatouille", 1);

        // ════════════════════════════════════════════════════════════════════
        // PRESERVES
        // ════════════════════════════════════════════════════════════════════

        shapeless("pickled_cucumber",   false, 400,  0.35f, "PRESERVE",
                List.of("gh_farming_overhaul:sliced_cucumber"),
                "gh_farming_overhaul:pickled_cucumber", 2);

        shapeless("pickled_carrot",     false, 400,  0.35f, "PRESERVE",
                List.of("gh_farming_overhaul:chopped_carrot"),
                "gh_farming_overhaul:pickled_carrot", 2);

        shapeless("sauerkraut",         false, 600,  0.45f, "PRESERVE",
                List.of("gh_farming_overhaul:shredded_cabbage",
                        "gh_farming_overhaul:shredded_cabbage"),
                "gh_farming_overhaul:sauerkraut", 2);

        shapeless("preserved_beetroot", false, 500,  0.40f, "PRESERVE",
                List.of("gh_farming_overhaul:chopped_beetroot",
                        "gh_farming_overhaul:chopped_beetroot"),
                "gh_farming_overhaul:preserved_beetroot", 2);

        shapeless("salted_fish",        false, 500,  0.45f, "PRESERVE",
                List.of("gh_farming_overhaul:fish_fillet"),
                "gh_farming_overhaul:salted_fish", 1);

        shapeless("salted_meat",        false, 500,  0.45f, "PRESERVE",
                List.of("gh_farming_overhaul:beef_strips"),
                "gh_farming_overhaul:salted_meat", 1);

        shapeless("garlic_paste",       false, 500,  0.40f, "PRESERVE",
                List.of("gh_farming_overhaul:minced_garlic",
                        "gh_farming_overhaul:minced_garlic",
                        "gh_farming_overhaul:minced_garlic"),
                "gh_farming_overhaul:garlic_paste", 1);

        shapeless("tomato_concentrate", false, 600,  0.40f, "PRESERVE",
                List.of("gh_farming_overhaul:tomato_paste",
                        "gh_farming_overhaul:tomato_paste",
                        "gh_farming_overhaul:tomato_paste"),
                "gh_farming_overhaul:tomato_concentrate", 1);

        shapeless("preserved_tomatoes", false, 700,  0.40f, "PRESERVE",
                List.of("gh_farming_overhaul:chopped_tomato",
                        "gh_farming_overhaul:chopped_tomato",
                        "gh_farming_overhaul:minced_garlic"),
                "gh_farming_overhaul:preserved_tomatoes", 2);

        shapeless("pickled_veg_mix",    false, 600,  0.40f, "PRESERVE",
                List.of("gh_farming_overhaul:chopped_carrot",
                        "gh_farming_overhaul:sliced_cucumber",
                        "gh_farming_overhaul:chopped_beetroot"),
                "gh_farming_overhaul:pickled_veg_mix", 2);

        shapeless("garlic_confit",      false, 800,  0.45f, "PRESERVE",
                List.of("gh_farming_overhaul:garlic",
                        "gh_farming_overhaul:garlic",
                        "gh_farming_overhaul:minced_garlic"),
                "gh_farming_overhaul:garlic_confit", 1);

        shapeless("onion_jam",          false, 900,  0.45f, "PRESERVE",
                List.of("gh_farming_overhaul:diced_onion",
                        "gh_farming_overhaul:diced_onion",
                        "minecraft:honey_bottle"),
                "gh_farming_overhaul:onion_jam", 1);

        shapeless("apple_preserve",     false, 900,  0.45f, "PRESERVE",
                List.of("gh_farming_overhaul:apple_slices",
                        "gh_farming_overhaul:apple_slices",
                        "minecraft:honey_bottle"),
                "gh_farming_overhaul:apple_preserve", 1);

        shapeless("berry_preserve",     false, 900,  0.45f, "PRESERVE",
                List.of("minecraft:sweet_berries",
                        "minecraft:sweet_berries",
                        "minecraft:honey_bottle"),
                "gh_farming_overhaul:berry_preserve", 1);

        shapeless("fruit_compote",      false, 1000, 0.40f, "PRESERVE",
                List.of("gh_farming_overhaul:sliced_peach",
                        "gh_farming_overhaul:sliced_pear",
                        "minecraft:honey_bottle"),
                "gh_farming_overhaul:fruit_compote", 1);

        // ════════════════════════════════════════════════════════════════════
        // OTHER
        // ════════════════════════════════════════════════════════════════════

        shapeless("herb_infusion",       true,  200, 0.05f, "OTHER",
                List.of("gh_farming_overhaul:minced_garlic"),
                "gh_farming_overhaul:herb_infusion", 1);

        shapeless("corn_porridge",       true,  400, 0.05f, "OTHER",
                List.of("gh_farming_overhaul:corn_kernels"),
                "gh_farming_overhaul:corn_porridge", 1);

        shapeless("oat_porridge",        true,  400, 0.05f, "OTHER",
                List.of("gh_farming_overhaul:bread_crumbs",
                        "gh_farming_overhaul:bread_crumbs"),
                "gh_farming_overhaul:oat_porridge", 1);

        shapeless("cooked_corn",         true,  400, 0.05f, "OTHER",
                List.of("gh_farming_overhaul:corn"),
                "gh_farming_overhaul:cooked_corn", 1);

        shapeless("mushroom_medley",     false, 400, 0.08f, "OTHER",
                List.of("minecraft:brown_mushroom",
                        "minecraft:red_mushroom"),
                "gh_farming_overhaul:mushroom_medley", 1);

        shapeless("scrambled_eggs",      false, 300, 0.05f, "OTHER",
                List.of("minecraft:egg", "minecraft:egg"),
                "gh_farming_overhaul:scrambled_eggs", 1);

        shapeless("sweet_potato_mash",   true,  500, 0.08f, "OTHER",
                List.of("gh_farming_overhaul:cubed_sweet_potato",
                        "gh_farming_overhaul:cubed_sweet_potato"),
                "gh_farming_overhaul:sweet_potato_mash", 1);

        shapeless("caramelized_onion",   false, 600, 0.10f, "OTHER",
                List.of("gh_farming_overhaul:diced_onion",
                        "gh_farming_overhaul:diced_onion",
                        "minecraft:honey_bottle"),
                "gh_farming_overhaul:caramelized_onion", 1);

        shapeless("eggs_with_veg",       false, 500, 0.08f, "OTHER",
                List.of("minecraft:egg",
                        "gh_farming_overhaul:diced_onion",
                        "gh_farming_overhaul:chopped_tomato"),
                "gh_farming_overhaul:eggs_with_veg", 1);

        shapeless("steamed_vegetables",  true,  500, 0.08f, "OTHER",
                List.of("gh_farming_overhaul:chopped_broccoli",
                        "gh_farming_overhaul:chopped_carrot",
                        "gh_farming_overhaul:sliced_courgette"),
                "gh_farming_overhaul:steamed_vegetables", 1);

        shapeless("honey_glazed_veg",    false, 600, 0.10f, "OTHER",
                List.of("gh_farming_overhaul:chopped_carrot",
                        "gh_farming_overhaul:cubed_sweet_potato",
                        "minecraft:honey_bottle"),
                "gh_farming_overhaul:honey_glazed_veg", 1);

        shapeless("fruit_salad",         false, 200, 0.05f, "OTHER",
                List.of("gh_farming_overhaul:apple_slices",
                        "gh_farming_overhaul:sliced_pear",
                        "gh_farming_overhaul:melon_chunks"),
                "gh_farming_overhaul:fruit_salad", 1);

        // Garlic bread — shaped
        shaped("garlic_bread", false, 300, 0.08f, "OTHER",
                List.of("AB ", "   ", "   "),
                java.util.Map.of(
                        "A", "minecraft:bread",
                        "B", "gh_farming_overhaul:minced_garlic"),
                "gh_farming_overhaul:garlic_bread", 1);

        // Stuffed cabbage — shaped
        shaped("stuffed_cabbage", true, 900, 0.12f, "OTHER",
                List.of("A  ", "BC ", "   "),
                java.util.Map.of(
                        "A", "gh_farming_overhaul:cabbage_leaf",
                        "B", "gh_farming_overhaul:minced_beef",
                        "C", "gh_farming_overhaul:diced_onion"),
                "gh_farming_overhaul:stuffed_cabbage", 1);
    }

    // ── DataProvider ──────────────────────────────────────────────────────────

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        registerAll();

        List<CompletableFuture<?>> futures = new ArrayList<>();
        Path folder = output.getOutputFolder()
                .resolve("data/gh_farming_overhaul/clay_pot_recipes");

        for (CPRecipe recipe : recipes) {
            JsonObject json = new JsonObject();
            json.addProperty("shaped", recipe.shaped());
            json.addProperty("requires_water", recipe.requiresWater());
            json.addProperty("cook_time_ticks", recipe.cookTimeTicks());
            json.addProperty("spoil_reduction", recipe.spoilReduction());
            json.addProperty("category", recipe.category());

            if (recipe.shaped()) {
                // Pattern
                JsonArray pattern = new JsonArray();
                for (String row : recipe.pattern()) pattern.add(row);
                json.add("pattern", pattern);
                // Keys
                JsonObject keys = new JsonObject();
                recipe.keys().forEach(keys::addProperty);
                json.add("keys", keys);
            } else {
                // Ingredients
                JsonArray ingredients = new JsonArray();
                for (String ing : recipe.ingredients()) ingredients.add(ing);
                json.add("ingredients", ingredients);
            }

            // Result
            JsonObject result = new JsonObject();
            result.addProperty("item", recipe.resultItem());
            result.addProperty("count", recipe.resultCount());
            json.add("result", result);

            Path path = folder.resolve(recipe.name() + ".json");
            futures.add(DataProvider.saveStable(cache, GSON.toJsonTree(json), path));
        }

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "GH Clay Pot Recipes";
    }
}