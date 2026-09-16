package net.enderwish.Atmospheric_Overhaul_Subpack.client.particle;

import net.enderwish.Atmospheric_Overhaul_Subpack.AtmosphericOverhaulSubpack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

@EventBusSubscriber(modid = AtmosphericOverhaulSubpack.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientParticleFactories {

    @SubscribeEvent
    public static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.RAIN_DROP.get(), RainDropParticle.Provider::new);
        event.registerSpriteSet(ModParticles.RAIN_SPLASH.get(), RainSplashParticle.Provider::new);
        event.registerSpriteSet(ModParticles.SWIRLING_LEAVES.get(), SwirlingLeavesParticle.Provider::new);
        event.registerSpriteSet(ModParticles.DUST_MOTE.get(), DustMoteParticle.Provider::new);
        event.registerSpriteSet(ModParticles.FOG_WISP.get(), FogWispParticle.Provider::new);
    }
}
