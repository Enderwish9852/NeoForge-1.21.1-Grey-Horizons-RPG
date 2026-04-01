package net.enderwish.HUD_Visuals_Subpack.client;

import net.enderwish.HUD_Visuals_Subpack.core.Season;
import net.enderwish.HUD_Visuals_Subpack.event.SeasonManager;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;

import java.util.List;

@EventBusSubscriber(modid = "gh_hud_visuals", value = Dist.CLIENT)
public class ClientDebugHandler {

    @SubscribeEvent
    public static void onDebugInfo(CustomizeGuiOverlayEvent.DebugText event) {
        Minecraft mc = Minecraft.getInstance();

        // 1. Safety check for Level and Debug Screen visibility
        if (mc.level == null || mc.player == null || !mc.getDebugOverlay().showDebugScreen()) {
            return;
        }

        // 2. Fetch Data from our handlers
        Season season = ClientSeasonHandler.getSeason();
        int day = ClientSeasonHandler.getDay();
        String weather = ClientSeasonHandler.getWeather();

        // 3. Get the Adjusted Temperature for the player's current spot
        float adjTemp = SeasonManager.getAdjustedTemperature(mc.level, mc.player.blockPosition());

        // 4. Determine Season Phase
        String phaseStr = getSeasonPhaseName(day);

        // 5. Add info to the Left Side of F3 Menu
        List<String> leftList = event.getLeft();
        leftList.add(""); // Spacer
        leftList.add("§6[GH Seasons]§r " + phaseStr + " " + season.name() + " (Day " + day + "/20)");
        leftList.add("§6[GH Weather]§r " + weather.toUpperCase());

        // This line is crucial for testing your "Snow in Winter" logic
        String tempColor = adjTemp < 0.15f ? "§b" : "§e"; // Cyan for freezing, Yellow for warm
        leftList.add("§6[GH Temp]§r " + tempColor + String.format("%.2f", adjTemp) + "°C");
    }

    /**
     * Helper to define the phase of the season.
     */
    private static String getSeasonPhaseName(int day) {
        if (day <= 6) return "Early";
        if (day <= 14) return "Mid";
        return "Late";
    }
}