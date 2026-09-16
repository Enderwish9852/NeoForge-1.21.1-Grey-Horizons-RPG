package net.enderwish.Atmospheric_Overhaul_Subpack.client.particle;

import net.enderwish.Atmospheric_Overhaul_Subpack.AtmosphericOverhaulSubpack;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModParticles {

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, AtmosphericOverhaulSubpack.MOD_ID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> RAIN_DROP =
            PARTICLE_TYPES.register("rain_drop", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> RAIN_SPLASH =
            PARTICLE_TYPES.register("rain_splash", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SWIRLING_LEAVES =
            PARTICLE_TYPES.register("swirling_leaves", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DUST_MOTE =
            PARTICLE_TYPES.register("dust_mote", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FOG_WISP =
            PARTICLE_TYPES.register("fog_wisp", () -> new SimpleParticleType(false));

    public static void register(IEventBus modEventBus) {
        PARTICLE_TYPES.register(modEventBus);
    }
}
