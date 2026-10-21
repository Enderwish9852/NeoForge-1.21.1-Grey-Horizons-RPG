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

    /** The flat ring a raindrop leaves on the ground. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> RAIN_SPLASH_RING =
        PARTICLE_TYPES.register("rain_splash_ring", () -> new SimpleParticleType(false));

    /** One of the droplets thrown up by a splash. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> RAIN_CROWN =
        PARTICLE_TYPES.register("rain_crown", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SWIRLING_LEAVES =
            PARTICLE_TYPES.register("swirling_leaves", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DUST_MOTE =
            PARTICLE_TYPES.register("dust_mote", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FOG_WISP =
            PARTICLE_TYPES.register("fog_wisp", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> WATER_RIPPLE =
            PARTICLE_TYPES.register("water_ripple", () -> new SimpleParticleType(false));

    public static void register(IEventBus modEventBus) {
        PARTICLE_TYPES.register(modEventBus);
    }
}
