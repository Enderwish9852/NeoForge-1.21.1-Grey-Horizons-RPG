package net.enderwish.HUD_Visuals_Subpack.core;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import java.util.Random;

/**
 * Manages weather transitions and sends vanilla-style server messages.
 */
public class WeatherManager {
    private static final Random RANDOM = new Random();
    private static final WeatherManager INSTANCE = new WeatherManager();

    private WeatherType currentType = WeatherType.CLEAR;
    private int ticksRemaining = 0;
    private float currentIntensity = 0.0f;

    private WeatherManager() {}

    public static WeatherManager getInstance() {
        return INSTANCE;
    }

    public void tick(Level level) {
        if (level.isClientSide) return;

        if (ticksRemaining > 0) {
            ticksRemaining--;
        } else if (level instanceof ServerLevel serverLevel) {
            changeWeather(serverLevel);
        }
    }

    private void changeWeather(ServerLevel level) {
        WeatherType[] types = WeatherType.values();
        WeatherType nextType = types[RANDOM.nextInt(types.length)];
        // Standard duration: 6000 to 18000 ticks
        int duration = 6000 + RANDOM.nextInt(12000);
        setWeather(level, nextType, duration, nextType.getIntensity());
    }

    public void setWeather(ServerLevel level, WeatherType type, int duration, float intensity) {
        this.currentType = type;
        this.ticksRemaining = duration;
        this.currentIntensity = intensity;

        // Apply physical changes to the world
        updateVanillaWeather(level, type, duration);

        // Send the "Set the weather to..." message to all players
        sendWeatherChatMessage(level, type.name().toLowerCase());
    }

    private void sendWeatherChatMessage(ServerLevel level, String weatherName) {
        // Use Component.translatable if you want it localized,
        // or literal for your custom "blizzard" type.
        Component message = Component.literal("Set the weather to " + weatherName);

        // Broadcasts to the system chat (gray text by default in many themes)
        level.getServer().getPlayerList().broadcastSystemMessage(message, false);
    }

    private void updateVanillaWeather(ServerLevel level, WeatherType type, int duration) {
        // IMPORTANT: Use WeatherType.<VALUE> to avoid the "Field to Import" error
        switch (type) {
            case CLEAR:
            case HEATWAVE:
                level.setWeatherParameters(duration, 0, false, false);
                break;
            case LIGHT_RAIN:
            case HEAVY_RAIN:
            case FOG:
            case BLIZZARD:
                // This makes the sky dark and starts precipitation
                level.setWeatherParameters(0, duration, true, false);
                break;
            case THUNDERSTORM:
                level.setWeatherParameters(0, duration, true, true);
                break;
        }
    }

    public WeatherData getCurrentWeatherData() {
        return new WeatherData(currentType, ticksRemaining, currentIntensity);
    }

    public boolean isExtremeWeather() {
        return currentType == WeatherType.BLIZZARD ||
                currentType == WeatherType.THUNDERSTORM ||
                currentType == WeatherType.HEATWAVE;
    }
}