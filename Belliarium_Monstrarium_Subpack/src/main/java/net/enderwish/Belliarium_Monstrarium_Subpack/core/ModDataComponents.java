package net.enderwish.Belliarium_Monstrarium_Subpack.core;

import com.mojang.serialization.Codec;
import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModDataComponents {

    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, BelliariumMonstrariumSubpack.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<RepairChargesComponent>> REPAIR_CHARGES =
            DATA_COMPONENTS.register("repair_charges", () ->
                    DataComponentType.<RepairChargesComponent>builder().persistent(RepairChargesComponent.CODEC).build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> SKILL_ID =
            DATA_COMPONENTS.register("skill_id", () ->
                    DataComponentType.<String>builder().persistent(Codec.STRING).build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> LEARNING_PROGRESS =
            DATA_COMPONENTS.register("learning_progress_ticks", () ->
                    DataComponentType.<Integer>builder().persistent(Codec.INT).build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> LEARNING_COMPLETE =
            DATA_COMPONENTS.register("learning_complete", () ->
                    DataComponentType.<Boolean>builder().persistent(Codec.BOOL).build());

    public static void register(IEventBus eventBus) {
        DATA_COMPONENTS.register(eventBus);
    }
}
