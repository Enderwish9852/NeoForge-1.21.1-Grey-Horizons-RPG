package net.enderwish.Farming_Overhaul_Subpack.item;

import net.enderwish.Farming_Overhaul_Subpack.FarmingOverhaulSubpack;
import net.enderwish.Farming_Overhaul_Subpack.block.ModBlocks;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(FarmingOverhaulSubpack.MODID);

    // ── Misc ──────────────────────────────────────────────────────────────────
    public static final DeferredItem<Item> ROTTEN_SCRAP = ITEMS.register("rotten_scrap",
            () -> new Item(new Item.Properties().stacksTo(64)));

    // ── Equipment ─────────────────────────────────────────────────────────────
    public static final DeferredItem<Item> WET_CLAY_POT = ITEMS.register("wet_clay_pot",
            () -> new BlockItem(ModBlocks.WET_CLAY_POT.get(),
                    new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> CLAY_POT = ITEMS.register("clay_pot",
            () -> new BlockItem(ModBlocks.CLAY_POT.get(),
                    new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> CUTTING_BOARD = ITEMS.register("cutting_board",
            () -> new BlockItem(ModBlocks.CUTTING_BOARD.get(),
                    new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> KNIFE = ITEMS.register("knife",
            () -> new net.minecraft.world.item.SwordItem(
                    net.minecraft.world.item.Tiers.STONE,
                    new Item.Properties().durability(64)));

    public static final DeferredItem<Item> CLEAVER = ITEMS.register("cleaver",
            () -> new net.minecraft.world.item.SwordItem(
                    net.minecraft.world.item.Tiers.IRON,
                    new Item.Properties().durability(128)));

    public static final DeferredItem<Item> BUNDLE = ITEMS.register("bundle",
            () -> new net.minecraft.world.item.BundleItem(
                    new Item.Properties().stacksTo(1)));

    // ── Vegetable Crops ───────────────────────────────────────────────────────
    public static final DeferredItem<Item> AUBERGINE    = ITEMS.register("aubergine",    () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> BELL_PEPPER  = ITEMS.register("bell_pepper",  () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> BROCCOLI     = ITEMS.register("broccoli",     () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> CABBAGE      = ITEMS.register("cabbage",      () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> CORN         = ITEMS.register("corn",         () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> COURGETTE    = ITEMS.register("courgette",    () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> CUCUMBER     = ITEMS.register("cucumber",     () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> GARLIC       = ITEMS.register("garlic",
            () -> new SelfPlantingCropItem(
                    ModBlocks.GARLIC_CROP::get,
                    new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> LETTUCE      = ITEMS.register("lettuce",      () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> ONION        = ITEMS.register("onion",
            () -> new SelfPlantingCropItem(
                    ModBlocks.ONION_CROP::get,
                    new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> SWEET_POTATO = ITEMS.register("sweet_potato",
            () -> new SelfPlantingCropItem(
                    ModBlocks.SWEET_POTATO_CROP::get,
                    new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> TOMATO       = ITEMS.register("tomato",       () -> new Item(new Item.Properties().stacksTo(16)));

    // ── Fruit Crops ───────────────────────────────────────────────────────────
    public static final DeferredItem<Item> BANANA      = ITEMS.register("banana",      () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> FIG         = ITEMS.register("fig",         () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> GUAVA       = ITEMS.register("guava",       () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> KIWI        = ITEMS.register("kiwi",        () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> MANGO       = ITEMS.register("mango",       () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> ORANGE      = ITEMS.register("orange",      () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> PAPAYA      = ITEMS.register("papaya",      () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> PEACH       = ITEMS.register("peach",       () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> PEAR        = ITEMS.register("pear",        () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> PERSIMMON   = ITEMS.register("persimmon",   () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> PLUM        = ITEMS.register("plum",        () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> POMEGRANATE = ITEMS.register("pomegranate", () -> new Item(new Item.Properties().stacksTo(16)));

    // ── Vegetable Seeds ───────────────────────────────────────────────────────
    public static final DeferredItem<Item> AUBERGINE_SEEDS   = ITEMS.register("aubergine_seeds",
            () -> new net.minecraft.world.item.ItemNameBlockItem(
                    ModBlocks.AUBERGINE_CROP.get(),
                    new Item.Properties().stacksTo(64)));

    public static final DeferredItem<Item> BELL_PEPPER_SEEDS = ITEMS.register("bell_pepper_seeds",
            () -> new net.minecraft.world.item.ItemNameBlockItem(
                    ModBlocks.BELL_PEPPER_CROP.get(),
                    new Item.Properties().stacksTo(64)));

    public static final DeferredItem<Item> BROCCOLI_SEEDS    = ITEMS.register("broccoli_seeds",
            () -> new net.minecraft.world.item.ItemNameBlockItem(
                    ModBlocks.BROCCOLI_CROP.get(),
                    new Item.Properties().stacksTo(64)));

    public static final DeferredItem<Item> CABBAGE_SEEDS     = ITEMS.register("cabbage_seeds",
            () -> new net.minecraft.world.item.ItemNameBlockItem(
                    ModBlocks.CABBAGE_CROP.get(),
                    new Item.Properties().stacksTo(64)));

    public static final DeferredItem<Item> CORN_SEEDS        = ITEMS.register("corn_seeds",
            () -> new net.minecraft.world.item.ItemNameBlockItem(
                    ModBlocks.CORN_CROP.get(),
                    new Item.Properties().stacksTo(64)));

    public static final DeferredItem<Item> COURGETTE_SEEDS   = ITEMS.register("courgette_seeds",
            () -> new net.minecraft.world.item.ItemNameBlockItem(
                    ModBlocks.COURGETTE_CROP.get(),
                    new Item.Properties().stacksTo(64)));

    public static final DeferredItem<Item> CUCUMBER_SEEDS    = ITEMS.register("cucumber_seeds",
            () -> new net.minecraft.world.item.ItemNameBlockItem(
                    ModBlocks.CUCUMBER_CROP.get(),
                    new Item.Properties().stacksTo(64)));

    public static final DeferredItem<Item> LETTUCE_SEEDS     = ITEMS.register("lettuce_seeds",
            () -> new net.minecraft.world.item.ItemNameBlockItem(
                    ModBlocks.LETTUCE_CROP.get(),
                    new Item.Properties().stacksTo(64)));

    public static final DeferredItem<Item> TOMATO_SEEDS      = ITEMS.register("tomato_seeds",
            () -> new net.minecraft.world.item.ItemNameBlockItem(
                    ModBlocks.TOMATO_CROP.get(),
                    new Item.Properties().stacksTo(64)));



    // ── Fruit Seeds ───────────────────────────────────────────────────────────
    public static final DeferredItem<Item> FIG_SEEDS         = ITEMS.register("fig_seeds",         () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> GUAVA_SEEDS       = ITEMS.register("guava_seeds",       () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> KIWI_SEEDS        = ITEMS.register("kiwi_seeds",        () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> MANGO_SEEDS       = ITEMS.register("mango_seeds",       () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> ORANGE_SEEDS      = ITEMS.register("orange_seeds",      () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> PAPAYA_SEEDS      = ITEMS.register("papaya_seeds",      () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> PEACH_SEEDS       = ITEMS.register("peach_seeds",       () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> PEAR_SEEDS        = ITEMS.register("pear_seeds",        () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> PERSIMMON_SEEDS   = ITEMS.register("persimmon_seeds",   () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> PLUM_SEEDS        = ITEMS.register("plum_seeds",        () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> POMEGRANATE_SEEDS = ITEMS.register("pomegranate_seeds", () -> new Item(new Item.Properties().stacksTo(64)));

    // ── Chopped Vegetables ────────────────────────────────────────────────────
    public static final DeferredItem<Item> CHOPPED_TOMATO       = ITEMS.register("chopped_tomato",       () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> CHOPPED_BELL_PEPPER  = ITEMS.register("chopped_bell_pepper",  () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> CHOPPED_BROCCOLI     = ITEMS.register("chopped_broccoli",     () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> SHREDDED_CABBAGE     = ITEMS.register("shredded_cabbage",     () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> CABBAGE_LEAF         = ITEMS.register("cabbage_leaf",         () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> CORN_KERNELS         = ITEMS.register("corn_kernels",         () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> SLICED_COURGETTE     = ITEMS.register("sliced_courgette",     () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> SLICED_CUCUMBER      = ITEMS.register("sliced_cucumber",      () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> MINCED_GARLIC        = ITEMS.register("minced_garlic",        () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> SHREDDED_LETTUCE     = ITEMS.register("shredded_lettuce",     () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> DICED_ONION          = ITEMS.register("diced_onion",          () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> ONION_RINGS          = ITEMS.register("onion_rings",          () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> CUBED_SWEET_POTATO   = ITEMS.register("cubed_sweet_potato",   () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> TOMATO_PASTE         = ITEMS.register("tomato_paste",         () -> new Item(new Item.Properties().stacksTo(16)));

    // ── Peeled Vegetables ─────────────────────────────────────────────────────
    public static final DeferredItem<Item> PEELED_POTATO        = ITEMS.register("peeled_potato",        () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> PEELED_CUCUMBER      = ITEMS.register("peeled_cucumber",      () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> PEELED_COURGETTE     = ITEMS.register("peeled_courgette",     () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> PEELED_SWEET_POTATO  = ITEMS.register("peeled_sweet_potato",  () -> new Item(new Item.Properties().stacksTo(16)));

    // ── Chopped Vanilla Vegetables ────────────────────────────────────────────
    public static final DeferredItem<Item> DICED_POTATO         = ITEMS.register("diced_potato",         () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> CHOPPED_CARROT       = ITEMS.register("chopped_carrot",       () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> PEELED_CARROT        = ITEMS.register("peeled_carrot",        () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> CHOPPED_BEETROOT     = ITEMS.register("chopped_beetroot",     () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> GRATED_CARROT        = ITEMS.register("grated_carrot",        () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> GRATED_COURGETTE     = ITEMS.register("grated_courgette",     () -> new Item(new Item.Properties().stacksTo(16)));

    // ── Peels (Vegetables) ────────────────────────────────────────────────────
    public static final DeferredItem<Item> POTATO_PEEL          = ITEMS.register("potato_peel",          () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> CARROT_PEEL          = ITEMS.register("carrot_peel",          () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> CUCUMBER_PEEL        = ITEMS.register("cucumber_peel",        () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> COURGETTE_PEEL       = ITEMS.register("courgette_peel",       () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> SWEET_POTATO_PEEL    = ITEMS.register("sweet_potato_peel",    () -> new Item(new Item.Properties().stacksTo(64)));

    // ── Sliced / Prepped Fruits ───────────────────────────────────────────────
    public static final DeferredItem<Item> SLICED_BANANA        = ITEMS.register("sliced_banana",        () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> MASHED_BANANA        = ITEMS.register("mashed_banana",        () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> HALVED_FIG           = ITEMS.register("halved_fig",           () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> CUBED_GUAVA          = ITEMS.register("cubed_guava",          () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> SLICED_KIWI          = ITEMS.register("sliced_kiwi",          () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> PEELED_KIWI          = ITEMS.register("peeled_kiwi",          () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> CUBED_MANGO          = ITEMS.register("cubed_mango",          () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> PEELED_MANGO         = ITEMS.register("peeled_mango",         () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> ORANGE_SLICES        = ITEMS.register("orange_slices",        () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> ORANGE_ZEST          = ITEMS.register("orange_zest",          () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> CUBED_PAPAYA         = ITEMS.register("cubed_papaya",         () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> SLICED_PEACH         = ITEMS.register("sliced_peach",         () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> PEELED_PEACH         = ITEMS.register("peeled_peach",         () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> SLICED_PEAR          = ITEMS.register("sliced_pear",          () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> PEELED_PEAR          = ITEMS.register("peeled_pear",          () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> SLICED_PERSIMMON     = ITEMS.register("sliced_persimmon",     () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> HALVED_PLUM          = ITEMS.register("halved_plum",          () -> new Item(new Item.Properties().stacksTo(16)));

    // ── Peeled Vanilla Fruits ─────────────────────────────────────────────────
    public static final DeferredItem<Item> APPLE_SLICES         = ITEMS.register("apple_slices",         () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> PEELED_APPLE         = ITEMS.register("peeled_apple",         () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> GRATED_APPLE         = ITEMS.register("grated_apple",         () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> MELON_CHUNKS         = ITEMS.register("melon_chunks",         () -> new Item(new Item.Properties().stacksTo(16)));

    // ── Peels (Fruits) ────────────────────────────────────────────────────────
    public static final DeferredItem<Item> BANANA_PEEL          = ITEMS.register("banana_peel",          () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> KIWI_PEEL            = ITEMS.register("kiwi_peel",            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> MANGO_PEEL           = ITEMS.register("mango_peel",           () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> ORANGE_PEEL          = ITEMS.register("orange_peel",          () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> PEACH_PEEL           = ITEMS.register("peach_peel",           () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> PEAR_PEEL            = ITEMS.register("pear_peel",            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> APPLE_PEEL           = ITEMS.register("apple_peel",           () -> new Item(new Item.Properties().stacksTo(64)));

    // ── Meat Prep ─────────────────────────────────────────────────────────────
    public static final DeferredItem<Item> CUBED_BEEF           = ITEMS.register("cubed_beef",           () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> MINCED_BEEF          = ITEMS.register("minced_beef",          () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> BEEF_STRIPS          = ITEMS.register("beef_strips",          () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> CUBED_PORK           = ITEMS.register("cubed_pork",           () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> MINCED_PORK          = ITEMS.register("minced_pork",          () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> PORK_STRIPS          = ITEMS.register("pork_strips",          () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> DICED_CHICKEN        = ITEMS.register("diced_chicken",        () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> CHICKEN_STRIPS       = ITEMS.register("chicken_strips",       () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> CUBED_MUTTON         = ITEMS.register("cubed_mutton",         () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> MINCED_MUTTON        = ITEMS.register("minced_mutton",        () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> DICED_RABBIT         = ITEMS.register("diced_rabbit",         () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> FISH_FILLET          = ITEMS.register("fish_fillet",          () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> FISH_CHUNKS          = ITEMS.register("fish_chunks",          () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> SALMON_FILLET        = ITEMS.register("salmon_fillet",        () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> SALMON_CHUNKS        = ITEMS.register("salmon_chunks",        () -> new Item(new Item.Properties().stacksTo(16)));

    // ── Misc Prep ─────────────────────────────────────────────────────────────
    public static final DeferredItem<Item> BREAD_CRUMBS         = ITEMS.register("bread_crumbs",         () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> EGG_WASH             = ITEMS.register("egg_wash",             () -> new Item(new Item.Properties().stacksTo(16)));

    // ── Salad Meals (need bowl) ───────────────────────────────────────────────
    public static final DeferredItem<Item> GARDEN_SALAD = ITEMS.register("garden_salad",
            () -> new Item(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> COLESLAW = ITEMS.register("coleslaw",
            () -> new Item(new Item.Properties().stacksTo(1)));

    // ── Farming Tools ─────────────────────────────────────────────────────────

    public static final DeferredItem<Item> WOODEN_BUCKET = ITEMS.register("wooden_bucket",
            () -> new net.minecraft.world.item.BucketItem(
                    net.minecraft.world.level.material.Fluids.EMPTY,
                    new Item.Properties().stacksTo(16)));

    // ── Fertilizer Buckets ──────────────────────────────────────────────────

    public static final DeferredItem<Item> WOODEN_BUCKET_OF_FERTILIZER = ITEMS.register(
            "wooden_bucket_of_fertilizer",
            () -> new net.enderwish.Farming_Overhaul_Subpack.item.FertilizerBucketItem(
                    new Item.Properties().stacksTo(1),
                    net.enderwish.Farming_Overhaul_Subpack.item.ModItems.WOODEN_BUCKET));

    public static final DeferredItem<Item> BUCKET_OF_FERTILIZER = ITEMS.register(
            "bucket_of_fertilizer",
            () -> new net.enderwish.Farming_Overhaul_Subpack.item.FertilizerBucketItem(
                    new Item.Properties().stacksTo(1),
                    () -> net.minecraft.world.item.Items.BUCKET));

    public static final DeferredItem<Item> FERTILIZED_FARMLAND = ITEMS.register(
            "fertilized_farmland",
            () -> new BlockItem(ModBlocks.FERTILIZED_FARMLAND.get(),
                    new Item.Properties()));

    public static final DeferredItem<Item> GH_COMPOSTER = ITEMS.register("gh_composter",
            () -> new BlockItem(ModBlocks.GH_COMPOSTER.get(),
                    new Item.Properties()));

    // ── Tree Blocks ───────────────────────────────────────────────────────────

    public static final DeferredItem<Item> OAK_LOG = ITEMS.register(
            "oak_log",
            () -> new BlockItem(ModBlocks.OAK_LOG.get(),
                    new Item.Properties()));

    public static final DeferredItem<Item> OAK_LEAVES = ITEMS.register(
            "oak_leaves",
            () -> new BlockItem(ModBlocks.OAK_LEAVES.get(),
                    new Item.Properties()));
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}