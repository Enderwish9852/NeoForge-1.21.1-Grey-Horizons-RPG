package net.enderwish.HUD_Visuals_Subpack.event;

import net.enderwish.HUD_Visuals_Subpack.core.WeatherData;
import net.enderwish.HUD_Visuals_Subpack.core.WeatherManager;
import net.enderwish.HUD_Visuals_Subpack.network.ModMessages;
import net.enderwish.HUD_Visuals_Subpack.network.WeatherSyncPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = "hud_visuals_subpack")
public class WeatherEventHandler {

    // 1. Update weather logic every tick on the server
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        // We only want to run weather logic on the Server side
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            // Accessing the manager via getInstance() to fix the "static" error
            WeatherManager.getInstance().tick(serverLevel);

            // Sync every 20 ticks (1 second) to keep clients accurate
            if (serverLevel.getGameTime() % 20 == 0) {
                syncWeatherToAll(serverLevel);
            }
        }
    }

    // 2. When a player joins, send them the current weather immediately
    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Fetching current data from the instance
            WeatherData data = WeatherManager.getInstance().getCurrentWeatherData();
            ModMessages.sendToPlayer(new WeatherSyncPacket(data), player);
        }
    }

    // Helper method to broadcast weather to everyone in a dimension
    private static void syncWeatherToAll(ServerLevel level) {
        WeatherData data = WeatherManager.getInstance().getCurrentWeatherData();
        for (ServerPlayer player : level.players()) {
            ModMessages.sendToPlayer(new WeatherSyncPacket(data), player);
        }
    }
}