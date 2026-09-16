package net.enderwish.Atmospheric_Overhaul_Subpack;

import net.enderwish.Atmospheric_Overhaul_Subpack.client.particle.ModParticles;
import net.enderwish.Atmospheric_Overhaul_Subpack.command.SeasonCommand;
import net.enderwish.Atmospheric_Overhaul_Subpack.command.WeatherCommand;
import net.enderwish.Atmospheric_Overhaul_Subpack.core.temperature.HeatBlockRegistry;
import net.enderwish.Atmospheric_Overhaul_Subpack.core.weather.WeatherRegistry;
import net.enderwish.Atmospheric_Overhaul_Subpack.core.weather.WindManager;
import net.enderwish.Atmospheric_Overhaul_Subpack.event.LocalPlayerEnvironmentHandler;
import net.enderwish.Atmospheric_Overhaul_Subpack.event.SeasonEnvironmentHandler;
import net.enderwish.Atmospheric_Overhaul_Subpack.event.SeasonEventHandler;
import net.enderwish.Atmospheric_Overhaul_Subpack.network.ModMessages;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod(AtmosphericOverhaulSubpack.MOD_ID)
public class AtmosphericOverhaulSubpack {

    public static final String MOD_ID = "gh_atmospheric";

    public AtmosphericOverhaulSubpack(IEventBus modEventBus) {

        modEventBus.addListener(this::registerNetworking);
        ModParticles.register(modEventBus);

        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(this::onLevelTick);

        NeoForge.EVENT_BUS.register(SeasonEventHandler.class);
        NeoForge.EVENT_BUS.register(SeasonEnvironmentHandler.class);
        NeoForge.EVENT_BUS.register(LocalPlayerEnvironmentHandler.class);

        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false,
                AddReloadListenerEvent.class,
                e -> {
                    e.addListener(WeatherRegistry.INSTANCE);
                    e.addListener(HeatBlockRegistry.INSTANCE);
                }
        );
    }

    private void registerNetworking(final RegisterPayloadHandlersEvent event) {
        ModMessages.register(event);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        WeatherCommand.register(event.getDispatcher());
        SeasonCommand.register(event.getDispatcher());
    }

    private void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (level.dimension() != ServerLevel.OVERWORLD) return;

        WindManager.INSTANCE.tick(level);
    }
}