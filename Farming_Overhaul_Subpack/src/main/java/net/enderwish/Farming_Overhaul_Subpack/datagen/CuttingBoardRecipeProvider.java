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

public class CuttingBoardRecipeProvider implements DataProvider {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final PackOutput output;

    public CuttingBoardRecipeProvider(PackOutput output) {
        this.output = output;
    }

    record CBRecipe(
            String name,
            boolean shaped,
            String toolType,
            String containerType,
            String category,
            int chopTimeTicks,
            float spoilReduction,
            List<String> ingredients,
            String resultItem,
            int resultCount,
            String secondaryItem,
            int secondaryMin,
            int secondaryMax
    ) {}

    private final List<CBRecipe> recipes = new ArrayList<>();

    private void shapeless(String name, String tool, String container,
                           int ticks, String category,
                           List<String> ingredients,
                           String result, int count) {
        recipes.add(new CBRecipe(name, false, tool, container, category,
                ticks, 0.0f, ingredients, result, count, null, 0, 0));
    }

    private void shapelessWithPeel(String name, String tool, int ticks,
                                   String category,
                                   List<String> ingredients,
                                   String result, int count,
                                   String secondary, int min, int max) {
        recipes.add(new CBRecipe(name, false, tool, "none", category,
                ticks, 0.0f, ingredients, result, count, secondary, min, max));
    }

    private void registerAll() {

        // ── Knife — Chopping Vegetables ───────────────────────────────────────
        shapeless("chopped_tomato",      "knife","none", 40, "CHOP", List.of("gh_farming_overhaul:tomato"),       "gh_farming_overhaul:chopped_tomato",      2);
        shapeless("chopped_bell_pepper", "knife","none", 40, "CHOP", List.of("gh_farming_overhaul:bell_pepper"),  "gh_farming_overhaul:chopped_bell_pepper", 2);
        shapeless("chopped_broccoli",    "knife","none", 40, "CHOP", List.of("gh_farming_overhaul:broccoli"),     "gh_farming_overhaul:chopped_broccoli",    2);
        shapeless("corn_kernels",        "knife","none", 60, "CHOP", List.of("gh_farming_overhaul:corn"),         "gh_farming_overhaul:corn_kernels",        3);
        shapeless("sliced_courgette",    "knife","none", 40, "CHOP", List.of("gh_farming_overhaul:courgette"),    "gh_farming_overhaul:sliced_courgette",    3);
        shapeless("sliced_cucumber",     "knife","none", 40, "CHOP", List.of("gh_farming_overhaul:cucumber"),     "gh_farming_overhaul:sliced_cucumber",     3);
        shapeless("minced_garlic",       "knife","none", 60, "CHOP", List.of("gh_farming_overhaul:garlic"),       "gh_farming_overhaul:minced_garlic",       2);
        shapeless("shredded_lettuce",    "knife","none", 30, "CHOP", List.of("gh_farming_overhaul:lettuce"),      "gh_farming_overhaul:shredded_lettuce",    2);
        shapeless("diced_onion",         "knife","none", 50, "CHOP", List.of("gh_farming_overhaul:onion"),        "gh_farming_overhaul:diced_onion",         2);
        shapeless("onion_rings",         "knife","none", 60, "CHOP", List.of("gh_farming_overhaul:onion"),        "gh_farming_overhaul:onion_rings",         3);
        shapeless("cubed_sweet_potato",  "knife","none", 50, "CHOP", List.of("gh_farming_overhaul:sweet_potato"), "gh_farming_overhaul:cubed_sweet_potato",  2);
        shapeless("tomato_paste",        "knife","none",100, "CHOP",
                List.of("gh_farming_overhaul:tomato","gh_farming_overhaul:tomato"),
                "gh_farming_overhaul:tomato_paste", 1);
        shapelessWithPeel("shredded_cabbage","knife", 50, "CHOP",
                List.of("gh_farming_overhaul:cabbage"),
                "gh_farming_overhaul:shredded_cabbage", 2,
                "gh_farming_overhaul:cabbage_leaf", 1, 2);

        // ── Knife — Chopping Vanilla Vegetables ───────────────────────────────
        shapeless("diced_potato",    "knife","none", 50, "CHOP", List.of("minecraft:potato"),      "gh_farming_overhaul:diced_potato",    2);
        shapeless("chopped_carrot",  "knife","none", 40, "CHOP", List.of("minecraft:carrot"),      "gh_farming_overhaul:chopped_carrot",  2);
        shapeless("chopped_beetroot","knife","none", 40, "CHOP", List.of("minecraft:beetroot"),    "gh_farming_overhaul:chopped_beetroot",2);
        shapeless("bread_crumbs",    "knife","none", 40, "CHOP", List.of("minecraft:bread"),       "gh_farming_overhaul:bread_crumbs",    2);
        shapeless("melon_chunks",    "knife","none", 40, "CHOP", List.of("minecraft:melon_slice"), "gh_farming_overhaul:melon_chunks",    2);
        shapeless("apple_slices",    "knife","none", 40, "CHOP", List.of("minecraft:apple"),       "gh_farming_overhaul:apple_slices",    3);

        // ── Knife — Slicing Fruits ────────────────────────────────────────────
        shapeless("sliced_banana",        "knife","none", 30,  "CHOP", List.of("gh_farming_overhaul:banana"),    "gh_farming_overhaul:sliced_banana",        3);
        shapeless("mashed_banana",        "knife","none", 100, "CHOP",
                List.of("gh_farming_overhaul:banana","gh_farming_overhaul:banana"),
                "gh_farming_overhaul:mashed_banana", 1);
        shapeless("halved_fig",           "knife","none", 30,  "CHOP", List.of("gh_farming_overhaul:fig"),       "gh_farming_overhaul:halved_fig",           2);
        shapeless("cubed_guava",          "knife","none", 40,  "CHOP", List.of("gh_farming_overhaul:guava"),     "gh_farming_overhaul:cubed_guava",          2);
        shapeless("sliced_kiwi",          "knife","none", 40,  "CHOP", List.of("gh_farming_overhaul:kiwi"),      "gh_farming_overhaul:sliced_kiwi",          3);
        shapeless("cubed_mango",          "knife","none", 50,  "CHOP", List.of("gh_farming_overhaul:mango"),     "gh_farming_overhaul:cubed_mango",          2);
        shapeless("orange_slices",        "knife","none", 40,  "CHOP", List.of("gh_farming_overhaul:orange"),    "gh_farming_overhaul:orange_slices",        3);
        shapeless("cubed_papaya",         "knife","none", 50,  "CHOP", List.of("gh_farming_overhaul:papaya"),    "gh_farming_overhaul:cubed_papaya",         2);
        shapeless("sliced_peach",         "knife","none", 40,  "CHOP", List.of("gh_farming_overhaul:peach"),     "gh_farming_overhaul:sliced_peach",         3);
        shapeless("sliced_pear",          "knife","none", 40,  "CHOP", List.of("gh_farming_overhaul:pear"),      "gh_farming_overhaul:sliced_pear",          3);
        shapeless("sliced_persimmon",     "knife","none", 40,  "CHOP", List.of("gh_farming_overhaul:persimmon"), "gh_farming_overhaul:sliced_persimmon",     3);
        shapeless("halved_plum",          "knife","none", 30,  "CHOP", List.of("gh_farming_overhaul:plum"),      "gh_farming_overhaul:halved_plum",          2);
        shapeless("pomegranate_seeds_item","knife","none", 60, "CHOP", List.of("gh_farming_overhaul:pomegranate"),"gh_farming_overhaul:pomegranate_seeds_item",3);

        // ── Knife — Peeling ───────────────────────────────────────────────────
        shapelessWithPeel("peeled_potato",       "knife", 60, "PEEL", List.of("minecraft:potato"),                 "gh_farming_overhaul:peeled_potato",       1, "gh_farming_overhaul:potato_peel",       1, 3);
        shapelessWithPeel("peeled_carrot",       "knife", 60, "PEEL", List.of("minecraft:carrot"),                 "gh_farming_overhaul:peeled_carrot",       1, "gh_farming_overhaul:carrot_peel",       1, 3);
        shapelessWithPeel("peeled_cucumber",     "knife", 60, "PEEL", List.of("gh_farming_overhaul:cucumber"),     "gh_farming_overhaul:peeled_cucumber",     1, "gh_farming_overhaul:cucumber_peel",     1, 3);
        shapelessWithPeel("peeled_courgette",    "knife", 60, "PEEL", List.of("gh_farming_overhaul:courgette"),    "gh_farming_overhaul:peeled_courgette",    1, "gh_farming_overhaul:courgette_peel",    1, 3);
        shapelessWithPeel("peeled_sweet_potato", "knife", 60, "PEEL", List.of("gh_farming_overhaul:sweet_potato"), "gh_farming_overhaul:peeled_sweet_potato", 1, "gh_farming_overhaul:sweet_potato_peel", 1, 3);
        shapelessWithPeel("peeled_kiwi",         "knife", 60, "PEEL", List.of("gh_farming_overhaul:kiwi"),         "gh_farming_overhaul:peeled_kiwi",         1, "gh_farming_overhaul:kiwi_peel",         1, 3);
        shapelessWithPeel("peeled_mango",        "knife", 60, "PEEL", List.of("gh_farming_overhaul:mango"),        "gh_farming_overhaul:peeled_mango",        1, "gh_farming_overhaul:mango_peel",        1, 3);
        shapelessWithPeel("peeled_peach",        "knife", 60, "PEEL", List.of("gh_farming_overhaul:peach"),        "gh_farming_overhaul:peeled_peach",        1, "gh_farming_overhaul:peach_peel",        1, 3);
        shapelessWithPeel("peeled_pear",         "knife", 60, "PEEL", List.of("gh_farming_overhaul:pear"),         "gh_farming_overhaul:peeled_pear",         1, "gh_farming_overhaul:pear_peel",         1, 3);
        shapelessWithPeel("peeled_apple",        "knife", 60, "PEEL", List.of("minecraft:apple"),                  "gh_farming_overhaul:peeled_apple",        1, "gh_farming_overhaul:apple_peel",        1, 3);
        shapelessWithPeel("peeled_banana",       "knife", 30, "PEEL", List.of("gh_farming_overhaul:banana"),       "gh_farming_overhaul:sliced_banana",       3, "gh_farming_overhaul:banana_peel",       1, 1);
        shapelessWithPeel("peeled_orange",       "knife", 50, "PEEL", List.of("gh_farming_overhaul:orange"),       "gh_farming_overhaul:orange_slices",       3, "gh_farming_overhaul:orange_peel",       1, 2);

        // ── Knife — Meat ──────────────────────────────────────────────────────
        shapeless("cubed_beef",    "knife","none", 60, "MEAT", List.of("minecraft:beef"),                             "gh_farming_overhaul:cubed_beef",    2);
        shapeless("minced_beef",   "knife","none", 80, "MEAT", List.of("minecraft:beef","minecraft:beef"),            "gh_farming_overhaul:minced_beef",   3);
        shapeless("cubed_pork",    "knife","none", 60, "MEAT", List.of("minecraft:porkchop"),                         "gh_farming_overhaul:cubed_pork",    2);
        shapeless("minced_pork",   "knife","none", 80, "MEAT", List.of("minecraft:porkchop","minecraft:porkchop"),    "gh_farming_overhaul:minced_pork",   3);
        shapeless("diced_chicken", "knife","none", 60, "MEAT", List.of("minecraft:chicken"),                          "gh_farming_overhaul:diced_chicken", 2);
        shapeless("cubed_mutton",  "knife","none", 60, "MEAT", List.of("minecraft:mutton"),                           "gh_farming_overhaul:cubed_mutton",  2);
        shapeless("minced_mutton", "knife","none", 80, "MEAT", List.of("minecraft:mutton","minecraft:mutton"),        "gh_farming_overhaul:minced_mutton", 3);
        shapeless("diced_rabbit",  "knife","none", 60, "MEAT", List.of("minecraft:rabbit"),                           "gh_farming_overhaul:diced_rabbit",  2);
        shapeless("fish_fillet",   "knife","none", 60, "MEAT", List.of("minecraft:cod"),                              "gh_farming_overhaul:fish_fillet",   1);
        shapeless("fish_chunks",   "knife","none", 50, "MEAT", List.of("minecraft:cod"),                              "gh_farming_overhaul:fish_chunks",   2);
        shapeless("salmon_fillet", "knife","none", 60, "MEAT", List.of("minecraft:salmon"),                           "gh_farming_overhaul:salmon_fillet", 1);
        shapeless("salmon_chunks", "knife","none", 50, "MEAT", List.of("minecraft:salmon"),                           "gh_farming_overhaul:salmon_chunks", 2);
        shapeless("egg_wash",      "knife","bowl", 30, "MEAT", List.of("minecraft:egg","minecraft:egg"),              "gh_farming_overhaul:egg_wash",      1);

        // ── Cleaver — Large Meat Cuts ─────────────────────────────────────────
        shapeless("beef_strips",          "cleaver","none", 50, "MEAT", List.of("minecraft:beef"),      "gh_farming_overhaul:beef_strips",    3);
        shapeless("pork_strips",          "cleaver","none", 50, "MEAT", List.of("minecraft:porkchop"),  "gh_farming_overhaul:pork_strips",    3);
        shapeless("chicken_strips",       "cleaver","none", 50, "MEAT", List.of("minecraft:chicken"),   "gh_farming_overhaul:chicken_strips", 3);
        shapeless("salmon_chunks_cleaver","cleaver","none", 40, "MEAT", List.of("minecraft:salmon"),    "gh_farming_overhaul:salmon_chunks",  3);

        // ── Salads ────────────────────────────────────────────────────────────
        shapeless("garden_salad", "knife","bowl", 80, "SALAD",
                List.of("gh_farming_overhaul:shredded_lettuce",
                        "gh_farming_overhaul:sliced_cucumber",
                        "gh_farming_overhaul:chopped_tomato"),
                "gh_farming_overhaul:garden_salad", 1);
        shapeless("coleslaw",     "knife","bowl", 60, "SALAD",
                List.of("gh_farming_overhaul:shredded_cabbage",
                        "gh_farming_overhaul:chopped_carrot"),
                "gh_farming_overhaul:coleslaw", 1);
        shapeless("fruit_salad",  "knife","bowl", 80, "SALAD",
                List.of("gh_farming_overhaul:apple_slices",
                        "gh_farming_overhaul:sliced_pear",
                        "gh_farming_overhaul:melon_chunks"),
                "gh_farming_overhaul:fruit_salad", 1);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        registerAll();

        List<CompletableFuture<?>> futures = new ArrayList<>();
        Path folder = output.getOutputFolder()
                .resolve("data/gh_farming_overhaul/cutting_board_recipes");

        for (CBRecipe recipe : recipes) {
            JsonObject json = new JsonObject();
            json.addProperty("shaped",         recipe.shaped());
            json.addProperty("tool_type",      recipe.toolType());
            json.addProperty("container_type", recipe.containerType());
            json.addProperty("category",       recipe.category());
            json.addProperty("chop_time_ticks",recipe.chopTimeTicks());
            json.addProperty("spoil_reduction",recipe.spoilReduction());

            JsonArray ingredients = new JsonArray();
            for (String ing : recipe.ingredients()) ingredients.add(ing);
            json.add("ingredients", ingredients);

            JsonObject result = new JsonObject();
            result.addProperty("item",  recipe.resultItem());
            result.addProperty("count", recipe.resultCount());
            json.add("result", result);

            if (recipe.secondaryItem() != null) {
                JsonObject secondary = new JsonObject();
                secondary.addProperty("item",  recipe.secondaryItem());
                secondary.addProperty("count", 1);
                json.add("secondary_result", secondary);
                json.addProperty("secondary_min", recipe.secondaryMin());
                json.addProperty("secondary_max", recipe.secondaryMax());
            }

            Path path = folder.resolve(recipe.name() + ".json");
            futures.add(DataProvider.saveStable(cache, GSON.toJsonTree(json), path));
        }

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() { return "GH Cutting Board Recipes"; }
}
