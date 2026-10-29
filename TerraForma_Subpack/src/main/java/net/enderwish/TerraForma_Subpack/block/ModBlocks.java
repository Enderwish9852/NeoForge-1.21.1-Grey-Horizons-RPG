package net.enderwish.TerraForma_Subpack.block;

import net.enderwish.TerraForma_Subpack.TerraFormaSubpack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * ModBlocks — Custom geology blocks for Grey Horizons terrain.
 *
 * Custom blocks give each biome unique visual character:
 *   Salt, Volcanic Ash, Chalk, Marble, Slate, Crystalline,
 *   Petrified Wood, Salt Ore
 */
public class ModBlocks {

    public static final String MODID = "gh_terraforma";

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(MODID);

    // ── Salt Family ──────────────────────────────────────────────────────────

    /** White block found in Salt Flats. Mineable for salt items. */
    public static final DeferredBlock<Block> SALT_BLOCK = BLOCKS.register("salt_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(1.5f, 3.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    /** Salt ore — salt block with crystalline veins. Drops salt item. */
    public static final DeferredBlock<Block> SALT_ORE = BLOCKS.register("salt_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.0f, 6.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    // ── Volcanic Family ──────────────────────────────────────────────────────

    /** Grey dusty block that falls like sand. Found in Ash Plains. */
    public static final DeferredBlock<Block> VOLCANIC_ASH_BLOCK = BLOCKS.register("volcanic_ash_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(0.1f, 0.2f)
                    .sound(SoundType.SAND)
                    .noOcclusion()
                    .randomTicks()));

    /** Basalt with polished variant for volcanic crust. */
    public static final DeferredBlock<Block> POLISHED_BASALT_BLOCK = BLOCKS.register("polished_basalt_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(1.5f, 6.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    // ── Sedimentary Family ───────────────────────────────────────────────────

    /** White block with grey streaks. Found in Coastal Cliffs. */
    public static final DeferredBlock<Block> CHALK_BLOCK = BLOCKS.register("chalk_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(0.6f, 1.2f)
                    .sound(SoundType.STONE)));

    /** White block with grey veins. Found in Highland Orchard. */
    public static final DeferredBlock<Block> MARBLE_BLOCK = BLOCKS.register("marble_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(1.5f, 6.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    /** Dark grey block. Found in Birch Highlands, Highland Orchard. */
    public static final DeferredBlock<Block> SLATE_BLOCK = BLOCKS.register("slate_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(1.5f, 6.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    // ── Special Blocks ───────────────────────────────────────────────────────

    /** Translucent glowing block. Found in Overgrown Ruins, Petrified Forest. */
    public static final DeferredBlock<Block> CRYSTALLINE_BLOCK = BLOCKS.register("crystalline_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(1.5f, 3.0f)
                    .sound(SoundType.GLASS)
                    .lightLevel((state) -> 7)
                    .noOcclusion()));

    /** Stone-colored wood that doesn't burn. Found in Petrified Forest. */
    public static final DeferredBlock<Block> PETRIFIED_WOOD_BLOCK = BLOCKS.register("petrified_wood_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.5f, 9.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()
                    .ignitedByLava()));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
