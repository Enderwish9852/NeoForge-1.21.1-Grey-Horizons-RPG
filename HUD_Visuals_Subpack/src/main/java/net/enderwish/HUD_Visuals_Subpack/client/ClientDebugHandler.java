package net.enderwish.HUD_Visuals_Subpack.client;

import net.enderwish.HUD_Visuals_Subpack.HUDVisualsSubpack;
import net.enderwish.HUD_Visuals_Subpack.api.ClimateData;
import net.enderwish.HUD_Visuals_Subpack.api.ClimateHooks;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;

import java.util.List;

/**
 * Adds Gray Horizons climate data to the F3 Debug Menu.
 * Useful for verifying temperature degrees and tag-based logic.
 */
@EventBusSubscriber(modid = HUDVisualsSubpack.MOD_ID, value = Dist.CLIENT)
public class ClientDebugHandler {

    @SubscribeEvent
    public static void onDebugInfo(CustomizeGuiOverlayEvent.DebugText event) {
        Minecraft mc = Minecraft.getInstance();

        // 1. Safety check
        if (mc.level == null || mc.player == null) return;

        // 2. Fetch Data from the Cache
        ClimateData data = ClientClimateCache.get();
        if (data == null) return;

        // 3. Logic Checks (Using the unified Tag-aware Hooks)
        boolean isFreezing = ClimateHooks.isColdToFreeze(mc.level);
        float currentTemp = ClimateHooks.getTemperatureInDegrees(mc.level, mc.player.blockPosition());

        // 4. Day Logic (Days 1-20 per season)
        int dayInSeason = data.day();
        String phaseStr = getSeasonPhaseName(dayInSeason);

        // 5. Add to F3 Menu (Left Side)
        List<String> leftList = event.getLeft();
        leftList.add(""); // Spacer for readability

        // Season & Phase Info
        leftList.add("§6[GH Seasons]§r " + phaseStr + " " + data.season().getSerializedName().toUpperCase() + " (Day " + dayInSeason + "/20)");

        // Temperature & Weather Info (Shows actual degrees from your Alpha Doc)
        leftList.add("§6[GH Climate]§r Temp: " + String.format("%.1f°C", currentTemp) + " | Offset: " + String.format("%.1f", data.tempOffset()));

        // Status Indicators
        String freezeStatus = isFreezing ? "§b[FREEZING]" : "§e[TEMPERATE]";
        leftList.add("§6[GH Weather]§r ID: " + data.weather() + " (Int: " + String.format("%.1f", data.intensity()) + ") " + freezeStatus);
    }

    private static String getSeasonPhaseName(int day) {
        if (day <= 6) return "Early";
        if (day <= 14) return "Mid";
        return "Late";
    }
}