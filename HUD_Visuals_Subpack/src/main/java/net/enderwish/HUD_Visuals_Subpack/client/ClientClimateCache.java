package net.enderwish.HUD_Visuals_Subpack.client;

import net.enderwish.HUD_Visuals_Subpack.api.ClimateData;
import net.enderwish.HUD_Visuals_Subpack.api.ClimateHooks;
import net.enderwish.HUD_Visuals_Subpack.api.WeatherRegistry;
import net.enderwish.HUD_Visuals_Subpack.api.WeatherType;
import net.enderwish.HUD_Visuals_Subpack.core.Season;
import net.minecraft.client.Minecraft;

/**
 * Thread-safe client-side storage for the current world climate.
 * Acts as the data source for the Sports Watch and Visual Overlays.
 */
public class ClientClimateCache {

    // Default state: SPRING Day 1, Clear Weather.
    private static ClimateData instance = new ClimateData(Season.SPRING, 1, "clear", 0.5f, 0.0f);

    /**
     * Updates the cache when a ClimateSyncPacket arrives from the server.
     */
    public static void setInstance(ClimateData data) {
        if (data != null) {
            instance = data;
        }
    }

    /**
     * Static access to the current data.
     */
    public static ClimateData getInstance() {
        return instance;
    }

    /**
     * Master Getter used by SportsWatchHUD and WeatherRenderers.
     */
    public static ClimateData get() {
        return instance;
    }

    // --- HELPER LOGIC ---

    /**
     * Returns the full WeatherType object for the currently synced weather.
     * Use this to get Fog Colors, Sky Intensity, and Precipitation toggles.
     */
    public static WeatherType getCurrentWeather() {
        return WeatherRegistry.getById(instance.weather());
    }

    /**
     * Uses ClimateHooks to determine if the client should render freezing effects.
     * Follows the Alpha Doc: Winter is always cold, others depend on weather.
     */
    public static boolean isFreezing() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return instance.season() == Season.WINTER;
        return ClimateHooks.isColdToFreeze(mc.level);
    }

    /**
     * Returns the exact temperature in degrees for the Sports Watch display.
     * Matches the Alpha Test ranges (e.g., Summer 25-38, Winter -15 to 5).
     */
    public static float getTempForDisplay() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            // Fallback to base season temp if world isn't loaded
            return switch (instance.season()) {
                case SPRING -> 15.0f;
                case SUMMER -> 31.5f;
                case AUTUMN -> 12.5f;
                case WINTER -> -5.0f;
            };
        }

        return ClimateHooks.getTemperatureInDegrees(mc.level, mc.player.blockPosition());
    }

    /**
     * Check if the "Initial Thaw" is active (Spring Days 1-7 + Negative Offset).
     * Useful for showing "Misty" screen overlays during the first login.
     */
    public static boolean isThawing() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;
        return ClimateHooks.isThawingPeriod(mc.level);
    }
}