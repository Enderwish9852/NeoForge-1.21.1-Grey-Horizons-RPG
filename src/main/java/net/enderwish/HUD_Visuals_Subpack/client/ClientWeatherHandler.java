package net.enderwish.HUD_Visuals_Subpack.client;

import net.enderwish.HUD_Visuals_Subpack.core.WeatherData;
import net.enderwish.HUD_Visuals_Subpack.core.WeatherType;

/**
 * Client-side Weather Controller.
 * Manages visual smoothing for HUD rings and environmental effects.
 */
public class ClientWeatherHandler {

    private static WeatherData data = new WeatherData(WeatherType.CLEAR, 0, 0.0f, 0.0f);

    // Smoothed values for rendering to prevent "snapping" visuals
    private static float smoothedIntensity = 0.0f;
    private static float smoothedTempOffset = 0.0f;

    public static void setWeatherFromServer(WeatherType type, int ticks, float intensity, float tempOffset) {
        data = new WeatherData(type, ticks, intensity, tempOffset);
    }

    /**
     * Called every client tick to smooth out transitions.
     */
    public static void tick() {
        // 1. Smooth Intensity (Fades in/out over ~2 seconds)
        float targetIntensity = data.intensity();
        if (Math.abs(smoothedIntensity - targetIntensity) > 0.001f) {
            smoothedIntensity += (targetIntensity > smoothedIntensity) ? 0.01f : -0.01f;
        }

        // 2. Smooth Temp Offset (Used for HUD color bleeding)
        float targetTemp = data.tempOffset();
        if (Math.abs(smoothedTempOffset - targetTemp) > 0.01f) {
            smoothedTempOffset += (targetTemp > smoothedTempOffset) ? 0.05f : -0.05f;
        }

        // 3. Local tick countdown (Optional but helpful for UI bars)
        if (data.ticksRemaining() > 0) {
            // Since records are immutable, we just decrement the local reference if needed,
            // but usually, we just wait for the next server sync.
        }
    }

    // --- Getters for Rendering ---

    public static WeatherType getType() { return data.type(); }

    public static float getIntensity() { return smoothedIntensity; }

    public static float getTempOffset() { return smoothedTempOffset; }

    /**
     * Returns true if the sky should currently be showing precipitation.
     */
    public static boolean isRaining() {
        return switch (data.type()) {
            case RAIN, THUNDER, SNOW, BLIZZARD, HAIL -> true;
            default -> false;
        };
    }

    /**
     * Special check for the "Freezing" Grade HUD overlay.
     */
    public static boolean isBlizzard() {
        return data.type() == WeatherType.BLIZZARD && smoothedIntensity > 0.5f;
    }

    /**
     * Returns a string for the Sports Watch display.
     */
    public static String getWeatherLabel() {
        return data.type().name().replace("_", " ");
    }
}