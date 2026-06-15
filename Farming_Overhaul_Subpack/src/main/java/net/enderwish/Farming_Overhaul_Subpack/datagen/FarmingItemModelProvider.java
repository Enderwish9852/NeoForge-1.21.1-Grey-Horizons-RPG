package net.enderwish.Farming_Overhaul_Subpack.datagen;

import net.enderwish.Farming_Overhaul_Subpack.FarmingOverhaulSubpack;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class FarmingItemModelProvider extends ItemModelProvider {

    public FarmingItemModelProvider(PackOutput output,
                                    ExistingFileHelper existingFileHelper) {
        super(output, FarmingOverhaulSubpack.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {

        // ── Veg crop items (have textures) ────────────────────────────────────
        cropItem("aubergine");
        cropItem("bell_pepper");
        cropItem("broccoli");
        cropItem("cabbage");
        cropItem("corn");
        cropItem("courgette");
        cropItem("cucumber");
        cropItem("garlic");
        cropItem("lettuce");
        cropItem("onion");
        cropItem("sweet_potato");
        cropItem("tomato");

        // ── Fruit crop items (placeholder until textures added) ───────────────
        placeholder("banana");
        placeholder("fig");
        placeholder("guava");
        placeholder("kiwi");
        placeholder("mango");
        placeholder("orange");
        placeholder("papaya");
        placeholder("peach");
        placeholder("pear");
        placeholder("persimmon");
        placeholder("plum");
        placeholder("pomegranate");

        // ── Vegetable seeds ───────────────────────────────────────────────────
        placeholder("aubergine_seeds");
        placeholder("bell_pepper_seeds");
        placeholder("broccoli_seeds");
        placeholder("cabbage_seeds");
        placeholder("corn_seeds");
        placeholder("courgette_seeds");
        placeholder("cucumber_seeds");
        placeholder("lettuce_seeds");
        placeholder("tomato_seeds");

        // ── Fruit seeds ───────────────────────────────────────────────────────
        placeholder("fig_seeds");
        placeholder("guava_seeds");
        placeholder("kiwi_seeds");
        placeholder("mango_seeds");
        placeholder("orange_seeds");
        placeholder("papaya_seeds");
        placeholder("peach_seeds");
        placeholder("pear_seeds");
        placeholder("persimmon_seeds");
        placeholder("plum_seeds");
        placeholder("pomegranate_seeds");

        // ── Block items — point to block models ───────────────────────────────
        withExistingParent("wet_clay_pot",
                ResourceLocation.fromNamespaceAndPath(
                        FarmingOverhaulSubpack.MODID, "block/wet_clay_pot"));
        withExistingParent("clay_pot",
                ResourceLocation.fromNamespaceAndPath(
                        FarmingOverhaulSubpack.MODID, "block/clay_pot"));
        withExistingParent("cutting_board",
                ResourceLocation.fromNamespaceAndPath(
                        FarmingOverhaulSubpack.MODID, "block/cutting_board"));
        withExistingParent("fertilized_farmland",
                ResourceLocation.fromNamespaceAndPath(
                        FarmingOverhaulSubpack.MODID, "block/fertilized_farmland"));
        withExistingParent("gh_composter",
                ResourceLocation.withDefaultNamespace("block/composter"));

        // ── Tools ─────────────────────────────────────────────────────────────
        placeholder("knife");
        handheldItem("cleaver");
        placeholder("bundle");
        placeholder("wooden_bucket");
        placeholder("wooden_bucket_of_fertilizer");
        placeholder("bucket_of_fertilizer");

        // ── Misc ──────────────────────────────────────────────────────────────
        placeholder("rotten_scrap");

        // ── Chopped vegetables ────────────────────────────────────────────────
        placeholder("chopped_tomato");
        placeholder("chopped_bell_pepper");
        placeholder("chopped_broccoli");
        placeholder("shredded_cabbage");
        placeholder("cabbage_leaf");
        placeholder("corn_kernels");
        placeholder("sliced_courgette");
        placeholder("sliced_cucumber");
        placeholder("minced_garlic");
        placeholder("shredded_lettuce");
        placeholder("diced_onion");
        placeholder("onion_rings");
        placeholder("cubed_sweet_potato");
        placeholder("tomato_paste");

        // ── Peeled vegetables ─────────────────────────────────────────────────
        placeholder("peeled_potato");
        placeholder("peeled_cucumber");
        placeholder("peeled_courgette");
        placeholder("peeled_sweet_potato");

        // ── Chopped vanilla vegetables ────────────────────────────────────────
        placeholder("diced_potato");
        placeholder("chopped_carrot");
        placeholder("peeled_carrot");
        placeholder("chopped_beetroot");
        placeholder("grated_carrot");
        placeholder("grated_courgette");

        // ── Vegetable peels ───────────────────────────────────────────────────
        placeholder("potato_peel");
        placeholder("carrot_peel");
        placeholder("cucumber_peel");
        placeholder("courgette_peel");
        placeholder("sweet_potato_peel");

        // ── Prepped fruits ────────────────────────────────────────────────────
        placeholder("sliced_banana");
        placeholder("mashed_banana");
        placeholder("halved_fig");
        placeholder("cubed_guava");
        placeholder("sliced_kiwi");
        placeholder("peeled_kiwi");
        placeholder("cubed_mango");
        placeholder("peeled_mango");
        placeholder("orange_slices");
        placeholder("orange_zest");
        placeholder("cubed_papaya");
        placeholder("sliced_peach");
        placeholder("peeled_peach");
        placeholder("sliced_pear");
        placeholder("peeled_pear");
        placeholder("sliced_persimmon");
        placeholder("halved_plum");
        placeholder("pomegranate_arils");

        // ── Peeled vanilla fruits ─────────────────────────────────────────────
        placeholder("apple_slices");
        placeholder("peeled_apple");
        placeholder("grated_apple");
        placeholder("melon_chunks");

        // ── Fruit peels ───────────────────────────────────────────────────────
        placeholder("banana_peel");
        placeholder("kiwi_peel");
        placeholder("mango_peel");
        placeholder("orange_peel");
        placeholder("peach_peel");
        placeholder("pear_peel");
        placeholder("apple_peel");

        // ── Meat prep ─────────────────────────────────────────────────────────
        placeholder("cubed_beef");
        placeholder("minced_beef");
        placeholder("beef_strips");
        placeholder("cubed_pork");
        placeholder("minced_pork");
        placeholder("pork_strips");
        placeholder("diced_chicken");
        placeholder("chicken_strips");
        placeholder("cubed_mutton");
        placeholder("minced_mutton");
        placeholder("diced_rabbit");
        placeholder("fish_fillet");
        placeholder("fish_chunks");
        placeholder("salmon_fillet");
        placeholder("salmon_chunks");

        // ── Misc prep ─────────────────────────────────────────────────────────
        placeholder("bread_crumbs");
        placeholder("egg_wash");

        // ── Salads ────────────────────────────────────────────────────────────
        placeholder("garden_salad");
        placeholder("coleslaw");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Real item texture at item/crop/<name>.png
     * Used for veg crops that have actual textures ready.
     */
    private void cropItem(String name) {
        withExistingParent(name,
                ResourceLocation.withDefaultNamespace("item/generated"))
                .texture("layer0",
                        ResourceLocation.fromNamespaceAndPath(
                                FarmingOverhaulSubpack.MODID,
                                "item/crop/" + name));
    }

    /**
     * Handheld tool texture at item/<name>.png
     */
    private void handheldItem(String name) {
        withExistingParent(name,
                ResourceLocation.withDefaultNamespace("item/handheld"))
                .texture("layer0",
                        ResourceLocation.fromNamespaceAndPath(
                                FarmingOverhaulSubpack.MODID,
                                "item/" + name));
    }

    /**
     * Placeholder — uses suspicious_stew model.
     * No texture needed. Replace incrementally as real textures are added.
     */
    private void placeholder(String name) {
        withExistingParent(name,
                ResourceLocation.withDefaultNamespace("item/suspicious_stew"));
    }
}
