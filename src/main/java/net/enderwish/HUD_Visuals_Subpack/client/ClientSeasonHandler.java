package net.enderwish.HUD_Visuals_Subpack.client;

import net.enderwish.HUD_Visuals_Subpack.core.Season;
import net.enderwish.HUD_Visuals_Subpack.network.SeasonSyncPacket;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;

/**
 * Manages seasonal state on the client and forces visual updates.
 */
public class ClientSeasonHandler {

    private static Season clientSeason = Season.SPRING;
    private static int clientDay = 1;
    private static String clientWeather = "clear";

    /**
     * Handles incoming data and triggers a hard refresh if the season or day changes.
     */
    public static void handleData(final SeasonSyncPacket data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Season newSeason = data.season();
            int newDay = data.day();

            // Check if a visual refresh is required (Season change or Day change)
            if (newSeason != clientSeason || newDay != clientDay) {
                clientSeason = newSeason;
                clientDay = newDay;
                clientWeather = data.weather();

                // Perform the hard refresh of all chunks
                hardRefresh();
            } else {
                // Just update weather if no visual rebuild is needed
                setWeather(data.weather());
            }
        });
    }

    /**
     * The "Nuclear Option" for client-side visuals.
     * Clears all color caches and forces every loaded chunk to rebuild its mesh.
     */
    public static void hardRefresh() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LevelRenderer renderer = mc.levelRenderer;

        if (level != null) {
            // 1. Clear Tint Caches: Minecraft caches grass/foliage colors internally.
            // If we don't clear this, the rebuild might pull the old colors again.
            level.clearTintCaches();
        }

        if (renderer != null) {
            // 2. allChanged(): This marks every single loaded chunk as "dirty".
            // It effectively re-runs the geometry building for the entire world,
            // which will trigger your BiomeMixin to fetch the new seasonal colors.
            renderer.allChanged();
        }
    }

    // --- Standard Getters ---

    public static Season getSeason() { return clientSeason; }
    public static Season getClientSeason() { return clientSeason; }
    public static int getDay() { return clientDay; }
    public static int getClientDay() { return clientDay; }
    public static String getWeather() { return clientWeather; }

    // --- Manual Setters (triggering refresh) ---

    public static void setSeason(Season season) {
        if (clientSeason != season) {
            clientSeason = season;
            hardRefresh();
        }
    }

    public static void setDay(int day) {
        clientDay = day;
    }

    public static void setWeather(String weather) {
        if (weather != null) {
            clientWeather = weather.toLowerCase();
        }
    }
}