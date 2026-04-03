package net.enderwish.HUD_Visuals_Subpack.client;

import net.enderwish.HUD_Visuals_Subpack.api.ClimateData;
import net.enderwish.HUD_Visuals_Subpack.api.ClimateHooks;
import net.enderwish.HUD_Visuals_Subpack.core.Season;
import net.minecraft.client.Minecraft;

/**
 * Thread-safe client-side storage for the current world climate.
 * Acts as the data source for the Sports Watch and Visual Overlays.
 */
public class ClientClimateCache {

    // Default state prevents NullPointerExceptions during the first few ticks of a world load
    private static ClimateData instance = new ClimateData(Season.SPRING, 1, "clear", 0.0f, 0.0f);

    /**
     * Updates the cache when a ClimateSyncPacket arrives from the server.
     */
    public static ClimateData getInstance() {
        return instance;
    }
    public static void setInstance(ClimateData data) {
        if (data != null) {
            instance = data;
        }
    }

    /**
     * The Master Getter used by SportsWatchHUD and WeatherRenderers.
     */
    public static ClimateData get() {
        return instance;
    }

    // --- CLEAN LOGIC HELPERS ---

    /**
     * Uses ClimateHooks to determine if the client should render freezing effects.
     * Follows the Alpha Doc: Winter is always cold, others depend on weather[cite: 12].
     */
    public static boolean isFreezing() {
        return ClimateHooks.isColdToFreeze(Minecraft.getInstance().level);
    }


    /**
     * Returns the exact temperature in degrees for the Sports Watch display.
     * Matches the Alpha Test ranges (e.g., Summer 25-38, Winter -15 to 5)[cite: 8, 12].
     */
    public static float getTempForDisplay() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return 0.0f;

        return ClimateHooks.getTemperatureInDegrees(mc.level, mc.player.blockPosition());
    }
}