package net.enderwish.Farming_Overhaul_Subpack.init;

import net.enderwish.Farming_Overhaul_Subpack.FarmingOverhaulSubpack;
import net.enderwish.Farming_Overhaul_Subpack.block.clay_pot.ClayPotBlockEntity;
import net.enderwish.Farming_Overhaul_Subpack.block.ModBlocks;
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

    public static void register(net.neoforged.bus.api.IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}