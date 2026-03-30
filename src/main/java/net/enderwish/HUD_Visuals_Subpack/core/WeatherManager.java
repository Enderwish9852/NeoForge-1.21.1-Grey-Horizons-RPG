package net.enderwish.HUD_Visuals_Subpack.core;

import net.enderwish.HUD_Visuals_Subpack.network.ModMessages;
import net.enderwish.HUD_Visuals_Subpack.network.WeatherSyncPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Manages weighted weather selection and temperature offsets.
 * Logic based on "Hud & visuals subpack (alpha test).docx".
 */
public class WeatherManager {
    private static final Random RANDOM = new Random();
    private static final WeatherManager INSTANCE = new WeatherManager();

    private WeatherType currentType = WeatherType.CLEAR;
    private int ticksRemaining = 0;
    private float currentIntensity = 0.0f;
    private float currentTempOffset = 0.0f;
    private float targetTempOffset = 0.0f;

    private WeatherManager() {}

    public static WeatherManager getInstance() { return INSTANCE; }

    public WeatherData getCurrentWeatherData() {
        return new WeatherData(this.currentType, this.ticksRemaining, this.currentIntensity, this.currentTempOffset);
    }

    /**
     * Main logic loop called by WeatherEventHandler.
     */
    public void tick(ServerLevel level, Season currentSeason) {
        if (ticksRemaining > 0) {
            ticksRemaining--;

            // Smoothly move current temp toward the target (0.02 degrees per tick)
            if (Math.abs(currentTempOffset - targetTempOffset) > 0.1f) {
                currentTempOffset += (targetTempOffset > currentTempOffset) ? 0.02f : -0.02f;
            }
        } else {
            // Weather has ended, roll for new weather
            selectNewWeather(level, currentSeason);
        }
    }

    private void selectNewWeather(ServerLevel level, Season season) {
        List<WeatherType> pool = new ArrayList<>();

        // Build weighted pool based on the current season
        for (WeatherType type : WeatherType.values()) {
            int weight = type.getWeight(season);
            for (int i = 0; i < weight; i++) {
                pool.add(type);
            }
        }

        WeatherType nextType = pool.isEmpty() ? WeatherType.CLEAR : pool.get(RANDOM.nextInt(pool.size()));

        // Special weather lasts 1 full day (24000 ticks) as per doc, others 6000-12000
        int duration = isSpecial(nextType) ? 24000 : 6000 + RANDOM.nextInt(6000);
        float intensity = 0.5f + RANDOM.nextFloat() * 0.5f;

        setWeather(level, nextType, duration, intensity);
    }

    public void setWeather(ServerLevel level, WeatherType type, int duration, float intensity) {
        this.currentType = type;
        this.ticksRemaining = duration;
        this.currentIntensity = (type == WeatherType.CLEAR) ? 0.0f : intensity;
        this.targetTempOffset = type.getTempModifier();

        // Sync vanilla sky/rain states
        updateVanillaWeather(level, type, duration);

        // Broadcast to all players
        ModMessages.sendToAllPlayers(new WeatherSyncPacket(getCurrentWeatherData()));

        // Optional: Log to server console
        level.getServer().getPlayerList().broadcastSystemMessage(
                Component.literal("Weather changed to: " + type.name() + " for " + (duration/20) + "s"), false
        );
    }

    private void updateVanillaWeather(ServerLevel level, WeatherType type, int duration) {
        boolean rain = switch(type) {
            case LIGHT_RAIN, HEAVY_RAIN, THUNDER, SNOW, SNOW_STORM, BLIZZARD, HAIL -> true;
            default -> false;
        };
        boolean thunder = (type == WeatherType.THUNDER || type == WeatherType.BLIZZARD);

        level.setWeatherParameters(0, duration, rain, thunder);
    }

    private boolean isSpecial(WeatherType type) {
        return type == WeatherType.POLLEN_HAZE || type == WeatherType.DIAMOND_DUST || type == WeatherType.THAW;
    }
}