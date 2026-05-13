package net.enderwish.Atmospheric_Overhaul_Subpack.api;

import net.enderwish.Atmospheric_Overhaul_Subpack.client.ClientSeasonState;
import net.enderwish.Atmospheric_Overhaul_Subpack.core.season.SeasonCalendar;
import net.enderwish.Atmospheric_Overhaul_Subpack.core.season.SeasonData;
import net.enderwish.Atmospheric_Overhaul_Subpack.core.season.SeasonTemperature;
import net.enderwish.Atmospheric_Overhaul_Subpack.core.weather.WeatherDefinition;
import net.enderwish.Atmospheric_Overhaul_Subpack.core.weather.WeatherRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * SeasonsAPI
 *
 * The ONLY class other subpacks should import.
 * A static facade over the seasons internals.
 *
 * Other subpacks never import SeasonData, WeatherRegistry,
 * WeatherRoller, SeasonCalendar, or SeasonTemperature directly.
 *
 * Usage example (from Farming subpack):
 *   import net.enderwish.HUD_Visuals_Subpack.api.SeasonsAPI;
 *
 *   SeasonCalendar.Season season = SeasonsAPI.getSeason(level);
 *   if (season == SeasonCalendar.Season.SPRING) { ... }
 *
 *   float temp = SeasonsAPI.getTemperature(level, player.blockPosition());
 *   if (temp < SeasonTemperature.NO_FORM_THRESHOLD) { ... }
 */
public final class SeasonsAPI {

    private SeasonsAPI() {}

    // ── Calendar queries (server-side) ────────────────────────────────────────

    /** Returns the current season. e.g. SPRING, SUMMER, AUTUMN, WINTER */
    public static SeasonCalendar.Season getSeason(ServerLevel level) {
        return SeasonData.get(level).getSeason();
    }

    /** Returns the current phase. e.g. EARLY, MID, LATE */
    public static SeasonCalendar.Phase getPhase(ServerLevel level) {
        return SeasonData.get(level).getPhase();
    }

    /** Returns the current day within the year (0-79). */
    public static int getYearDay(ServerLevel level) {
        return SeasonData.get(level).getYearDay();
    }

    /** Returns the current day within the season (0-19). */
    public static int getSeasonDay(ServerLevel level) {
        return SeasonData.get(level).getSeasonDay();
    }

    /** Returns how many full years have passed. */
    public static int getYear(ServerLevel level) {
        return SeasonData.get(level).getYear();
    }

    /** Returns the total ever-incrementing day count. */
    public static int getTotalDays(ServerLevel level) {
        return SeasonData.get(level).getTotalDays();
    }

    /** Returns a human-readable label. e.g. "Early Spring, Year 2" */
    public static String getDisplayLabel(ServerLevel level) {
        return SeasonData.get(level).getDisplayLabel();
    }

    // ── Weather queries (server-side) ─────────────────────────────────────────

    /** Returns the ID of the currently active weather. e.g. "rain", "blizzard" */
    public static String getActiveWeatherId(ServerLevel level) {
        return SeasonData.get(level).getActiveWeatherId();
    }

    /** Returns the full WeatherDefinition for the currently active weather. */
    public static WeatherDefinition getActiveWeather(ServerLevel level) {
        return WeatherRegistry.INSTANCE.getByName(
                SeasonData.get(level).getActiveWeatherId()
        );
    }

    /** Returns true if it is currently raining or snowing. */
    public static boolean isPrecipitating(ServerLevel level) {
        return getActiveWeather(level).hasRain();
    }

    /** Returns true if there is currently a thunderstorm. */
    public static boolean isThundering(ServerLevel level) {
        return getActiveWeather(level).hasThunder();
    }

    /** Returns true if the active weather is special (blizzard, heatwave). */
    public static boolean isSpecialWeather(ServerLevel level) {
        return getActiveWeather(level).isSpecial();
    }

    /** Returns the current weather intensity (0.0 - 1.0). */
    public static float getWeatherIntensity(ServerLevel level) {
        return SeasonData.get(level).getActiveIntensity();
    }

    /** Returns how many ticks remain for the current weather. */
    public static int getWeatherTicksRemaining(ServerLevel level) {
        return SeasonData.get(level).getWeatherTicksRemaining();
    }

    // ── Temperature queries (server-side) ─────────────────────────────────────

    /**
     * Returns the calculated dynamic temperature at a position.
     * Accounts for biome base temp + season + phase + weather.
     *
     * Useful for Farming subpack crop damage, player warmth system etc.
     */
    public static float getTemperature(ServerLevel level, BlockPos pos) {
        return SeasonTemperature.calculate(level, pos);
    }

    /**
     * Returns a human-readable temperature label at a position.
     * e.g. "Freezing", "Cold", "Warm", "Hot", "Scorching"
     */
    public static String getTemperatureLabel(ServerLevel level, BlockPos pos) {
        return SeasonTemperature.getLabel(SeasonTemperature.calculate(level, pos));
    }

    /**
     * Returns the temperature as a rough Celsius value at a position.
     * e.g. -20, 5, 18, 35
     * Not scientifically accurate — for player display only.
     */
    public static int getTemperatureCelsius(ServerLevel level, BlockPos pos) {
        return SeasonTemperature.toCelsius(SeasonTemperature.calculate(level, pos));
    }

    /**
     * Returns true if it is cold enough for snow/ice to form at a position.
     */
    public static boolean isColdEnoughToFreeze(ServerLevel level, BlockPos pos) {
        return SeasonTemperature.shouldForm(SeasonTemperature.calculate(level, pos));
    }

    /**
     * Returns true if it is warm enough to melt snow/ice at a position.
     */
    public static boolean isWarmEnoughToMelt(ServerLevel level, BlockPos pos) {
        return SeasonTemperature.shouldMelt(SeasonTemperature.calculate(level, pos));
    }

    // ── Convenience checks (server-side) ──────────────────────────────────────

    /** Returns true if the current season matches. */
    public static boolean isSeason(ServerLevel level, SeasonCalendar.Season season) {
        return getSeason(level) == season;
    }

    /** Returns true if the current phase matches. */
    public static boolean isPhase(ServerLevel level, SeasonCalendar.Phase phase) {
        return getPhase(level) == phase;
    }

    /** Returns true if it is winter AND a special weather is active. */
    public static boolean isWinterStorm(ServerLevel level) {
        return isSeason(level, SeasonCalendar.Season.WINTER) && isSpecialWeather(level);
    }

    /** Returns true if a heatwave is currently active. */
    public static boolean isHeatwave(ServerLevel level) {
        return getActiveWeatherId(level).equals("heatwave");
    }

    // ── Client-side queries ───────────────────────────────────────────────────

    /** Returns the current season on the client side. */
    public static SeasonCalendar.Season getClientSeason() {
        return ClientSeasonState.getSeason();
    }

    /** Returns the current phase on the client side. */
    public static SeasonCalendar.Phase getClientPhase() {
        return ClientSeasonState.getPhase();
    }

    /** Returns the active weather ID on the client side. */
    public static String getClientWeatherId() {
        return ClientSeasonState.getWeatherId();
    }

    /** Returns the display label on the client side. e.g. "Early Spring, Year 2" */
    public static String getClientDisplayLabel() {
        return ClientSeasonState.getDisplayLabel();
    }

    /** Returns the client-side intensity. */
    public static float getClientIntensity() {
        return ClientSeasonState.getIntensity();
    }
}