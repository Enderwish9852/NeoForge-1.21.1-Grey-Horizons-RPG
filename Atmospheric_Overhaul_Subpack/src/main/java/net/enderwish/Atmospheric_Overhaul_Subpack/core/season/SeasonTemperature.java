package net.enderwish.Atmospheric_Overhaul_Subpack.core.season;

import net.enderwish.Atmospheric_Overhaul_Subpack.core.weather.WeatherDefinition;
import net.enderwish.Atmospheric_Overhaul_Subpack.core.weather.WeatherRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * SeasonTemperature
 *
 * finalTemp = biomeBaseTemp + seasonOffset + phaseOffset + weatherOffset + diurnalOffset
 *
 * NEW — diurnal offset: previously the whole formula never referenced
 * time of day at all, confirmed absent as reported. Added a smooth
 * cosine curve over the vanilla day-tick cycle (0-24000), peaking at
 * tick 6000 (noon, +amplitude) and bottoming at tick 18000 (midnight,
 * -amplitude), zero at dawn/dusk transition points — continuous, no
 * instant jumps. Amplitude (0.15) is a deliberately modest, easily
 * tunable constant relative to the existing season swings (0.1-0.6).
 *
 * calculateClient(...) gained a new `dayTime` parameter to carry this
 * through to the F3 debug display — the client's own ClientLevel
 * already tracks day time via vanilla's normal sync, no new GH network
 * packet needed. ClientDebugHandler.java's call site is updated to
 * match (see below).
 */
public class SeasonTemperature {

    private static final float SPRING_OFFSET = -0.1f;
    private static final float SUMMER_OFFSET =  0.4f;
    private static final float AUTUMN_OFFSET = -0.2f;
    private static final float WINTER_OFFSET = -0.6f;

    private static final float EARLY_OFFSET = -0.05f;
    private static final float MID_OFFSET   =  0.05f;
    private static final float LATE_OFFSET  =  0.0f;

    private static final float RAIN_TEMP_EFFECT       = -0.1f;
    private static final float THUNDERSTORM_EFFECT    = -0.15f;
    private static final float BLIZZARD_EFFECT        = -0.4f;
    private static final float HEATWAVE_EFFECT        =  0.5f;
    private static final float FOG_EFFECT             = -0.05f;
    private static final float CLEAR_EFFECT           =  0.05f;

    /** Peak swing above/below the base curve at noon/midnight. */
    private static final float DIURNAL_AMPLITUDE = 0.15f;

    public static final float MELT_THRESHOLD     =  0.5f;
    public static final float NO_FORM_THRESHOLD  =  0.15f;
    public static final float FAST_FORM_THRESHOLD = -0.2f;
    public static final float EXTREME_THRESHOLD   = -0.5f;

    // ── Main calculation ──────────────────────────────────────────────────────

    public static float calculate(ServerLevel level, BlockPos pos,
                                  SeasonCalendar.Season season,
                                  SeasonCalendar.Phase phase,
                                  String activeWeatherId,
                                  float weatherIntensity) {

        float base = level.getBiome(pos).value().getBaseTemperature();
        float seasonOff = getSeasonOffset(season);
        float phaseOff = getPhaseOffset(phase);
        float weatherOff = getWeatherOffset(activeWeatherId, weatherIntensity);
        float diurnalOff = getDiurnalOffset(level.getDayTime());

        return base + seasonOff + phaseOff + weatherOff + diurnalOff;
    }

    public static float calculate(ServerLevel level, BlockPos pos) {
        SeasonData data = SeasonData.get(level);
        return calculate(
                level, pos,
                data.getSeason(),
                data.getPhase(),
                data.getActiveWeatherId(),
                data.getActiveIntensity()
        );
    }

    /**
     * CLIENT SIDE — reads from ClientSeasonState, plus the client's own
     * ClientLevel day time (pass mc.level.getDayTime() directly — no
     * new sync packet needed, vanilla already tracks this).
     */
    public static float calculateClient(
            float biomeBaseTemp,
            SeasonCalendar.Season season,
            SeasonCalendar.Phase phase,
            String activeWeatherId,
            float weatherIntensity,
            long dayTime) {

        return biomeBaseTemp
                + getSeasonOffset(season)
                + getPhaseOffset(phase)
                + getWeatherOffset(activeWeatherId, weatherIntensity)
                + getDiurnalOffset(dayTime);
    }

    // ── Snow/ice behaviour helpers (unchanged) ────────────────────────────────

    public static boolean shouldMelt(float temp) {
        return temp > MELT_THRESHOLD;
    }

    public static boolean shouldForm(float temp) {
        return temp <= NO_FORM_THRESHOLD;
    }

    public static float getFormationSpeed(float temp) {
        if (temp <= EXTREME_THRESHOLD)   return 3.0f;
        if (temp <= FAST_FORM_THRESHOLD) return 2.0f;
        if (temp <= NO_FORM_THRESHOLD)   return 1.0f;
        return 0.0f;
    }

    public static float getMeltSpeed(float temp) {
        if (temp > MELT_THRESHOLD + 0.5f) return 3.0f;
        if (temp > MELT_THRESHOLD + 0.2f) return 2.0f;
        if (temp > MELT_THRESHOLD)        return 1.0f;
        return 0.0f;
    }

    public static int getMaxSnowDepth(float temp) {
        if (temp <= EXTREME_THRESHOLD)   return 8;
        if (temp <= FAST_FORM_THRESHOLD) return 6;
        if (temp <= NO_FORM_THRESHOLD)   return 3;
        return 0;
    }

    public static int getIceSpread(float temp) {
        if (temp <= EXTREME_THRESHOLD)   return 8;
        if (temp <= FAST_FORM_THRESHOLD) return 5;
        if (temp <= NO_FORM_THRESHOLD)   return 2;
        return 0;
    }

    // ── Display helpers (unchanged) ───────────────────────────────────────────

    public static String getLabel(float temp) {
        if (temp <= EXTREME_THRESHOLD)   return "Freezing";
        if (temp <= FAST_FORM_THRESHOLD) return "Very Cold";
        if (temp <= NO_FORM_THRESHOLD)   return "Cold";
        if (temp <= MELT_THRESHOLD)      return "Cool";
        if (temp <= 1.0f)                return "Warm";
        if (temp <= 1.5f)                return "Hot";
        return "Scorching";
    }

    public static int toCelsius(float temp) {
        return Math.round(temp * 30f + 5f);
    }

    // ── Offsets ───────────────────────────────────────────────────────────────

    private static float getSeasonOffset(SeasonCalendar.Season season) {
        return switch (season) {
            case SPRING -> SPRING_OFFSET;
            case SUMMER -> SUMMER_OFFSET;
            case AUTUMN -> AUTUMN_OFFSET;
            case WINTER -> WINTER_OFFSET;
        };
    }

    private static float getPhaseOffset(SeasonCalendar.Phase phase) {
        return switch (phase) {
            case EARLY -> EARLY_OFFSET;
            case MID   -> MID_OFFSET;
            case LATE  -> LATE_OFFSET;
        };
    }

    private static float getWeatherOffset(String weatherId, float intensity) {
        WeatherDefinition def = WeatherRegistry.INSTANCE.getByName(weatherId);
        float offset = def.tempOffset();
        return def.tempScalesWithIntensity() ? offset * intensity : offset;
    }

    /**
     * Smooth cosine curve over the 0-24000 tick day cycle.
     * Peaks (+amplitude) at tick 6000 (noon), troughs (-amplitude) at
     * tick 18000 (midnight), zero-crossing at dawn (0) and dusk (12000).
     */
    private static float getDiurnalOffset(long dayTime) {
        float dayFraction = (dayTime % 24000L) / 24000f;
        return DIURNAL_AMPLITUDE * (float) Math.cos(2 * Math.PI * (dayFraction - 0.25));
    }
}