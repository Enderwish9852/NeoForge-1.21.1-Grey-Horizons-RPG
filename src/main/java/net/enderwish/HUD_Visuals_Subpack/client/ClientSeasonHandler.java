package net.enderwish.HUD_Visuals_Subpack.client;

import net.enderwish.HUD_Visuals_Subpack.core.Season;
import net.enderwish.HUD_Visuals_Subpack.network.SeasonSyncPacket;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Stores and manages the current season and day data on the client side.
 * This data is used by the HUD and other client-side visual elements.
 */
public class ClientSeasonHandler {

    private static Season clientSeason = Season.SPRING;
    private static int clientDay = 1;

    /**
     * Handles the incoming SeasonSyncPacket.
     * Use this if you register the packet using a method reference to this handler.
     */
    public static void handleData(final SeasonSyncPacket data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            setSeason(data.season());
            setDay(data.day());
        });
    }

    /**
     * Updates the current season stored on the client.
     */
    public static void setSeason(Season season) {
        clientSeason = season;
    }

    /**
     * Updates the current day count stored on the client.
     */
    public static void setDay(int day) {
        clientDay = day;
    }

    /**
     * @return The current season as known by the client.
     */
    public static Season getClientSeason() {
        return clientSeason;
    }

    /**
     * @return The current day count as known by the client.
     */
    public static int getClientDay() {
        return clientDay;
    }
}