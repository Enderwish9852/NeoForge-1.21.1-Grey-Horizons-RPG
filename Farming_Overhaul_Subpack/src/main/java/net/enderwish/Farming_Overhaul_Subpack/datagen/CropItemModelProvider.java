package net.enderwish.Farming_Overhaul_Subpack.datagen;

import net.enderwish.Farming_Overhaul_Subpack.FarmingOverhaulSubpack;
import net.enderwish.Farming_Overhaul_Subpack.item.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class CropItemModelProvider extends ItemModelProvider {

    public CropItemModelProvider(PackOutput output,
                                 ExistingFileHelper existingFileHelper) {
        super(output, FarmingOverhaulSubpack.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        // Crop items — use generated/handheld parent with item texture
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
        //cropItem("banana");
        //cropItem("fig");
        //cropItem("guava");
        //cropItem("kiwi");
        //cropItem("mango");
        //cropItem("orange");
        //cropItem("papaya");
        //cropItem("peach");
        //cropItem("pear");
        //cropItem("persimmon");
        //cropItem("plum");
        //cropItem("pomegranate");
        //cropItem("rotten_scrap");

        // Seed items
        //seedItem("aubergine_seeds");
        //seedItem("bell_pepper_seeds");
        //seedItem("broccoli_seeds");
        //seedItem("cabbage_seeds");
        //seedItem("corn_seeds");
        //seedItem("courgette_seeds");
        //seedItem("cucumber_seeds");
        //seedItem("lettuce_seeds");
        //seedItem("tomato_seeds");
        //seedItem("fig_seeds");
        //seedItem("guava_seeds");
        //seedItem("kiwi_seeds");
        //seedItem("mango_seeds");
        //seedItem("orange_seeds");
        //seedItem("papaya_seeds");
        //seedItem("peach_seeds");
        //seedItem("pear_seeds");
        //seedItem("persimmon_seeds");
        //seedItem("plum_seeds");
        //seedItem("pomegranate_seeds");

        // Clay pot block items
        withExistingParent("wet_clay_pot",
                ResourceLocation.fromNamespaceAndPath(
                        FarmingOverhaulSubpack.MODID, "block/wet_clay_pot"));
        withExistingParent("clay_pot",
                ResourceLocation.fromNamespaceAndPath(
                        FarmingOverhaulSubpack.MODID, "block/clay_pot"));
    }

    private void cropItem(String name) {
        withExistingParent(name,
                ResourceLocation.withDefaultNamespace("item/generated"))
                .texture("layer0",
                        ResourceLocation.fromNamespaceAndPath(
                                FarmingOverhaulSubpack.MODID,
                                "item/crop/" + name));
    }

    private void seedItem(String name) {
        withExistingParent(name,
                ResourceLocation.withDefaultNamespace("item/generated"))
                .texture("layer0",
                        ResourceLocation.fromNamespaceAndPath(
                                FarmingOverhaulSubpack.MODID,
                                "item/seed/" + name));
    }
}
