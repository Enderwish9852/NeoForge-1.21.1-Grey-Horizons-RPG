package net.enderwish.HUD_Visuals_Subpack.client;

import net.enderwish.HUD_Visuals_Subpack.core.WeatherData;
import net.enderwish.HUD_Visuals_Subpack.core.WeatherType;
import net.enderwish.HUD_Visuals_Subpack.network.WeatherSyncPacket;

/**
 * Handles weather data on the client side.
 * Manages local state and provides smoothed data for rendering (fog/HUD).
 */
public class ClientWeatherHandler {
    // Defaulting to CLEAR with 0 intensity
    private static WeatherData currentWeatherData = new WeatherData(WeatherType.CLEAR, 0, 0.0f);

    // Used for smooth transitions in rendering (0.0 to 1.0)
    private static float transitionAlpha = 0.0f;

    /**
     * Updates the client-side weather data when a packet is received.
     * This matches the call made in WeatherSyncPacket.handle.
     */
    public static void handleWeatherSync(WeatherSyncPacket packet) {
        currentWeatherData = packet.data();
    }

    /**
     * Alternative setter if you prefer passing individual values.
     */
    public static void setWeatherFromServer(WeatherType type, float intensity) {
        // We create a temporary duration or use a default if the packet didn't specify
        currentWeatherData = new WeatherData(type, (int) 200F, (int) intensity);
    }

    public static WeatherType getCurrentType() {
        return currentWeatherData.type();
    }

    public static int getTicksRemaining() {
        return currentWeatherData.ticksRemaining();
    }

    public static float getIntensity() {
        return currentWeatherData.intensity();
    }

    /**
     * Logic to decrement ticks on the client to keep the UI smooth.
     * Should be called from ClientTickEvent.
     */
    public static void tick() {
        if (currentWeatherData.ticksRemaining() > 0) {
            currentWeatherData = new WeatherData(
                    currentWeatherData.type(),
                    currentWeatherData.ticksRemaining() - 1,
                    currentWeatherData.intensity()
            );
        } else if (currentWeatherData.type() != WeatherType.CLEAR) {
            // Auto-clear when time runs out
            currentWeatherData = new WeatherData(WeatherType.CLEAR, 0, 0.0f);
        }

        // Smooth transition logic
        // If weather is active, fade in. If CLEAR, fade out.
        if (currentWeatherData.type() != WeatherType.CLEAR) {
            transitionAlpha = Math.min(1.0f, transitionAlpha + 0.02f); // Fades in over 50 ticks
        } else {
            transitionAlpha = Math.max(0.0f, transitionAlpha - 0.02f); // Fades out over 50 ticks
        }
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