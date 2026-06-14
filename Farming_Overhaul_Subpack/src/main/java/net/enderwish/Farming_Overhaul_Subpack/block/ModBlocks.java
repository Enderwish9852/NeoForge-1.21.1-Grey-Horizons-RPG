package net.enderwish.Farming_Overhaul_Subpack.block;

import net.enderwish.Farming_Overhaul_Subpack.block.clay_pot.ClayPotBlock;
import net.enderwish.Farming_Overhaul_Subpack.block.clay_pot.WetClayPotBlock;
import net.enderwish.Farming_Overhaul_Subpack.block.crop.GHCropBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final String MODID = "gh_farming_overhaul";

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(MODID);

    // ── Clay Pot ──────────────────────────────────────────────────────────────

    public static final DeferredBlock<Block> WET_CLAY_POT = BLOCKS.register("wet_clay_pot",
            () -> new WetClayPotBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_ORANGE)
                    .strength(0.5f)
                    .sound(SoundType.GRAVEL)
                    .noOcclusion()));

    public static final DeferredBlock<Block> CLAY_POT = BLOCKS.register("clay_pot",
            () -> new ClayPotBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_ORANGE)
                    .strength(1.0f)
                    .sound(SoundType.STONE)
                    .noOcclusion()));

    // ── Cutting Board ─────────────────────────────────────────────────────────

    public static final DeferredBlock<Block> CUTTING_BOARD = BLOCKS.register("cutting_board",
            () -> new net.enderwish.Farming_Overhaul_Subpack.block.cutting_board.CuttingBoardBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.WOOD)
                            .strength(2.0f)
                            .sound(SoundType.WOOD)
                            .noOcclusion()));

    // ── Vegetable Crops ───────────────────────────────────────────────────────

    public static final DeferredBlock<Block> AUBERGINE_CROP = BLOCKS.register("aubergine_crop",
            () -> new GHCropBlock("aubergine", BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .noCollission()
                    .randomTicks()
                    .instabreak()
                    .sound(SoundType.CROP)
                    .noOcclusion()));

    public static final DeferredBlock<Block> BELL_PEPPER_CROP = BLOCKS.register("bell_pepper_crop",
            () -> new GHCropBlock("bell_pepper", BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .noCollission()
                    .randomTicks()
                    .instabreak()
                    .sound(SoundType.CROP)
                    .noOcclusion()));

    public static final DeferredBlock<Block> BROCCOLI_CROP = BLOCKS.register("broccoli_crop",
            () -> new GHCropBlock("broccoli", BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .noCollission()
                    .randomTicks()
                    .instabreak()
                    .sound(SoundType.CROP)
                    .noOcclusion()));

    public static final DeferredBlock<Block> CABBAGE_CROP = BLOCKS.register("cabbage_crop",
            () -> new GHCropBlock("cabbage", BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .noCollission()
                    .randomTicks()
                    .instabreak()
                    .sound(SoundType.CROP)
                    .noOcclusion()));

    public static final DeferredBlock<Block> CORN_CROP = BLOCKS.register("corn_crop",
            () -> new GHCropBlock("corn", BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .noCollission()
                    .randomTicks()
                    .instabreak()
                    .sound(SoundType.CROP)
                    .noOcclusion()));

    public static final DeferredBlock<Block> COURGETTE_CROP = BLOCKS.register("courgette_crop",
            () -> new GHCropBlock("courgette", BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .noCollission()
                    .randomTicks()
                    .instabreak()
                    .sound(SoundType.CROP)
                    .noOcclusion()));

    public static final DeferredBlock<Block> CUCUMBER_CROP = BLOCKS.register("cucumber_crop",
            () -> new GHCropBlock("cucumber", BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .noCollission()
                    .randomTicks()
                    .instabreak()
                    .sound(SoundType.CROP)
                    .noOcclusion()));

    public static final DeferredBlock<Block> GARLIC_CROP = BLOCKS.register("garlic_crop",
            () -> new GHCropBlock("garlic", BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .noCollission()
                    .randomTicks()
                    .instabreak()
                    .sound(SoundType.CROP)
                    .noOcclusion()));

    public static final DeferredBlock<Block> LETTUCE_CROP = BLOCKS.register("lettuce_crop",
            () -> new GHCropBlock("lettuce", BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .noCollission()
                    .randomTicks()
                    .instabreak()
                    .sound(SoundType.CROP)
                    .noOcclusion()));

    public static final DeferredBlock<Block> ONION_CROP = BLOCKS.register("onion_crop",
            () -> new GHCropBlock("onion", BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .noCollission()
                    .randomTicks()
                    .instabreak()
                    .sound(SoundType.CROP)
                    .noOcclusion()));

    public static final DeferredBlock<Block> SWEET_POTATO_CROP = BLOCKS.register("sweet_potato_crop",
            () -> new GHCropBlock("sweet_potato", BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .noCollission()
                    .randomTicks()
                    .instabreak()
                    .sound(SoundType.CROP)
                    .noOcclusion()));

    public static final DeferredBlock<Block> TOMATO_CROP = BLOCKS.register("tomato_crop",
            () -> new GHCropBlock("tomato", BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .noCollission()
                    .randomTicks()
                    .instabreak()
                    .sound(SoundType.CROP)
                    .noOcclusion()));

    // ── Fruit Crops ───────────────────────────────────────────────────────────

    public static final DeferredBlock<Block> BANANA_CROP = BLOCKS.register("banana_crop",
            () -> new GHCropBlock("banana", BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .noCollission()
                    .randomTicks()
                    .instabreak()
                    .sound(SoundType.CROP)
                    .noOcclusion()));

    public static final DeferredBlock<Block> FIG_CROP = BLOCKS.register("fig_crop",
            () -> new GHCropBlock("fig", BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .noCollission()
                    .randomTicks()
                    .instabreak()
                    .sound(SoundType.CROP)
                    .noOcclusion()));

    public static final DeferredBlock<Block> GUAVA_CROP = BLOCKS.register("guava_crop",
            () -> new GHCropBlock("guava", BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .noCollission()
                    .randomTicks()
                    .instabreak()
                    .sound(SoundType.CROP)
                    .noOcclusion()));

    public static final DeferredBlock<Block> KIWI_CROP = BLOCKS.register("kiwi_crop",
            () -> new GHCropBlock("kiwi", BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .noCollission()
                    .randomTicks()
                    .instabreak()
                    .sound(SoundType.CROP)
                    .noOcclusion()));

    public static final DeferredBlock<Block> MANGO_CROP = BLOCKS.register("mango_crop",
            () -> new GHCropBlock("mango", BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .noCollission()
                    .randomTicks()
                    .instabreak()
                    .sound(SoundType.CROP)
                    .noOcclusion()));

    public static final DeferredBlock<Block> ORANGE_CROP = BLOCKS.register("orange_crop",
            () -> new GHCropBlock("orange", BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .noCollission()
                    .randomTicks()
                    .instabreak()
                    .sound(SoundType.CROP)
                    .noOcclusion()));

    public static final DeferredBlock<Block> PAPAYA_CROP = BLOCKS.register("papaya_crop",
            () -> new GHCropBlock("papaya", BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .noCollission()
                    .randomTicks()
                    .instabreak()
                    .sound(SoundType.CROP)
                    .noOcclusion()));

    public static final DeferredBlock<Block> PEACH_CROP = BLOCKS.register("peach_crop",
            () -> new GHCropBlock("peach", BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .noCollission()
                    .randomTicks()
                    .instabreak()
                    .sound(SoundType.CROP)
                    .noOcclusion()));

    public static final DeferredBlock<Block> PEAR_CROP = BLOCKS.register("pear_crop",
            () -> new GHCropBlock("pear", BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .noCollission()
                    .randomTicks()
                    .instabreak()
                    .sound(SoundType.CROP)
                    .noOcclusion()));

    public static final DeferredBlock<Block> PERSIMMON_CROP = BLOCKS.register("persimmon_crop",
            () -> new GHCropBlock("persimmon", BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .noCollission()
                    .randomTicks()
                    .instabreak()
                    .sound(SoundType.CROP)
                    .noOcclusion()));

    public static final DeferredBlock<Block> PLUM_CROP = BLOCKS.register("plum_crop",
            () -> new GHCropBlock("plum", BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .noCollission()
                    .randomTicks()
                    .instabreak()
                    .sound(SoundType.CROP)
                    .noOcclusion()));

    public static final DeferredBlock<Block> POMEGRANATE_CROP = BLOCKS.register("pomegranate_crop",
            () -> new GHCropBlock("pomegranate", BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .noCollission()
                    .randomTicks()
                    .instabreak()
                    .sound(SoundType.CROP)
                    .noOcclusion()));

    // ── Fertilized Farmland ───────────────────────────────────────────────────

    public static final DeferredBlock<Block> FERTILIZED_FARMLAND = BLOCKS.register(
            "fertilized_farmland",
            () -> new net.enderwish.Farming_Overhaul_Subpack.block.farmland
                    .FertilizedFarmlandBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.DIRT)
                            .strength(0.6f)
                            .sound(SoundType.GRAVEL)
                            .randomTicks()));

    // ── GH Composter ──────────────────────────────────────────────────────────

    public static final DeferredBlock<Block> GH_COMPOSTER = BLOCKS.register(
            "gh_composter",
            () -> new net.enderwish.Farming_Overhaul_Subpack.block.composter
                    .GHComposterBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.WOOD)
                            .strength(0.6f)
                            .sound(SoundType.WOOD)));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}