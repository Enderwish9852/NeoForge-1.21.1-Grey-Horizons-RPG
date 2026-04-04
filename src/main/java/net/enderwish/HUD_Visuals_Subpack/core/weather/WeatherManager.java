package net.enderwish.HUD_Visuals_Subpack.core.weather;

import net.enderwish.HUD_Visuals_Subpack.api.ClimateData;
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

    // This is now moved to the Attachment or a saved variable to ensure persistence
    private int weatherTicksLeft = 0;

    public static WeatherManager getInstance() {
        return INSTANCE;
    }

    /**
     * Called when the world loads. Restores the weather timer and checks for fresh starts.
     */
    public void onWorldLoad(ServerLevel level) {
        if (level.dimension() != ServerLevel.OVERWORLD) return;

        ClimateData data = level.getData(ModAttachments.CLIMATE);

        // 1. Initial Cold Start Logic (for Day 1 Spring)
        if (data.day() == 1 && data.season() == Season.SPRING && data.tempOffset() == 0.0f) {
            setData(level, new ClimateData(
                    Season.SPRING, 1, "clear", 0.5f, -0.4f
            ));
        }

        // 2. RESTORE PERSISTENCE:
        // Note: In a full implementation, weatherTicksLeft should be in ClimateData
        // so it saves automatically. For now, we set a default if it's 0.
        if (weatherTicksLeft <= 0) {
            weatherTicksLeft = 6000;
        }

        syncToAll(level);
    }

    public void tick(ServerLevel level) {
        if (level.dimension() != ServerLevel.OVERWORLD) return;

        ClimateData data = level.getData(ModAttachments.CLIMATE);
        long gameTime = level.getGameTime();

        // Advance Day every 24000 ticks
        if (gameTime % 24000 == 0) {
            advanceDay(level, data);
        }

        // Handle Weather Transitions
        if (weatherTicksLeft <= 0) {
            rollNewWeather(level, data);
        } else {
            weatherTicksLeft--;
        }

        // Periodic Sync (Every 5 seconds)
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

        if (roll <= 60) {
            rarity = WeatherType.WeatherRarity.COMMON;
            durationTicks = 6000 + RANDOM.nextInt(6001); // 5-10 mins
        } else if (roll <= 94) {
            rarity = WeatherType.WeatherRarity.UNCOMMON;
            durationTicks = 12000 + RANDOM.nextInt(6001); // 10-15 mins
        } else {
            rarity = WeatherType.WeatherRarity.RARE;
            durationTicks = 24000; // Full day
            intensity = 1.0f;
        }

        // Fixed selection logic to ensure custom weathers are picked
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

        this.weatherTicksLeft = durationTicks;
        setWeather(level, selected, intensity);
    }

    private void advanceDay(ServerLevel level, ClimateData old) {
        int nextDay = old.day() + 1;
        Season nextSeason = old.season();

        if (nextDay > DAYS_PER_SEASON) {
            nextDay = 1;
            nextSeason = old.season().next();
        }

        float currentOffset = old.tempOffset();
        if (nextSeason == Season.SPRING && currentOffset < 0) {
            currentOffset += 0.06f;
            if (currentOffset > 0) currentOffset = 0;
        }

        setData(level, new ClimateData(
                nextSeason, nextDay, old.weather(), old.intensity(), currentOffset
        ));
    }

    /**
     * FORCED WEATHER CHANGE: Call this from commands or internally.
     */
    public void setWeather(ServerLevel level, WeatherType type, float intensity) {
        ClimateData current = level.getData(ModAttachments.CLIMATE);

        // 1. Update Mod Data and Save to NBT
        setData(level, new ClimateData(
                current.season(), current.day(), type.id().toString(), intensity, current.tempOffset()
        ));

        // 2. Sync Vanilla Rendering
        boolean isStormy = WeatherRegistry.is(type, WeatherRegistry.IS_STORM);
        boolean isThunder = WeatherRegistry.is(type, WeatherRegistry.IS_THUNDER);

        // This is the core method for vanilla persistence
        level.setWeatherParameters(0, weatherTicksLeft, isStormy, isThunder);

        if (isStormy) {
            level.setRainLevel(intensity);
            level.setThunderLevel(isThunder ? intensity : 0.0f);
        } else {
            level.setRainLevel(0.0f);
            level.setThunderLevel(0.0f);
        }

        syncToAll(level);
    }

    /**
     * Helper to update data and ensure it's saved to the world.
     */
    private void setData(ServerLevel level, ClimateData data) {
        level.setData(ModAttachments.CLIMATE, data);
    }

    public void syncToAll(ServerLevel level) {
        ClimateData data = level.getData(ModAttachments.CLIMATE);
        if (data != null) {
            PacketDistributor.sendToAllPlayers(new ClimateSyncPacket(data));
        }
    }
}