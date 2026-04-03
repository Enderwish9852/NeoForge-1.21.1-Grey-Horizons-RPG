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
     * This manages the 24,000 tick day cycle and d100 weather rolls.
     */
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        // Filter for Server-side Overworld only to prevent logic duplication
        if (event.getLevel() instanceof ServerLevel level && level.dimension() == Level.OVERWORLD) {
            WeatherManager.getInstance().tick(level);
        }
    }

    /**
     * Sync on Join: Ensures new players receive the current ClimateData immediately.
     * Fixed: Now sends a direct packet to the joining player to ensure snow, ice,
     * and seasonal colors render correctly from the very first frame.
     */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // 1. Get current master data from the manager
            ClimateData currentData = WeatherManager.getInstance().getCurrentData(player.serverLevel());

            // 2. Send direct packet to the joining player
            // This forces the client cache to update before the world renders.
            ModMessages.sendToPlayer(new ClimateSyncPacket(currentData), player);

            // 3. Maintenance sync for the rest of the server
            WeatherManager.getInstance().syncToAll(player.serverLevel());
        }
    }
}