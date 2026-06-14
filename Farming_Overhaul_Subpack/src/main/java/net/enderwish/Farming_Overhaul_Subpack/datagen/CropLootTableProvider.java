package net.enderwish.Farming_Overhaul_Subpack.datagen;

import net.enderwish.Farming_Overhaul_Subpack.FarmingOverhaulSubpack;
import net.enderwish.Farming_Overhaul_Subpack.block.ModBlocks;
import net.enderwish.Farming_Overhaul_Subpack.block.crop.GHCropBlock;
import net.enderwish.Farming_Overhaul_Subpack.item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class CropLootTableProvider extends LootTableProvider {

    public CropLootTableProvider(PackOutput output,
                                 CompletableFuture<HolderLookup.Provider> provider) {
        super(output, Set.of(), List.of(
                new SubProviderEntry(
                        p -> new CropLootSubProvider(p),
                        net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.BLOCK
                )
        ), provider);
    }

    static class CropLootSubProvider extends BlockLootSubProvider {

        protected CropLootSubProvider(HolderLookup.Provider provider) {
            super(Set.of(), FeatureFlags.REGISTRY.allFlags(), provider);
        }

        @Override
        protected void generate() {
            // Veg crops — drop item + seeds when fully grown
            cropDrop(ModBlocks.AUBERGINE_CROP,    ModItems.AUBERGINE,    ModItems.AUBERGINE_SEEDS);
            cropDrop(ModBlocks.BELL_PEPPER_CROP,  ModItems.BELL_PEPPER,  ModItems.BELL_PEPPER_SEEDS);
            cropDrop(ModBlocks.BROCCOLI_CROP,     ModItems.BROCCOLI,     ModItems.BROCCOLI_SEEDS);
            cropDrop(ModBlocks.CABBAGE_CROP,      ModItems.CABBAGE,      ModItems.CABBAGE_SEEDS);
            cropDrop(ModBlocks.CORN_CROP,         ModItems.CORN,         ModItems.CORN_SEEDS);
            cropDrop(ModBlocks.COURGETTE_CROP,    ModItems.COURGETTE,    ModItems.COURGETTE_SEEDS);
            cropDrop(ModBlocks.CUCUMBER_CROP,     ModItems.CUCUMBER,     ModItems.CUCUMBER_SEEDS);
            cropDrop(ModBlocks.LETTUCE_CROP,      ModItems.LETTUCE,      ModItems.LETTUCE_SEEDS);
            cropDrop(ModBlocks.TOMATO_CROP,       ModItems.TOMATO,       ModItems.TOMATO_SEEDS);

            // Self-planting crops — drop item only (item IS the seed)
            selfCropDrop(ModBlocks.GARLIC_CROP,       ModItems.GARLIC);
            selfCropDrop(ModBlocks.ONION_CROP,        ModItems.ONION);
            selfCropDrop(ModBlocks.SWEET_POTATO_CROP, ModItems.SWEET_POTATO);

            // Fruit crops
            //cropDrop(ModBlocks.FIG_CROP,          ModItems.FIG,          ModItems.FIG_SEEDS);
            //cropDrop(ModBlocks.GUAVA_CROP,        ModItems.GUAVA,        ModItems.GUAVA_SEEDS);
            //cropDrop(ModBlocks.KIWI_CROP,         ModItems.KIWI,         ModItems.KIWI_SEEDS);
            //cropDrop(ModBlocks.MANGO_CROP,        ModItems.MANGO,        ModItems.MANGO_SEEDS);
            //cropDrop(ModBlocks.ORANGE_CROP,       ModItems.ORANGE,       ModItems.ORANGE_SEEDS);
            //cropDrop(ModBlocks.PAPAYA_CROP,       ModItems.PAPAYA,       ModItems.PAPAYA_SEEDS);
            //cropDrop(ModBlocks.PEACH_CROP,        ModItems.PEACH,        ModItems.PEACH_SEEDS);
            //cropDrop(ModBlocks.PEAR_CROP,         ModItems.PEAR,         ModItems.PEAR_SEEDS);
            //cropDrop(ModBlocks.PERSIMMON_CROP,    ModItems.PERSIMMON,    ModItems.PERSIMMON_SEEDS);
            //cropDrop(ModBlocks.PLUM_CROP,         ModItems.PLUM,         ModItems.PLUM_SEEDS);
            //cropDrop(ModBlocks.POMEGRANATE_CROP,  ModItems.POMEGRANATE,  ModItems.POMEGRANATE_SEEDS);

            // Banana — self planting
            //selfCropDrop(ModBlocks.BANANA_CROP, ModItems.BANANA);
        }

        private void cropDrop(DeferredBlock<Block> block,
                              DeferredItem<Item> crop,
                              DeferredItem<Item> seeds) {
            add(block.get(), createCropDrops(
                    block.get(),
                    crop.get(),
                    seeds.get(),
                    net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition
                            .hasBlockStateProperties(block.get())
                            .setProperties(
                                    net.minecraft.advancements.critereon.StatePropertiesPredicate
                                            .Builder.properties()
                                            .hasProperty(GHCropBlock.AGE, 7)
                            )
            ));
        }

        private void selfCropDrop(DeferredBlock<Block> block,
                                  DeferredItem<Item> item) {
            add(block.get(), createSingleItemTable(item.get()));
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return List.of(
                    ModBlocks.AUBERGINE_CROP.get(),
                    ModBlocks.BELL_PEPPER_CROP.get(),
                    ModBlocks.BROCCOLI_CROP.get(),
                    ModBlocks.CABBAGE_CROP.get(),
                    ModBlocks.CORN_CROP.get(),
                    ModBlocks.COURGETTE_CROP.get(),
                    ModBlocks.CUCUMBER_CROP.get(),
                    ModBlocks.GARLIC_CROP.get(),
                    ModBlocks.LETTUCE_CROP.get(),
                    ModBlocks.ONION_CROP.get(),
                    ModBlocks.SWEET_POTATO_CROP.get(),
                    ModBlocks.TOMATO_CROP.get()
                    //ModBlocks.BANANA_CROP.get(),
                    //ModBlocks.FIG_CROP.get(),
                    //ModBlocks.GUAVA_CROP.get(),
                    //ModBlocks.KIWI_CROP.get(),
                    //ModBlocks.MANGO_CROP.get(),
                    //ModBlocks.ORANGE_CROP.get(),
                    //ModBlocks.PAPAYA_CROP.get(),
                    //ModBlocks.PEACH_CROP.get(),
                    //ModBlocks.PEAR_CROP.get(),
                    //ModBlocks.PERSIMMON_CROP.get(),
                    //ModBlocks.PLUM_CROP.get(),
                    //ModBlocks.POMEGRANATE_CROP.get()
            );
        }
    }
}
