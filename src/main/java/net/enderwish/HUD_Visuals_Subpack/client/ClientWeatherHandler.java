package net.enderwish.HUD_Visuals_Subpack.client;

import net.enderwish.HUD_Visuals_Subpack.core.WeatherData;
import net.enderwish.HUD_Visuals_Subpack.core.WeatherType;
import net.enderwish.HUD_Visuals_Subpack.network.WeatherSyncPacket;

/**
 * Handles weather data on the client side.
 * Manages local state and provides smoothed data for rendering (fog/HUD).
 */
public class ClientWeatherHandler {

    // Updated: Now passing 4 arguments (Type, Ticks, Intensity, TempOffset) to match the Record
    private static WeatherData currentWeatherData = new WeatherData(WeatherType.CLEAR, 0, 0.0f, 0.0f);

    // Used for smooth transitions in rendering (0.0 to 1.0)
    private static float transitionAlpha = 0.0f;

    /**
     * Updates the client-side weather data when a packet is received.
     */
    public static void handleWeatherSync(WeatherSyncPacket packet) {
        currentWeatherData = packet.data();
    }

    /**
     * Sets the weather from the server.
     * Updated to include the new temperature offset and duration fields.
     */
    public static void setWeatherFromServer(WeatherType type, int ticks, float intensity, float tempOffset) {
        currentWeatherData = new WeatherData(type, ticks, intensity, tempOffset);
    }

    /**
     * Legacy/Simplified setter for cases where ticks and temp might be default.
     */
    public static void setWeatherFromServer(WeatherType type, float intensity) {
        currentWeatherData = new WeatherData(type, 0, intensity, 0.0f);
    }

    public static WeatherType getCurrentType() {
        return currentWeatherData.type();
    }

    public static float getIntensity() {
        return currentWeatherData.intensity();
    }

    public static float getTempOffset() {
        return currentWeatherData.tempOffset();
    }

    public static int getTicksRemaining() {
        return currentWeatherData.ticksRemaining();
    }

    /**
     * Logic to handle visual transitions on the client.
     */
    public static void tick() {
        // Smooth transition logic
        // If weather is active (not CLEAR), fade in. If CLEAR, fade out.
        if (currentWeatherData.type() != WeatherType.CLEAR) {
            transitionAlpha = Math.min(1.0f, transitionAlpha + 0.02f); // Fades in over 50 ticks
        } else {
            transitionAlpha = Math.max(0.0f, transitionAlpha - 0.02f); // Fades out over 50 ticks
        }

        // Optional: Local countdown of ticks if you want to predict end-of-weather
        /*
        if (currentWeatherData.ticksRemaining() > 0) {
            // Note: currentWeatherData is a record (immutable),
            // so you'd need to re-instantiate if you want to track ticks locally.
        }
        */
    }

    public static float getVisualAlpha() {
        return transitionAlpha;
    }

    public static float getBlizzardIntensity() {
        return (getCurrentType() == WeatherType.BLIZZARD) ? (transitionAlpha * getIntensity()) : 0.0f;
    }

    public static float getHeatwaveIntensity() {
        return (getCurrentType() == WeatherType.HEATWAVE) ? (transitionAlpha * getIntensity()) : 0.0f;
    }
}