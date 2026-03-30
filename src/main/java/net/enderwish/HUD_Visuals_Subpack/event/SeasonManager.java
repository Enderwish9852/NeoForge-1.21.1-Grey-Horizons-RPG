package net.enderwish.HUD_Visuals_Subpack.event;

import net.enderwish.HUD_Visuals_Subpack.client.ClientSeasonHandler;
import net.enderwish.HUD_Visuals_Subpack.core.Season;
import net.enderwish.HUD_Visuals_Subpack.core.SeasonData;
import net.enderwish.HUD_Visuals_Subpack.network.SeasonSyncPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = "hud_visuals_subpack")
public class SeasonManager {

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        Level level = event.getLevel();

        if (level instanceof ServerLevel serverLevel && level.dimension() == Level.OVERWORLD) {
            SeasonData data = SeasonData.get(serverLevel);
            long gameTime = serverLevel.getGameTime();

            // 1. Advance Day Logic
            if (gameTime % 24000 == 1) {
                Season oldSeason = data.getCurrentSeason();
                data.tick(serverLevel);
                data.setDirty();

                Season newSeason = data.getCurrentSeason();

                if (oldSeason != newSeason) {
                    triggerSeasonTransition(serverLevel, oldSeason, newSeason);
                } else {
                    syncToAll(serverLevel, data);
                }
            }

            // 2. Sync heartbeat (Every 5 seconds)
            if (gameTime % 100 == 0) {
                syncToAll(serverLevel, data);
            }
        }
    }

    /**
     * Called by commands to manually force a season change.
     */
    public static void setSeason(ServerLevel level, Season season) {
        SeasonData data = SeasonData.get(level);
        Season oldSeason = data.getCurrentSeason();
        data.setCurrentSeason(season);
        data.setDirty();

        if (oldSeason != season) {
            triggerSeasonTransition(level, oldSeason, season);
        } else {
            syncToAll(level, data);
        }
    }

    /**
     * Called by commands to manually set the specific day.
     */
    public static void setDay(ServerLevel level, int day) {
        SeasonData data = SeasonData.get(level);
        data.setSeasonDay(day);
        data.setDirty();
        syncToAll(level, data);
    }

    /**
     * Returns a float where:
     * Below 0.15 = Can Snow/Freeze
     * Above 0.15 = Rain/Melt
     */
    public static float getSubTemperature(Level level) {
        SeasonData data = (level instanceof ServerLevel sl) ? SeasonData.get(sl) : null;

        // If data is null (client side), we try to get info from the client handler
        Season season = (data != null) ? data.getCurrentSeason() : ClientSeasonHandler.getSeason();
        int day = (data != null) ? data.getDisplayDay() : 1;

        return switch (season) {
            case SPRING -> (day <= 5) ? -0.1f : (day <= 15 ? 0.2f : 0.5f);
            case SUMMER -> (day >= 8 && day <= 13) ? 1.2f : 0.8f;
            case AUTUMN -> (day <= 10) ? 0.4f : (day <= 15 ? 0.14f : -0.1f);
            case WINTER -> (day <= 5) ? -0.2f : -0.8f;
        };
    }

    /**
     * Determines the phase for HUD display (Early, Mid, Late).
     */
    public static String getSeasonPhase(Level level) {
        SeasonData data = (level instanceof ServerLevel sl) ? SeasonData.get(sl) : null;
        int day = (data != null) ? data.getDisplayDay() : 1;

        if (day <= 6) return "Early";
        if (day <= 14) return "Mid";
        return "Late";
    }

    public static void triggerSeasonTransition(ServerLevel level, Season from, Season to) {
        SeasonData data = SeasonData.get(level);
        syncToAll(level, data);

        for (ServerPlayer player : level.players()) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BEACON_ACTIVATE, SoundSource.AMBIENT, 1.0f, 1.0f);
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

    public static void syncToPlayer(ServerPlayer player, ServerLevel level, SeasonData data) {
        PacketDistributor.sendToPlayer(player, new SeasonSyncPacket(
                data.getCurrentSeason(),
                data.getDisplayDay(),
                getCurrentWeatherString(level)
        ));
    }

    public static Season getSeason(Level level) {
        if (level instanceof ServerLevel serverLevel) {
            return SeasonData.get(serverLevel).getCurrentSeason();
        } else {
            return ClientSeasonHandler.getSeason();
        }
    }
}