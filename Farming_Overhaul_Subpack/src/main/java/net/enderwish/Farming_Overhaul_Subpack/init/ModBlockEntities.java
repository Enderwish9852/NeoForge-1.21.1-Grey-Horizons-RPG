package net.enderwish.Farming_Overhaul_Subpack.init;

import net.enderwish.Farming_Overhaul_Subpack.block.ModBlocks;
import net.enderwish.Farming_Overhaul_Subpack.block.entity.GrowthNodeBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ModBlocks.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GrowthNodeBlockEntity>> GROWTH_NODE =
            BLOCK_ENTITIES.register("growth_node", () ->
                    BlockEntityType.Builder.of(GrowthNodeBlockEntity::new, ModBlocks.GROWTH_NODE.get()).build(null));
}