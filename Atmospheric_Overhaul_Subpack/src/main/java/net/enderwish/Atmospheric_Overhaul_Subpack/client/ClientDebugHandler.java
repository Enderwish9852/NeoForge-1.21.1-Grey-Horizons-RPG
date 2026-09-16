package net.enderwish.Atmospheric_Overhaul_Subpack.client;

import net.enderwish.Atmospheric_Overhaul_Subpack.AtmosphericOverhaulSubpack;
import net.enderwish.Atmospheric_Overhaul_Subpack.client.particle.RainDropParticle;
import net.enderwish.Atmospheric_Overhaul_Subpack.core.season.SeasonTemperature;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;

import java.util.List;

@EventBusSubscriber(modid = AtmosphericOverhaulSubpack.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ClientDebugHandler {

    @SubscribeEvent
    public static void onDebugInfo(CustomizeGuiOverlayEvent.DebugText event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        List<String> left = event.getLeft();
        left.add("");

        left.add("§6[GH Seasons]§r "
                + ClientSeasonState.getDisplayLabel()
                + " §7(Day " + ClientSeasonState.getYearDay() + "/79)");

        left.add("§6[GH Calendar]§r Season day: "
                + (ClientSeasonState.getYearDay() % 20)
                + "/19 | Year day: "
                + ClientSeasonState.getYearDay()
                + " | Total: "
                + ClientSeasonState.getTotalDays());

        left.add("§6[GH Weather]§r "
                + ClientSeasonState.getWeatherId()
                + " | Intensity: "
                + String.format("%.2f", ClientSeasonState.getIntensity())
                + (ClientSeasonState.isSpecialWeather() ? " §d[SPECIAL]§r" : "")
                + (ClientSeasonState.isPrecipitating() ? " §b[PRECIP]§r" : ""));

        // ── Global wind ────────────────────────────────────────────────────────
        float windSpeed = ClientSeasonState.getWindSpeed();
        float windDx = ClientSeasonState.getWindDx();
        float windDz = ClientSeasonState.getWindDz();
        float horizontalMag = (float) Math.sqrt(
                (windDx * RainDropParticle.WIND_INFLUENCE) * (windDx * RainDropParticle.WIND_INFLUENCE)
                        + (windDz * RainDropParticle.WIND_INFLUENCE) * (windDz * RainDropParticle.WIND_INFLUENCE));

        float intensity = ClientSeasonState.getIntensity();
        float currentFallSpeed = Mth.lerp(Mth.clamp(intensity, 0f, 1f),
                RainDropParticle.BASE_FALL_SPEED, RainDropParticle.MAX_FALL_SPEED);
        float particleAngleDeg = (float) Math.toDegrees(
                Math.atan2(horizontalMag, currentFallSpeed));

        left.add("§6[GH Wind]§r "
                + ClientSeasonState.getWindDirection()
                + " | Speed: " + String.format("%.2f", windSpeed)
                + (ClientSeasonState.isGale() ? " §d[GALE]§r"
                : ClientSeasonState.isWindy() ? " §e[WINDY]§r" : "")
                + " | Particle angle: " + String.format("%.1f°", particleAngleDeg));

        // ── Felt (local, obstruction-adjusted) wind ──────────────────────────
        left.add("§6[GH Felt Wind]§r "
                + ClientSeasonState.getFeltWindDirection()
                + " | Speed: " + String.format("%.2f", ClientSeasonState.getFeltWindSpeed()));

        // ── Regional temperature ─────────────────────────────────────────────
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

        // ── Feels-like (local, chunk-scan-adjusted) temperature ──────────────
        float feelsLike = ClientSeasonState.getFeelsLikeTemp();
        left.add("§6[GH Feels Like]§r "
                + SeasonTemperature.getLabel(feelsLike)
                + " §7(" + SeasonTemperature.toCelsius(feelsLike) + "°C)");

        left.add("§6[GH Biome]§r "
                + ClientWeatherHandler.getCurrentCategory()
                + (ClientWeatherHandler.isPrecipitationVisible() ? " §b[PRECIP]§r" : ""));
    }
}