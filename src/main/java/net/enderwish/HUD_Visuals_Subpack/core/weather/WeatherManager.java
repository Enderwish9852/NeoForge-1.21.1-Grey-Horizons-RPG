package net.enderwish.HUD_Visuals_Subpack.core.weather;

import net.enderwish.HUD_Visuals_Subpack.api.ClimateData;
import net.enderwish.HUD_Visuals_Subpack.api.ClimateHooks;
import net.enderwish.HUD_Visuals_Subpack.api.WeatherRegistry;
import net.enderwish.HUD_Visuals_Subpack.api.WeatherType;
import net.enderwish.HUD_Visuals_Subpack.core.ModAttachments;
import net.enderwish.HUD_Visuals_Subpack.core.Season;
import net.enderwish.HUD_Visuals_Subpack.network.ClimateSyncPacket;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Random;

public class WeatherManager {
    private static final Random RANDOM = new Random();
    public static final int DAYS_PER_SEASON = 20;

    private static final WeatherManager INSTANCE = new WeatherManager();
    private static int weatherTicksLeft = 0;

    public static WeatherManager getInstance() {
        return INSTANCE;
    }

    public void tick(ServerLevel level) {
        if (level.dimension() != ServerLevel.OVERWORLD) return;

        ClimateData data = level.getData(ModAttachments.CLIMATE);
        long gameTime = level.getGameTime();

        // 1. ADVANCE DAY LOGIC (Every 24000 ticks)
        if (gameTime % 24000 == 0) {
            advanceDay(level, data);
        }

        // 2. DYNAMIC WEATHER TICKING
        if (weatherTicksLeft <= 0) {
            rollNewWeather(level, data);
        } else {
            weatherTicksLeft--;
        }

        // 3. HEARTBEAT SYNC (Every 5 seconds)
        if (gameTime % 100 == 0) {
            syncToAll(level);
        }
    }

    private void rollNewWeather(ServerLevel level, ClimateData data) {
        Season season = data.season();
        int roll = RANDOM.nextInt(100) + 1;

        WeatherType.WeatherRarity rarity;
        int durationTicks;
        float intensity = 0.3f + RANDOM.nextFloat() * 0.7f;

        // --- RARITY & DURATION (From Doc) ---
        if (roll <= 60) {
            rarity = WeatherType.WeatherRarity.COMMON;
            durationTicks = 4800 + RANDOM.nextInt(4801); // 4-8h
        } else if (roll <= 94) {
            rarity = WeatherType.WeatherRarity.UNCOMMON;
            durationTicks = 9600 + RANDOM.nextInt(4801); // 8-12h
        } else {
            rarity = WeatherType.WeatherRarity.RARE;
            durationTicks = 24000; // 24h Lock
            intensity = 1.0f;
        }

        // --- SEASONAL POOLS (Using Registry Hooks) ---
        WeatherType selected = switch (season) {
            case SPRING -> (rarity == WeatherType.WeatherRarity.RARE) ?
                    WeatherRegistry.POLLEN_HAZE : WeatherRegistry.getRandomWeatherForSeason(season, rarity);

            case SUMMER -> (rarity == WeatherType.WeatherRarity.COMMON) ?
                    WeatherRegistry.getRandomFromList("clear", "heatwave", "draught") :
                    WeatherRegistry.getRandomFromList("rain", "wind");

            case AUTUMN -> (rarity == WeatherType.WeatherRarity.UNCOMMON) ?
                    WeatherRegistry.THUNDER : WeatherRegistry.getRandomFromList("rain", "fog", "cloudy", "clear", "wind");

            case WINTER -> (rarity == WeatherType.WeatherRarity.RARE) ?
                    (RANDOM.nextBoolean() ? WeatherRegistry.DIAMOND_DUST : WeatherRegistry.THAW) :
                    WeatherRegistry.getRandomWeatherForSeason(season, rarity);
        };

        if (selected == null) selected = WeatherRegistry.CLEAR;

        weatherTicksLeft = durationTicks;
        setWeather(level, selected, intensity);
    }

    private void advanceDay(ServerLevel level, ClimateData old) {
        int nextDay = old.day() + 1;
        Season nextSeason = old.season();

        if (nextDay > DAYS_PER_SEASON) {
            nextDay = 1;
            nextSeason = old.season().next();
        }

        // Apply new day data
        level.setData(ModAttachments.CLIMATE, new ClimateData(
                nextSeason, nextDay, old.weather(), old.intensity(), old.tempOffset()
        ));
    }

    public void setWeather(ServerLevel level, WeatherType type, float intensity) {
        ClimateData current = level.getData(ModAttachments.CLIMATE);

        // We use the Hook to set the tempOffset based on the WeatherType's modifier
        level.setData(ModAttachments.CLIMATE, new ClimateData(
                current.season(), current.day(), type.id().toString(), intensity, type.tempModifier()
        ));

        syncToAll(level);
    }

    public void syncToAll(ServerLevel level) {
        ClimateData data = level.getData(ModAttachments.CLIMATE);
        PacketDistributor.sendToAllPlayers(new ClimateSyncPacket(data));
    }

    public net.enderwish.HUD_Visuals_Subpack.api.ClimateData getCurrentData(ServerLevel level) {
        // This is the correct NeoForge 1.21.1 way: Hook into the level's attachment
        return level.getData(net.enderwish.HUD_Visuals_Subpack.core.ModAttachments.CLIMATE);
    }
}