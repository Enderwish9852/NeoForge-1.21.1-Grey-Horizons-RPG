package net.enderwish.HUD_Visuals_Subpack.core;

import net.enderwish.HUD_Visuals_Subpack.network.ModMessages;
import net.enderwish.HUD_Visuals_Subpack.network.WeatherSyncPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Redefined Weather Manager: Handles Dynamic Intensity and Redefined Types.
 */
public class WeatherManager {
    private static final Random RANDOM = new Random();
    private static final WeatherManager INSTANCE = new WeatherManager();

    private WeatherType currentType = WeatherType.CLEAR;
    private int ticksRemaining = 0;
    private float currentIntensity = 0.0f;
    private float currentTempOffset = 0.0f;

    private WeatherManager() {}

    public static WeatherManager getInstance() { return INSTANCE; }

    public WeatherData getCurrentWeatherData() {
        return new WeatherData(this.currentType, this.ticksRemaining, this.currentIntensity, this.currentTempOffset);
    }

    /**
     * Main logic loop called by Server tick.
     */
    public void tick(ServerLevel level, Season currentSeason) {
        if (ticksRemaining > 0) {
            ticksRemaining--;

            // Calculate dynamic temperature offset based on Type and current Intensity
            // (MaxModifier * Intensity) = current effect
            float target = currentType.getMaxTempModifier() * currentIntensity;

            // Smoothly lerp the offset (approx 0.01 per tick)
            currentTempOffset = Mth.lerp(0.05f, currentTempOffset, target);
        } else {
            selectNewWeather(level, currentSeason);
        }
    }

    private void selectNewWeather(ServerLevel level, Season season) {
        List<WeatherType> pool = new ArrayList<>();
        for (WeatherType type : WeatherType.values()) {
            int weight = type.getWeight(season);
            for (int i = 0; i < weight; i++) pool.add(type);
        }

        WeatherType nextType = pool.isEmpty() ? WeatherType.CLEAR : pool.get(RANDOM.nextInt(pool.size()));

        // Logic: Specials last 24k ticks at max intensity. Others are random.
        int duration;
        float intensity;

        if (nextType.isSpecial()) {
            duration = 24000;
            intensity = 1.0f;
        } else if (nextType == WeatherType.CLEAR) {
            duration = 12000 + RANDOM.nextInt(12000);
            intensity = 0.0f;
        } else {
            duration = 6000 + RANDOM.nextInt(12000);
            intensity = 0.1f + RANDOM.nextFloat() * 0.9f; // Randomly light or heavy
        }

        setWeather(level, nextType, duration, intensity);
    }

    public void setWeather(ServerLevel level, WeatherType type, int duration, float intensity) {
        this.currentType = type;
        this.ticksRemaining = duration;
        this.currentIntensity = intensity;

        // Sync vanilla sky states
        updateVanillaWeather(level, type, duration);

        // Broadcast to all players
        ModMessages.sendToAllPlayers(new WeatherSyncPacket(getCurrentWeatherData()));

        // Log to console (useful for Alpha testing)
        level.getServer().getPlayerList().broadcastSystemMessage(
                Component.literal("§6[Weather]§r " + type.name() + " (Int: " + String.format("%.2f", intensity) + ") for " + (duration/20) + "s"), false
        );
    }

    private void updateVanillaWeather(ServerLevel level, WeatherType type, int duration) {
        // Map our custom types back to vanilla sky/particle logic
        boolean isRainy = switch(type) {
            case RAIN, THUNDER, SNOW, BLIZZARD, HAIL -> true;
            default -> false;
        };
        boolean isThundering = (type == WeatherType.THUNDER || type == WeatherType.BLIZZARD);

        // Vanilla intensity (sky darkening) matches our custom intensity
        level.setWeatherParameters(0, duration, isRainy, isThundering);

        if (isRainy) {
            level.setRainLevel(currentIntensity);
            level.setThunderLevel(isThundering ? currentIntensity : 0.0f);
        }
    }

    public float getCurrentTempOffset() { return currentTempOffset; }
    public float getCurrentIntensity() { return currentIntensity; }
    public WeatherType getCurrentType() { return currentType; }
}