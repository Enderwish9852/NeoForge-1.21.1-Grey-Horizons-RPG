package net.enderwish.Farming_Overhaul_Subpack.core.spoilage;

import net.enderwish.Farming_Overhaul_Subpack.FarmingOverhaulSubpack;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModDataComponents {

    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, FarmingOverhaulSubpack.MODID);

    /**
     * The spoilage component — attached to every spoilable food item stack.
     * Persists through save/load via the Codec.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SpoilageComponent>> SPOILAGE =
            DATA_COMPONENTS.register("spoilage", () ->
                    DataComponentType.<SpoilageComponent>builder()
                            .persistent(SpoilageComponent.CODEC)
                            .build()
            );

    public static void register(IEventBus eventBus) {
        DATA_COMPONENTS.register(eventBus);
    }
}
