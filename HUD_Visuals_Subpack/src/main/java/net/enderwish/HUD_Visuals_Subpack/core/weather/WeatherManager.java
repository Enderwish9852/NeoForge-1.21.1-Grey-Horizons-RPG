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

    private int weatherTicksLeft = 0;

    public static WeatherManager getInstance() {
        return INSTANCE;
    }

    public void onWorldLoad(ServerLevel level) {
        if (level.dimension() != ServerLevel.OVERWORLD) return;

        ClimateData data = level.getData(ModAttachments.CLIMATE);

        if (data.day() == 1 && data.season() == Season.SPRING && data.tempOffset() == 0.0f) {
            setData(level, new ClimateData(
                    Season.SPRING, 1, "clear", 0.5f, -0.4f
            ));
        }

        if (weatherTicksLeft <= 0) {
            weatherTicksLeft = 6000;
        }

        syncToAll(level);
    }

    public void tick(ServerLevel level) {
        if (level.dimension() != ServerLevel.OVERWORLD) return;

        ClimateData data = level.getData(ModAttachments.CLIMATE);
        long gameTime = level.getGameTime();

        if (gameTime % 24000 == 0) {
            advanceDay(level, data);
        }

        if (weatherTicksLeft <= 0) {
            rollNewWeather(level, data);
        } else {
            weatherTicksLeft--;
        }

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
            durationTicks = 6000 + RANDOM.nextInt(6001);
        } else if (roll <= 94) {
            rarity = WeatherType.WeatherRarity.UNCOMMON;
            durationTicks = 12000 + RANDOM.nextInt(6001);
        } else {
            rarity = WeatherType.WeatherRarity.RARE;
            durationTicks = 24000;
            intensity = 1.0f;
        }

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

    public void setWeather(ServerLevel level, WeatherType type, float intensity) {
        ClimateData current = level.getData(ModAttachments.CLIMATE);

        setData(level, new ClimateData(
                current.season(), current.day(), type.id().toString(), intensity, current.tempOffset()
        ));

        boolean isStormy = WeatherRegistry.is(type, WeatherRegistry.IS_STORM);
        boolean isThunder = WeatherRegistry.is(type, WeatherRegistry.IS_THUNDER);

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

    private void setData(ServerLevel level, ClimateData data) {
        level.setData(ModAttachments.CLIMATE, data);
    }

    public void syncToAll(ServerLevel level) {
        ClimateData data = level.getData(ModAttachments.CLIMATE);
        if (data != null) {
            PacketDistributor.sendToAllPlayers(new ClimateSyncPacket(data));
        }
    }
    /**
     * Grabs the current data stored in the level's attachments.
     * Required by ClimateEventHandler to sync players on join.
     */
    public ClimateData getCurrentData(ServerLevel level) {
        return level.getData(ModAttachments.CLIMATE);
    }
}