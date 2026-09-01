package net.enderwish.Farming_Overhaul_Subpack.datagen;

import net.enderwish.Farming_Overhaul_Subpack.FarmingOverhaulSubpack;
import net.enderwish.Farming_Overhaul_Subpack.block.ModBlocks;
import net.enderwish.Farming_Overhaul_Subpack.item.ModItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class FarmingLanguageProvider extends LanguageProvider {

    public FarmingLanguageProvider(PackOutput output) {
        super(output, FarmingOverhaulSubpack.MODID, "en_us");
    }

    @Override
    protected void addTranslations() {

        // ── Misc ──────────────────────────────────────────────────────────────
        add(ModItems.ROTTEN_SCRAP.get(),         "Rotten Scrap");

        // ── Equipment / Tools ─────────────────────────────────────────────────
        add(ModItems.WET_CLAY_POT.get(),         "Wet Clay Pot");
        add(ModItems.CLAY_POT.get(),             "Clay Pot");
        add(ModItems.CUTTING_BOARD.get(),        "Cutting Board");
        add(ModItems.KNIFE.get(),                "Knife");
        add(ModItems.CLEAVER.get(),              "Cleaver");
        add(ModItems.BUNDLE.get(),               "Bundle");
        add(ModItems.WOODEN_BUCKET.get(),        "Wooden Bucket");
        add(ModItems.WOODEN_BUCKET_OF_FERTILIZER.get(), "Wooden Bucket of Fertilizer");
        add(ModItems.BUCKET_OF_FERTILIZER.get(), "Bucket of Fertilizer");

        // ── Vegetable Crops ───────────────────────────────────────────────────
        add(ModItems.AUBERGINE.get(),            "Aubergine");
        add(ModItems.BELL_PEPPER.get(),          "Bell Pepper");
        add(ModItems.BROCCOLI.get(),             "Broccoli");
        add(ModItems.CABBAGE.get(),              "Cabbage");
        add(ModItems.CORN.get(),                 "Corn");
        add(ModItems.COURGETTE.get(),            "Courgette");
        add(ModItems.CUCUMBER.get(),             "Cucumber");
        add(ModItems.GARLIC.get(),               "Garlic");
        add(ModItems.LETTUCE.get(),              "Lettuce");
        add(ModItems.ONION.get(),                "Onion");
        add(ModItems.SWEET_POTATO.get(),         "Sweet Potato");
        add(ModItems.TOMATO.get(),               "Tomato");

        // ── Fruit Crops ───────────────────────────────────────────────────────
        add(ModItems.BANANA.get(),               "Banana");
        add(ModItems.FIG.get(),                  "Fig");
        add(ModItems.GUAVA.get(),                "Guava");
        add(ModItems.KIWI.get(),                 "Kiwi");
        add(ModItems.MANGO.get(),                "Mango");
        add(ModItems.ORANGE.get(),               "Orange");
        add(ModItems.PAPAYA.get(),               "Papaya");
        add(ModItems.PEACH.get(),                "Peach");
        add(ModItems.PEAR.get(),                 "Pear");
        add(ModItems.PERSIMMON.get(),            "Persimmon");
        add(ModItems.PLUM.get(),                 "Plum");
        add(ModItems.POMEGRANATE.get(),          "Pomegranate");

        // ── Vegetable Seeds ───────────────────────────────────────────────────
        add(ModItems.AUBERGINE_SEEDS.get(),      "Aubergine Seeds");
        add(ModItems.BELL_PEPPER_SEEDS.get(),    "Bell Pepper Seeds");
        add(ModItems.BROCCOLI_SEEDS.get(),       "Broccoli Seeds");
        add(ModItems.CABBAGE_SEEDS.get(),        "Cabbage Seeds");
        add(ModItems.CORN_SEEDS.get(),           "Corn Seeds");
        add(ModItems.COURGETTE_SEEDS.get(),      "Courgette Seeds");
        add(ModItems.CUCUMBER_SEEDS.get(),       "Cucumber Seeds");
        add(ModItems.LETTUCE_SEEDS.get(),        "Lettuce Seeds");
        add(ModItems.TOMATO_SEEDS.get(),         "Tomato Seeds");

        // ── Fruit Seeds ───────────────────────────────────────────────────────
        add(ModItems.FIG_SEEDS.get(),            "Fig Seeds");
        add(ModItems.GUAVA_SEEDS.get(),          "Guava Seeds");
        add(ModItems.KIWI_SEEDS.get(),           "Kiwi Seeds");
        add(ModItems.MANGO_SEEDS.get(),          "Mango Seeds");
        add(ModItems.ORANGE_SEEDS.get(),         "Orange Seeds");
        add(ModItems.PAPAYA_SEEDS.get(),         "Papaya Seeds");
        add(ModItems.PEACH_SEEDS.get(),          "Peach Seeds");
        add(ModItems.PEAR_SEEDS.get(),           "Pear Seeds");
        add(ModItems.PERSIMMON_SEEDS.get(),      "Persimmon Seeds");
        add(ModItems.PLUM_SEEDS.get(),           "Plum Seeds");
        add(ModItems.POMEGRANATE_SEEDS.get(),    "Pomegranate Seeds");

        // ── Chopped Vegetables ────────────────────────────────────────────────
        add(ModItems.CHOPPED_TOMATO.get(),       "Chopped Tomato");
        add(ModItems.CHOPPED_BELL_PEPPER.get(),  "Chopped Bell Pepper");
        add(ModItems.CHOPPED_BROCCOLI.get(),     "Chopped Broccoli");
        add(ModItems.SHREDDED_CABBAGE.get(),     "Shredded Cabbage");
        add(ModItems.CABBAGE_LEAF.get(),         "Cabbage Leaf");
        add(ModItems.CORN_KERNELS.get(),         "Corn Kernels");
        add(ModItems.SLICED_COURGETTE.get(),     "Sliced Courgette");
        add(ModItems.SLICED_CUCUMBER.get(),      "Sliced Cucumber");
        add(ModItems.MINCED_GARLIC.get(),        "Minced Garlic");
        add(ModItems.SHREDDED_LETTUCE.get(),     "Shredded Lettuce");
        add(ModItems.DICED_ONION.get(),          "Diced Onion");
        add(ModItems.ONION_RINGS.get(),          "Onion Rings");
        add(ModItems.CUBED_SWEET_POTATO.get(),   "Cubed Sweet Potato");
        add(ModItems.TOMATO_PASTE.get(),         "Tomato Paste");

        // ── Peeled Vegetables ─────────────────────────────────────────────────
        add(ModItems.PEELED_POTATO.get(),        "Peeled Potato");
        add(ModItems.PEELED_CUCUMBER.get(),      "Peeled Cucumber");
        add(ModItems.PEELED_COURGETTE.get(),     "Peeled Courgette");
        add(ModItems.PEELED_SWEET_POTATO.get(),  "Peeled Sweet Potato");

        // ── Chopped Vanilla Vegetables ────────────────────────────────────────
        add(ModItems.DICED_POTATO.get(),         "Diced Potato");
        add(ModItems.CHOPPED_CARROT.get(),       "Chopped Carrot");
        add(ModItems.PEELED_CARROT.get(),        "Peeled Carrot");
        add(ModItems.CHOPPED_BEETROOT.get(),     "Chopped Beetroot");
        add(ModItems.GRATED_CARROT.get(),        "Grated Carrot");
        add(ModItems.GRATED_COURGETTE.get(),     "Grated Courgette");

        // ── Vegetable Peels ───────────────────────────────────────────────────
        add(ModItems.POTATO_PEEL.get(),          "Potato Peel");
        add(ModItems.CARROT_PEEL.get(),          "Carrot Peel");
        add(ModItems.CUCUMBER_PEEL.get(),        "Cucumber Peel");
        add(ModItems.COURGETTE_PEEL.get(),       "Courgette Peel");
        add(ModItems.SWEET_POTATO_PEEL.get(),    "Sweet Potato Peel");

        // ── Sliced / Prepped Fruits ───────────────────────────────────────────
        add(ModItems.SLICED_BANANA.get(),        "Sliced Banana");
        add(ModItems.MASHED_BANANA.get(),        "Mashed Banana");
        add(ModItems.HALVED_FIG.get(),           "Halved Fig");
        add(ModItems.CUBED_GUAVA.get(),          "Cubed Guava");
        add(ModItems.SLICED_KIWI.get(),          "Sliced Kiwi");
        add(ModItems.PEELED_KIWI.get(),          "Peeled Kiwi");
        add(ModItems.CUBED_MANGO.get(),          "Cubed Mango");
        add(ModItems.PEELED_MANGO.get(),         "Peeled Mango");
        add(ModItems.ORANGE_SLICES.get(),        "Orange Slices");
        add(ModItems.ORANGE_ZEST.get(),          "Orange Zest");
        add(ModItems.CUBED_PAPAYA.get(),         "Cubed Papaya");
        add(ModItems.SLICED_PEACH.get(),         "Sliced Peach");
        add(ModItems.PEELED_PEACH.get(),         "Peeled Peach");
        add(ModItems.SLICED_PEAR.get(),          "Sliced Pear");
        add(ModItems.PEELED_PEAR.get(),          "Peeled Pear");
        add(ModItems.SLICED_PERSIMMON.get(),     "Sliced Persimmon");
        add(ModItems.HALVED_PLUM.get(),          "Halved Plum");

        // ── Peeled Vanilla Fruits ─────────────────────────────────────────────
        add(ModItems.APPLE_SLICES.get(),         "Apple Slices");
        add(ModItems.PEELED_APPLE.get(),         "Peeled Apple");
        add(ModItems.GRATED_APPLE.get(),         "Grated Apple");
        add(ModItems.MELON_CHUNKS.get(),         "Melon Chunks");

        // ── Fruit Peels ───────────────────────────────────────────────────────
        add(ModItems.BANANA_PEEL.get(),          "Banana Peel");
        add(ModItems.KIWI_PEEL.get(),            "Kiwi Peel");
        add(ModItems.MANGO_PEEL.get(),           "Mango Peel");
        add(ModItems.ORANGE_PEEL.get(),          "Orange Peel");
        add(ModItems.PEACH_PEEL.get(),           "Peach Peel");
        add(ModItems.PEAR_PEEL.get(),            "Pear Peel");
        add(ModItems.APPLE_PEEL.get(),           "Apple Peel");

        // ── Meat Prep ─────────────────────────────────────────────────────────
        add(ModItems.CUBED_BEEF.get(),           "Cubed Beef");
        add(ModItems.MINCED_BEEF.get(),          "Minced Beef");
        add(ModItems.BEEF_STRIPS.get(),          "Beef Strips");
        add(ModItems.CUBED_PORK.get(),           "Cubed Pork");
        add(ModItems.MINCED_PORK.get(),          "Minced Pork");
        add(ModItems.PORK_STRIPS.get(),          "Pork Strips");
        add(ModItems.DICED_CHICKEN.get(),        "Diced Chicken");
        add(ModItems.CHICKEN_STRIPS.get(),       "Chicken Strips");
        add(ModItems.CUBED_MUTTON.get(),         "Cubed Mutton");
        add(ModItems.MINCED_MUTTON.get(),        "Minced Mutton");
        add(ModItems.DICED_RABBIT.get(),         "Diced Rabbit");
        add(ModItems.FISH_FILLET.get(),          "Fish Fillet");
        add(ModItems.FISH_CHUNKS.get(),          "Fish Chunks");
        add(ModItems.SALMON_FILLET.get(),        "Salmon Fillet");
        add(ModItems.SALMON_CHUNKS.get(),        "Salmon Chunks");

        // ── Misc Prep ─────────────────────────────────────────────────────────
        add(ModItems.BREAD_CRUMBS.get(),         "Bread Crumbs");
        add(ModItems.EGG_WASH.get(),             "Egg Wash");

        // ── Salads ────────────────────────────────────────────────────────────
        add(ModItems.GARDEN_SALAD.get(),         "Garden Salad");
        add(ModItems.COLESLAW.get(),             "Coleslaw");

        // ── Fertilized Farmland ───────────────────────────────────────────────
        add(ModItems.FERTILIZED_FARMLAND.get(),  "Fertilized Farmland");

        // ── Creative tabs ─────────────────────────────────────────────────────────
        add("creativetab.gh_farming_overhaul.crops",     "GH — Crops & Seeds");
        add("creativetab.gh_farming_overhaul.prep",      "GH — Prep Ingredients");
        add("creativetab.gh_farming_overhaul.meals",     "GH — Meals");
        add("creativetab.gh_farming_overhaul.equipment", "GH — Equipment");
        add("creativetab.gh_farming_overhaul.misc",      "GH — Misc");
        add("creativetab.gh_farming_overhaul.nature", "GH — Nature");

        // ── Container titles ──────────────────────────────────────────────────────
        add("container.gh_farming_overhaul.clay_pot",      "Clay Pot");
        add("container.gh_farming_overhaul.cutting_board", "Cutting Board");

        // ── Trees ─────────────────────────────────────────────────────────────────
        add(ModItems.OAK_LOG.get(),    "Oak Log");
        add(ModItems.OAK_LEAVES.get(), "Oak Leaves");

    }
}
