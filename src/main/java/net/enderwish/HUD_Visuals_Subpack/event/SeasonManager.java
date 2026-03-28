package net.enderwish.HUD_Visuals_Subpack.event;

import net.enderwish.HUD_Visuals_Subpack.core.Season;
import net.enderwish.HUD_Visuals_Subpack.core.SeasonData;
import net.enderwish.HUD_Visuals_Subpack.network.SeasonSyncPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Random;

/**
 * Handles season progression and seasonal weather logic.
 */
@EventBusSubscriber(modid = "hud_visuals_subpack")
public class SeasonManager {

    private static final Random RANDOM = new Random();

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        Level level = event.getLevel();

        if (level instanceof ServerLevel serverLevel && level.dimension() == Level.OVERWORLD) {
            SeasonData data = SeasonData.get(serverLevel);

            // 1. Advance Day Logic
            if (serverLevel.getGameTime() % 24000 == 0) {
                data.tick(serverLevel);
                data.setDirty();
                syncToAll(serverLevel, data);
            }

            // 2. Natural Weather Control Logic
            // This runs when the vanilla weather timer hits zero or a command changes it
            if (serverLevel.getGameTime() % 20 == 0) { // Check every second for consistency
                Season currentSeason = data.getCurrentSeason();

                // Safety Override: If it's Thundering (Blizzard) but NOT Winter, force it to stop.
                // This catches /weather thunder commands used in Summer/Spring/Autumn.
                if (serverLevel.isThundering() && currentSeason != Season.WINTER) {
                    serverLevel.setWeatherParameters(6000, 0, false, false);
                }

                // Season-Specific Weather Pools logic could be expanded here if
                // you want to trigger custom weather starts naturally.
            }

            // 3. Sync heartbeat (Every 5 seconds)
            if (serverLevel.getGameTime() % 100 == 0) {
                syncToAll(serverLevel, data);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerLevel level = player.serverLevel();
            SeasonData data = SeasonData.get(level);
            syncToPlayer(player, level, data);
        }
    }

    public static void setSeason(ServerLevel level, Season season) {
        SeasonData data = SeasonData.get(level);
        data.setCurrentSeason(season);
        data.setDirty();
        syncToAll(level, data);
    }

    public static void setDay(ServerLevel level, int day) {
        SeasonData data = SeasonData.get(level);
        data.setSeasonDay(day);
        data.setDirty();
        syncToAll(level, data);
    }

    private static String getCurrentWeatherString(ServerLevel level) {
        if (level.isThundering()) return "blizzard";
        if (level.isRaining()) return "rain";
        return "clear";
    }

    private static void syncToAll(ServerLevel level, SeasonData data) {
        PacketDistributor.sendToAllPlayers(new SeasonSyncPacket(
                data.getCurrentSeason(),
                data.getDisplayDay(),
                getCurrentWeatherString(level)
        ));
    }

    private static void syncToPlayer(ServerPlayer player, ServerLevel level, SeasonData data) {
        PacketDistributor.sendToPlayer(player, new SeasonSyncPacket(
                data.getCurrentSeason(),
                data.getDisplayDay(),
                getCurrentWeatherString(level)
        ));
    }
}