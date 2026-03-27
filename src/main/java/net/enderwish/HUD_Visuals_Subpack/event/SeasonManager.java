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

/**
 * Handles the logic for updating seasons over time and manual command overrides.
 */
@EventBusSubscriber(modid = "hud_visuals_subpack")
public class SeasonManager {

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        Level level = event.getLevel();

        // Only process on the server and in the overworld
        if (level instanceof ServerLevel serverLevel && level.dimension() == Level.OVERWORLD) {
            SeasonData data = SeasonData.get(serverLevel);

            Season oldSeason = data.getCurrentSeason();
            int oldDay = data.getDisplayDay();

            data.tick(serverLevel);

            // Sync if something changed (checked every second to save performance)
            if (level.getGameTime() % 20 == 0) {
                if (oldSeason != data.getCurrentSeason() || oldDay != data.getDisplayDay()) {
                    syncToAll(data);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.level() instanceof ServerLevel serverLevel) {
            SeasonData data = SeasonData.get(serverLevel);
            syncToPlayer(player, data);
        }
    }

    /**
     * Manually sets the season (called by SeasonCommand).
     */
    public static void setSeason(ServerLevel level, Season season) {
        SeasonData data = SeasonData.get(level);
        data.setCurrentSeason(season);
        data.setDirty();
        syncToAll(data);
    }

    /**
     * Manually sets the day (called by SeasonCommand).
     */
    public static void setDay(ServerLevel level, int day) {
        SeasonData data = SeasonData.get(level);
        data.setSeasonDay(day);
        data.setDirty();
        syncToAll(data);
    }

    private static void syncToAll(SeasonData data) {
        // NeoForge 1.21.1 uses PacketDistributor.ALL.send(...)
        PacketDistributor.sendToAllPlayers(new SeasonSyncPacket(
                data.getCurrentSeason(),
                data.getDisplayDay()
        ));
    }

    private static void syncToPlayer(ServerPlayer player, SeasonData data) {
        PacketDistributor.sendToPlayer(player, new SeasonSyncPacket(
                data.getCurrentSeason(),
                data.getDisplayDay()
        ));
    }
}