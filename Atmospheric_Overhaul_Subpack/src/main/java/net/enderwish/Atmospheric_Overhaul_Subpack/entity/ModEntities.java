package net.enderwish.Atmospheric_Overhaul_Subpack.entity;

import net.enderwish.Atmospheric_Overhaul_Subpack.AtmosphericOverhaulSubpack;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * ModEntities
 *
 * The floating ice: a clump of ice that has broken off a river's
 * surface and is drifting away on the current.
 */
public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, AtmosphericOverhaulSubpack.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<FloatingIceEntity>> FLOATING_ICE =
            ENTITY_TYPES.register("floating_ice", () -> EntityType.Builder
                    .<FloatingIceEntity>of(FloatingIceEntity::new, MobCategory.MISC)
                    .sized(0.9f, 0.4f)
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .build("floating_ice"));

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }
}
