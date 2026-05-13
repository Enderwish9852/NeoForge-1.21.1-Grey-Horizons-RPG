package net.enderwish.Atmospheric_Overhaul_Subpack.core.season;

import net.enderwish.Atmospheric_Overhaul_Subpack.core.weather.WeatherDefinition;
import net.enderwish.Atmospheric_Overhaul_Subpack.core.weather.WeatherRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * SeasonTemperature
 *
 * Calculates a dynamic "feels like" temperature for any position.
 *
 * Formula:
 *   finalTemp = biomeBaseTemp + seasonOffset + phaseOffset + weatherOffset
 *
 * This value drives:
 *   - Snow/ice formation speed and depth (SeasonEnvironmentHandler)
 *   - Snow/ice melting speed (SeasonEnvironmentHandler)
 *   - Player body temperature (future system)
 *   - Crop growth modifiers (Farming subpack)
 *
 * Temperature scale:
 *   > 1.5   = extreme heat  (desert summer heatwave)
 *   1.0-1.5 = hot           (savanna, summer)
 *   0.5-1.0 = warm          (plains summer)
 *   0.15-0.5= temperate     (plains spring/autumn)
 *  -0.2-0.15= cold          (winter plains)
 *  -0.5--0.2= very cold     (winter taiga)
 *   < -0.5  = freezing      (blizzard, frozen peaks winter)
 */
public class SeasonTemperature {

    // ── Season offsets ────────────────────────────────────────────────────────
    // How much each season shifts the base biome temperature

    private static final float SPRING_OFFSET = -0.1f; // slightly cool
    private static final float SUMMER_OFFSET =  0.4f; // warm
    private static final float AUTUMN_OFFSET = -0.2f; // cooling
    private static final float WINTER_OFFSET = -0.6f; // cold

    // ── Phase offsets ─────────────────────────────────────────────────────────
    // Applied on top of season offset — phases shift within the season

    private static final float EARLY_OFFSET = -0.05f; // just entering the season
    private static final float MID_OFFSET   =  0.05f; // peak of the season
    private static final float LATE_OFFSET  =  0.0f;  // transitioning out

    // ── Weather offsets ───────────────────────────────────────────────────────
    // Scaled by weather intensity (0.0 - 1.0)

    private static final float RAIN_TEMP_EFFECT       = -0.1f; // rain cools slightly
    private static final float THUNDERSTORM_EFFECT    = -0.15f; // storms cool more
    private static final float BLIZZARD_EFFECT        = -0.4f;  // blizzard = very cold
    private static final float HEATWAVE_EFFECT        =  0.5f;  // heatwave = very hot
    private static final float FOG_EFFECT             = -0.05f; // fog slightly cool
    private static final float CLEAR_EFFECT           =  0.05f; // clear slightly warm

    // ── Thresholds ────────────────────────────────────────────────────────────
    // These gate values control snow/ice behaviour in SeasonEnvironmentHandler

    /** Above this — no snow or ice forms, existing snow melts */
    public static final float MELT_THRESHOLD     =  0.5f;

    /** Above this — no new snow/ice, but existing stays */
    public static final float NO_FORM_THRESHOLD  =  0.15f;

    /** Below this — fast snow/ice formation */
    public static final float FAST_FORM_THRESHOLD = -0.2f;

    /** Below this — extremely fast, maximum depth */
    public static final float EXTREME_THRESHOLD   = -0.5f;

    // ── Main calculation ──────────────────────────────────────────────────────

    /**
     * Calculates the final dynamic temperature at a position.
     *
     * SERVER SIDE — use this in SeasonEnvironmentHandler and future
     * player temperature system.
     */
    public static float calculate(ServerLevel level, BlockPos pos,
                                  SeasonCalendar.Season season,
                                  SeasonCalendar.Phase phase,
                                  String activeWeatherId,
                                  float weatherIntensity) {

        // 1. Base biome temperature (vanilla, read-only)
        float base = level.getBiome(pos).value().getBaseTemperature();

        // 2. Season offset
        float seasonOff = getSeasonOffset(season);

        // 3. Phase offset
        float phaseOff = getPhaseOffset(phase);

        // 4. Weather offset (scaled by intensity)
        float weatherOff = getWeatherOffset(activeWeatherId, weatherIntensity);

        float final_ = base + seasonOff + phaseOff + weatherOff;

        return final_;
    }

    /**
     * Convenience method — reads season/weather from SeasonData directly.
     */
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
     * CLIENT SIDE — reads from ClientSeasonState.
     * Use this for HUD display and visual effects.
     */
    public static float calculateClient(
            float biomeBaseTemp,
            SeasonCalendar.Season season,
            SeasonCalendar.Phase phase,
            String activeWeatherId,
            float weatherIntensity) {

        return biomeBaseTemp
                + getSeasonOffset(season)
                + getPhaseOffset(phase)
                + getWeatherOffset(activeWeatherId, weatherIntensity);
    }

    // ── Snow/ice behaviour helpers ────────────────────────────────────────────

    /**
     * Returns true if snow/ice should actively melt at this temperature.
     */
    public static boolean shouldMelt(float temp) {
        return temp > MELT_THRESHOLD;
    }

    /**
     * Returns true if new snow/ice should form at this temperature.
     */
    public static boolean shouldForm(float temp) {
        return temp <= NO_FORM_THRESHOLD;
    }

    /**
     * Returns a formation speed multiplier based on temperature.
     * 1.0 = normal, 2.0 = twice as fast, 0.5 = half speed.
     *
     * Used by SeasonEnvironmentHandler to scale tick intervals.
     */
    public static float getFormationSpeed(float temp) {
        if (temp <= EXTREME_THRESHOLD)   return 3.0f; // extreme cold = 3x speed
        if (temp <= FAST_FORM_THRESHOLD) return 2.0f; // very cold = 2x speed
        if (temp <= NO_FORM_THRESHOLD)   return 1.0f; // normal cold = 1x speed
        return 0.0f; // too warm — no formation
    }

    /**
     * Returns a melt speed multiplier based on temperature.
     * Higher = faster melting.
     */
    public static float getMeltSpeed(float temp) {
        if (temp > MELT_THRESHOLD + 0.5f) return 3.0f; // very hot = fast melt
        if (temp > MELT_THRESHOLD + 0.2f) return 2.0f; // hot = medium melt
        if (temp > MELT_THRESHOLD)        return 1.0f; // just above threshold = slow melt
        return 0.0f; // not melting
    }

    /**
     * Returns the max snow layer depth allowed at this temperature.
     * Colder = deeper snow allowed.
     */
    public static int getMaxSnowDepth(float temp) {
        if (temp <= EXTREME_THRESHOLD)   return 8; // max depth
        if (temp <= FAST_FORM_THRESHOLD) return 6;
        if (temp <= NO_FORM_THRESHOLD)   return 3;
        return 0; // no snow
    }

    /**
     * Returns the ice spread radius allowed at this temperature.
     */
    public static int getIceSpread(float temp) {
        if (temp <= EXTREME_THRESHOLD)   return 8;
        if (temp <= FAST_FORM_THRESHOLD) return 5;
        if (temp <= NO_FORM_THRESHOLD)   return 2;
        return 0; // no ice
    }

    // ── Display helpers ───────────────────────────────────────────────────────

    /**
     * Returns a human-readable temperature label for HUD/debug.
     * e.g. "Freezing", "Cold", "Warm", "Hot"
     */
    public static String getLabel(float temp) {
        if (temp <= EXTREME_THRESHOLD)   return "Freezing";
        if (temp <= FAST_FORM_THRESHOLD) return "Very Cold";
        if (temp <= NO_FORM_THRESHOLD)   return "Cold";
        if (temp <= MELT_THRESHOLD)      return "Cool";
        if (temp <= 1.0f)                return "Warm";
        if (temp <= 1.5f)                return "Hot";
        return "Scorching";
    }

    /**
     * Converts the internal float to a rough Celsius-like value for display.
     * Not scientifically accurate — just for player readability.
     * Range: roughly -30°C (extreme freeze) to +50°C (scorching)
     */
    public static int toCelsius(float temp) {
        // Map -1.0 → -30°C, 0.0 → 10°C, 1.0 → 30°C, 2.0 → 50°C
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
}