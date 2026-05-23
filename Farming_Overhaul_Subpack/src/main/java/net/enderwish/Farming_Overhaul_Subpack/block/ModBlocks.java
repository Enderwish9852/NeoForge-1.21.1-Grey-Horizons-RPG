package net.enderwish.Farming_Overhaul_Subpack.block;

import net.enderwish.Farming_Overhaul_Subpack.block.clay_pot.ClayPotBlock;
import net.enderwish.Farming_Overhaul_Subpack.block.clay_pot.WetClayPotBlock;
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

    // Simple register method for the main class
    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}