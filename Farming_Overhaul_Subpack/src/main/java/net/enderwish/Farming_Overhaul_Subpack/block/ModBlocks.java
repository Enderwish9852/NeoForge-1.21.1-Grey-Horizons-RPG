package net.enderwish.Farming_Overhaul_Subpack.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final String MODID = "gh_farming_overhaul";

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(MODID);



    // Simple register method for the main class
    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}