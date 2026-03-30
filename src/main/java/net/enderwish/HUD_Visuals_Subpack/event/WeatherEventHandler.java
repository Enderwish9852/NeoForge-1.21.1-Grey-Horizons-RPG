package net.enderwish.HUD_Visuals_Subpack.event;

import net.enderwish.HUD_Visuals_Subpack.core.WeatherData;
import net.enderwish.HUD_Visuals_Subpack.core.WeatherManager;
import net.enderwish.HUD_Visuals_Subpack.network.ModMessages;
import net.enderwish.HUD_Visuals_Subpack.network.WeatherSyncPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Handles server-side weather logic and synchronization.
 * Updated for NeoForge 1.21.1.
 */
@EventBusSubscriber(modid = "hud_visuals") // Ensure this matches your actual mod id
public class WeatherEventHandler {

    /**
     * Updates weather logic every tick.
     * In 1.21.1, ServerTickEvent.Post is often preferred for global logic
     * to ensure all level data is processed.
     */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        // WeatherManager is typically global, so we process it once per server tick
        WeatherManager manager = WeatherManager.getInstance();

        // We can use the server instance to get the overworld or iterate levels
        // For simple weather, we usually track the primary level (Overworld)
        // If your manager needs a specific level:
        // manager.tick(event.getServer().overworld());

        // Sync logic: Every 20 ticks (1 second)
        // Using the server's tick count to broadcast to all players online
        if (event.getServer().getTickCount() % 20 == 0) {
            syncWeatherToAll(event.getServer().getPlayerList().getPlayers());
        }
    }

    /**
     * Syncs current weather to a player as soon as they log in.
     */
    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            WeatherData data = WeatherManager.getInstance().getCurrentWeatherData();
            ModMessages.sendToPlayer(new WeatherSyncPacket(data), player);
        }
    }

    /**
     * Helper to broadcast the current weather state to a list of players.
     */
    private static void syncWeatherToAll(Iterable<ServerPlayer> players) {
        WeatherData data = WeatherManager.getInstance().getCurrentWeatherData();
        WeatherSyncPacket packet = new WeatherSyncPacket(data);

        for (ServerPlayer player : players) {
            ModMessages.sendToPlayer(packet, player);
        }
    }
}