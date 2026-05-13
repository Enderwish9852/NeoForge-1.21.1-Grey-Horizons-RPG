package net.enderwish.Atmospheric_Overhaul_Subpack.client;

import net.enderwish.Atmospheric_Overhaul_Subpack.AtmosphericOverhaulSubpack;
import net.enderwish.Atmospheric_Overhaul_Subpack.core.season.SeasonTemperature;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;

import java.util.List;

/**
 * ClientDebugHandler
 *
 * Adds Grey Horizons season + weather + temperature data to the F3 debug menu.
 * Press F3 in-game to see the overlay on the left side.
 */
@EventBusSubscriber(modid = AtmosphericOverhaulSubpack.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ClientDebugHandler {

    @SubscribeEvent
    public static void onDebugInfo(CustomizeGuiOverlayEvent.DebugText event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        List<String> left = event.getLeft();
        left.add(""); // spacer

        // Season + phase + year
        left.add("§6[GH Seasons]§r "
                + ClientSeasonState.getDisplayLabel()
                + " §7(Day " + ClientSeasonState.getYearDay() + "/79)");

        // Calendar detail
        left.add("§6[GH Calendar]§r Season day: "
                + (ClientSeasonState.getYearDay() % 20)
                + "/19 | Year day: "
                + ClientSeasonState.getYearDay()
                + " | Total: "
                + ClientSeasonState.getTotalDays());

        // Weather
        left.add("§6[GH Weather]§r "
                + ClientSeasonState.getWeatherId()
                + " | Intensity: "
                + String.format("%.2f", ClientSeasonState.getIntensity())
                + (ClientSeasonState.isSpecialWeather() ? " §d[SPECIAL]§r" : "")
                + (ClientSeasonState.isPrecipitating() ? " §b[PRECIP]§r" : ""));

        // Dynamic temperature
        float biomeTemp = mc.level.getBiome(mc.player.blockPosition())
                .value().getBaseTemperature();
        float finalTemp = SeasonTemperature.calculateClient(
                biomeTemp,
                ClientSeasonState.getSeason(),
                ClientSeasonState.getPhase(),
                ClientSeasonState.getWeatherId(),
                ClientSeasonState.getIntensity()
        );
        left.add("§6[GH Temp]§r "
                + SeasonTemperature.getLabel(finalTemp)
                + " §7(" + SeasonTemperature.toCelsius(finalTemp) + "°C)"
                + " | Biome base: " + String.format("%.2f", biomeTemp)
                + " | Final: " + String.format("%.2f", finalTemp));

        // Biome weather category
        left.add("§6[GH Biome]§r "
                + ClientWeatherHandler.getCurrentCategory()
                + (ClientWeatherHandler.isPrecipitationVisible() ? " §b[PRECIP]§r" : ""));
    }
}