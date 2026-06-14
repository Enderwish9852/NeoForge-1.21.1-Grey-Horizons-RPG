package net.enderwish.Farming_Overhaul_Subpack.datagen;

import net.enderwish.Farming_Overhaul_Subpack.FarmingOverhaulSubpack;
import net.enderwish.Farming_Overhaul_Subpack.block.ModBlocks;
import net.enderwish.Farming_Overhaul_Subpack.block.crop.GHCropBlock;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;

public class CropBlockStateProvider extends BlockStateProvider {

    public CropBlockStateProvider(PackOutput output,
                                  ExistingFileHelper existingFileHelper) {
        super(output, FarmingOverhaulSubpack.MODID, existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        registerCrop(ModBlocks.AUBERGINE_CROP,    "aubergine");
        registerCrop(ModBlocks.BELL_PEPPER_CROP,  "bell_pepper");
        registerCrop(ModBlocks.BROCCOLI_CROP,     "broccoli");
        registerCrop(ModBlocks.CABBAGE_CROP,      "cabbage");
        registerCrop(ModBlocks.CORN_CROP,         "corn");
        registerCrop(ModBlocks.COURGETTE_CROP,    "courgette");
        registerCrop(ModBlocks.CUCUMBER_CROP,     "cucumber");
        registerCrop(ModBlocks.GARLIC_CROP,       "garlic");
        registerCrop(ModBlocks.LETTUCE_CROP,      "lettuce");
        registerCrop(ModBlocks.ONION_CROP,        "onion");
        registerCrop(ModBlocks.SWEET_POTATO_CROP, "sweet_potato");
        registerCrop(ModBlocks.TOMATO_CROP,       "tomato");
        //registerCrop(ModBlocks.BANANA_CROP,       "banana");
        //registerCrop(ModBlocks.FIG_CROP,          "fig");
        //registerCrop(ModBlocks.GUAVA_CROP,        "guava");
        //registerCrop(ModBlocks.KIWI_CROP,         "kiwi");
        //registerCrop(ModBlocks.MANGO_CROP,        "mango");
        //registerCrop(ModBlocks.ORANGE_CROP,       "orange");
        //registerCrop(ModBlocks.PAPAYA_CROP,       "papaya");
        //registerCrop(ModBlocks.PEACH_CROP,        "peach");
        //registerCrop(ModBlocks.PEAR_CROP,         "pear");
        //registerCrop(ModBlocks.PERSIMMON_CROP,    "persimmon");
        //registerCrop(ModBlocks.PLUM_CROP,         "plum");
        //registerCrop(ModBlocks.POMEGRANATE_CROP,  "pomegranate");
    }

    private void registerCrop(DeferredBlock<Block> block, String cropName) {
        getVariantBuilder(block.get()).forAllStates(state -> {
            int age = state.getValue(GHCropBlock.AGE);
            ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(
                    FarmingOverhaulSubpack.MODID,
                    "block/crop/" + cropName + "_stage" + age);
            return ConfiguredModel.builder()
                    .modelFile(models().withExistingParent(
                                    "block/crop/" + cropName + "_stage" + age,
                                    ResourceLocation.withDefaultNamespace("block/crop"))
                            .texture("crop", texture)
                            .renderType("cutout"))
                    .build();
        });
    }
}
