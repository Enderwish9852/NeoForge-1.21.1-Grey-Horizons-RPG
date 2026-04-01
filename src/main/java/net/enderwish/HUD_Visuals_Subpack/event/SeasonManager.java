package net.enderwish.HUD_Visuals_Subpack.event;

import net.enderwish.HUD_Visuals_Subpack.client.ClientSeasonHandler;
import net.enderwish.HUD_Visuals_Subpack.core.Season;
import net.enderwish.HUD_Visuals_Subpack.core.SeasonData;
import net.enderwish.HUD_Visuals_Subpack.network.SeasonSyncPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = "gh_hud_visuals")
public class SeasonManager {

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        Level level = event.getLevel();

        if (level instanceof ServerLevel serverLevel && level.dimension() == Level.OVERWORLD) {
            SeasonData data = SeasonData.get(serverLevel);
            long gameTime = serverLevel.getGameTime();

            // 1. Advance Day Logic (At the very end of the day)
            if (gameTime % 24000 == 23999) {
                Season oldSeason = data.getCurrentSeason();
                data.tick(serverLevel);
                data.setDirty();

                if (oldSeason != data.getCurrentSeason()) {
                    triggerSeasonTransition(serverLevel, oldSeason, data.getCurrentSeason());
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
     * The "Hook": Calculates temperature based on Biome + Season Offset.
     * Use this for HUD displays or weather logic.
     */
    public static float getAdjustedTemperature(Level level, BlockPos pos) {
        Holder<Biome> biomeHolder = level.getBiome(pos);
        Biome biome = biomeHolder.value();

        // FIX: In 1.21.1, the public way to get the temp at a position is:
        // biome.getTemperature(pos) is private.
        // We use the base temperature and manually adjust for height (like Vanilla does)
        // or use the specialized public accessor:
        float baseTemp = biome.getBaseTemperature();

        // Vanilla height logic: Temperature drops by 0.00125 for every block above sea level
        float heightOffset = (float)(pos.getY() - level.getSeaLevel()) * 0.00125F;
        float currentTemp = baseTemp - heightOffset;

        // Don't adjust "Hot" biomes (Deserts, Savannas stay hot)
        if (isHotBiome(biomeHolder)) return currentTemp;

        Season season = getSeason(level);
        float seasonOffset = switch (season) {
            case WINTER -> -0.7f;
            case AUTUMN -> -0.2f;
            case SPRING -> -0.1f;
            case SUMMER -> 0.2f;
        };

        return Mth.clamp(currentTemp + seasonOffset, -0.5f, 2.0f);
    }

    public static boolean isHotBiome(Holder<Biome> biome) {
        return biome.is(BiomeTags.HAS_VILLAGE_DESERT) ||
                biome.is(BiomeTags.IS_SAVANNA) ||
                biome.is(BiomeTags.HAS_VILLAGE_SAVANNA);
    }

    private static String getCurrentWeatherString(Level level) {
        if (!level.isRaining() && !level.isThundering()) return "clear";

        Season season = getSeason(level);
        if (season == Season.WINTER) {
            return level.isThundering() ? "blizzard" : "snow";
        }

        return "rain";
    }

    public static String getSeasonPhase(Level level) {
        int day = (level instanceof ServerLevel sl) ? SeasonData.get(sl).getDisplayDay() : 1;
        if (day <= 6) return "Early";
        if (day <= 14) return "Mid";
        return "Late";
    }

    // --- Command / Event Helpers ---

    public static void setSeason(ServerLevel level, Season season) {
        SeasonData data = SeasonData.get(level);
        data.setCurrentSeason(season);
        data.setDirty();
        triggerSeasonTransition(level, null, season);
    }

    public static void setDay(ServerLevel level, int day) {
        SeasonData data = SeasonData.get(level);
        data.setSeasonDay(day);
        data.setDirty();
        syncToAll(level, data);
    }

    public static void triggerSeasonTransition(ServerLevel level, Season from, Season to) {
        syncToAll(level, SeasonData.get(level));
        for (ServerPlayer player : level.players()) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BEACON_ACTIVATE, SoundSource.AMBIENT, 1.0f, 1.0f);
        }
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncToPlayer(player, player.serverLevel(), SeasonData.get(player.serverLevel()));
        }
    }

    private static void syncToAll(ServerLevel level, SeasonData data) {
        PacketDistributor.sendToAllPlayers(new SeasonSyncPacket(
                data.getCurrentSeason(), data.getDisplayDay(), getCurrentWeatherString(level)
        ));
    }

    public static void syncToPlayer(ServerPlayer player, ServerLevel level, SeasonData data) {
        PacketDistributor.sendToPlayer(player, new SeasonSyncPacket(
                data.getCurrentSeason(), data.getDisplayDay(), getCurrentWeatherString(level)
        ));
    }

    public static Season getSeason(Level level) {
        return (level instanceof ServerLevel sl) ? SeasonData.get(sl).getCurrentSeason() : ClientSeasonHandler.getSeason();
    }
}