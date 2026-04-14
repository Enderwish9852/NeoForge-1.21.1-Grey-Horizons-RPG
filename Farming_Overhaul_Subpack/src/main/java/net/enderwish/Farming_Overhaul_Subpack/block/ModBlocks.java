package net.enderwish.Farming_Overhaul_Subpack.block;

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

    // --- THE NESTED BRANCH CLASS ---
    public static class Branch extends AbstractBranchBlock {
        public Branch(MapColor color) {
            super(BlockBehaviour.Properties.of()
                    .mapColor(color)
                    .strength(2.0f, 3.0f)
                    .sound(SoundType.WOOD)
                    .ignitedByLava()
                    .noOcclusion());
        }
    }

    // --- VANILLA OVERHAUL BRANCHES ---
    public static final DeferredBlock<Block> OAK_BRANCH = BLOCKS.register("oak_branch",
            () -> new Branch(MapColor.WOOD));
    public static final DeferredBlock<Block> BIRCH_BRANCH = BLOCKS.register("birch_branch",
            () -> new Branch(MapColor.SAND));
    public static final DeferredBlock<Block> SPRUCE_BRANCH = BLOCKS.register("spruce_branch",
            () -> new Branch(MapColor.PODZOL));
    public static final DeferredBlock<Block> JUNGLE_BRANCH = BLOCKS.register("jungle_branch",
            () -> new Branch(MapColor.DIRT));
    public static final DeferredBlock<Block> ACACIA_BRANCH = BLOCKS.register("acacia_branch",
            () -> new Branch(MapColor.COLOR_ORANGE));
    public static final DeferredBlock<Block> DARK_OAK_BRANCH = BLOCKS.register("dark_oak_branch",
            () -> new Branch(MapColor.COLOR_BROWN));

    // --- FRUIT TREE BRANCHES ---
    public static final DeferredBlock<Block> APPLE_BRANCH = BLOCKS.register("apple_branch",
            () -> new Branch(MapColor.WOOD));

    // --- THE GROWTH NODE (The Tree Anchor) ---
    // This acts like the "Root" of the tree and holds the BlockEntity
    public static final DeferredBlock<Block> GROWTH_NODE = BLOCKS.register("growth_node",
            () -> new GrowthNodeBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DIRT)
                    .strength(0.6f)
                    .sound(SoundType.GRAVEL)));

    public static final DeferredBlock<Block> OAK_ADAPTIVE_SAPLING = BLOCKS.register("oak_adaptive_sapling",
            () -> new AdaptiveSaplingBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .noCollission()
                    .instabreak()
                    .sound(SoundType.GRASS)
                    .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)));

    // Simple register method for the main class
    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}