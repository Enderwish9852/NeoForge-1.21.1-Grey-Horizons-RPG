package net.enderwish.Farming_Overhaul_Subpack.init;

import net.enderwish.Farming_Overhaul_Subpack.FarmingOverhaulSubpack;
import net.enderwish.Farming_Overhaul_Subpack.block.clay_pot.ClayPotBlockEntity;
import net.enderwish.Farming_Overhaul_Subpack.block.ModBlocks;
import net.enderwish.Farming_Overhaul_Subpack.block.composter.GHComposterBlockEntity;
import net.enderwish.Farming_Overhaul_Subpack.block.cutting_board.CuttingBoardBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, FarmingOverhaulSubpack.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ClayPotBlockEntity>> CLAY_POT =
            BLOCK_ENTITIES.register("clay_pot", () ->
                    BlockEntityType.Builder.of(
                            ClayPotBlockEntity::new,
                            ModBlocks.CLAY_POT.get()
                    ).build(null));

    public static final DeferredHolder<BlockEntityType<?>,
            BlockEntityType<CuttingBoardBlockEntity>> CUTTING_BOARD =
            BLOCK_ENTITIES.register("cutting_board", () ->
                    BlockEntityType.Builder.of(
                            CuttingBoardBlockEntity::new,
                            ModBlocks.CUTTING_BOARD.get()
                    ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GHComposterBlockEntity>>
            GH_COMPOSTER = BLOCK_ENTITIES.register("gh_composter",
            () -> BlockEntityType.Builder.of(
                            GHComposterBlockEntity::new,
                            ModBlocks.GH_COMPOSTER.get())
                    .build(null));

    public static void register(net.neoforged.bus.api.IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}