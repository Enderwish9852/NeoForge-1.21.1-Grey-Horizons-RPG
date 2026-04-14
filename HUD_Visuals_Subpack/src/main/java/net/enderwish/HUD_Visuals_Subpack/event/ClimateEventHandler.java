package net.enderwish.HUD_Visuals_Subpack.event;

import net.enderwish.HUD_Visuals_Subpack.HUDVisualsSubpack;
import net.enderwish.HUD_Visuals_Subpack.api.ClimateData;
import net.enderwish.HUD_Visuals_Subpack.core.weather.WeatherManager;
import net.enderwish.HUD_Visuals_Subpack.network.ClimateSyncPacket;
import net.enderwish.HUD_Visuals_Subpack.network.ModMessages;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * The "Ear" of the mod.
 * Bridges Minecraft game loops with the WeatherManager logic.
 */
@EventBusSubscriber(modid = HUDVisualsSubpack.MOD_ID)
public class ClimateEventHandler {

    /**
     * Heartbeat: Ticks the WeatherManager on the server side.
     * Manages the 24,000 tick day cycle and seasonal growth leaps.
     */
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension() == Level.OVERWORLD) {
            WeatherManager.getInstance().tick(level);

            if (level.getDayTime() % 24000 == 0) {
                long totalDays = level.getDayTime() / 24000;

                if (totalDays > 0 && totalDays % 20 == 0) {
                    // FIRE THE EVENT: This sends a message to any mod listening
                    net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(
                            new net.enderwish.HUD_Visuals_Subpack.event.SeasonChangeEvent(level)
                    );
                }
            }
        }
    }

    /**
     * Sync on Join: Ensures new players receive the current ClimateData immediately.
     */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // 1. Get current master data from the manager
            ClimateData currentData = WeatherManager.getInstance().getCurrentData(player.serverLevel());

            // 2. Send direct packet to the joining player
            ModMessages.sendToPlayer(new ClimateSyncPacket(currentData), player);

            // 3. Maintenance sync for the rest of the server
            WeatherManager.getInstance().syncToAll(player.serverLevel());
        }
    }
}