package net.enderwish.HUD_Visuals_Subpack.client;

import net.enderwish.HUD_Visuals_Subpack.core.Season;
import net.enderwish.HUD_Visuals_Subpack.network.SeasonSyncPacket;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * PURE DATA HANDLER: Manages seasonal state on the client.
 * Visual refreshes and color shifts are handled by ClientColorHandler.
 */
public class ClientSeasonHandler {

    private static Season clientSeason = Season.SPRING;
    private static int clientDay = 1;
    private static String clientWeather = "clear";

    /**
     * Updates the client data from the server packet.
     */
    public static void handleData(final SeasonSyncPacket data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            boolean stateChanged = (data.season() != clientSeason || data.day() != clientDay);

            // Update pure data
            clientSeason = data.season();
            clientDay = data.day();
            clientWeather = data.weather().toLowerCase();

            // If the season or day changed, we notify your Color Handler
            if (stateChanged) {
                // Call your specific color handler here to trigger the refresh
                // ClientColorHandler.refreshVisuals();
            }
        });
    }

    // --- Getters ---

    public static Season getSeason() { return clientSeason; }
    public static int getDay() { return clientDay; }
    public static String getWeather() { return clientWeather; }

    public static boolean isBlizzard() {
        return "blizzard".equals(clientWeather);
    }

    public static boolean isSnowing() {
        return "snow".equals(clientWeather) || "blizzard".equals(clientWeather);
    }

    // --- Manual Setters (Debug only) ---

    public static void setSeason(Season season) {
        clientSeason = season;
    }

    public static void setDay(int day) {
        clientDay = day;
    }

    public static void setWeather(String weather) {
        if (weather != null) clientWeather = weather.toLowerCase();
    }
}