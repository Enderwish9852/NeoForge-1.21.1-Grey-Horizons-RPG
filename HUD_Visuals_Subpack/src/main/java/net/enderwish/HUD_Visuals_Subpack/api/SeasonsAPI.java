package net.enderwish.HUD_Visuals_Subpack.api;

import net.enderwish.HUD_Visuals_Subpack.core.season.SeasonCalendar;
import net.enderwish.HUD_Visuals_Subpack.core.season.SeasonData;
import net.enderwish.HUD_Visuals_Subpack.core.weather.WeatherDefinition;
import net.enderwish.HUD_Visuals_Subpack.core.weather.WeatherRegistry;
import net.minecraft.server.level.ServerLevel;

/**
 * SeasonsAPI
 *
 * The ONLY class other subpacks should import.
 * A static facade over the seasons internals.
 *
 * Other subpacks never import SeasonData, WeatherRegistry,
 * WeatherRoller, or SeasonCalendar directly.
 *
 * Client-side methods will be added once ClientSeasonState is built.
 *
 * Usage example (from Farming subpack):
 *   import net.enderwish.HUD_Visuals_Subpack.api.SeasonsAPI;
 *
 *   SeasonCalendar.Season season = SeasonsAPI.getSeason(level);
 *   if (season == SeasonCalendar.Season.SPRING) { ... }
 */
public final class SeasonsAPI {

    private SeasonsAPI() {} // static only, never instantiate

    // ── Calendar queries (server-side) ────────────────────────────────────────

    /**
     * Returns the current season.
     * e.g. SPRING, SUMMER, AUTUMN, WINTER
     */
    public static SeasonCalendar.Season getSeason(ServerLevel level) {
        return SeasonData.get(level).getSeason();
    }

    /**
     * Returns the current phase within the season.
     * e.g. EARLY, MID, LATE
     */
    public static SeasonCalendar.Phase getPhase(ServerLevel level) {
        return SeasonData.get(level).getPhase();
    }

    /**
     * Returns the current day within the year (0-79).
     */
    public static int getYearDay(ServerLevel level) {
        return SeasonData.get(level).getYearDay();
    }

    /**
     * Returns the current day within the season (0-19).
     */
    public static int getSeasonDay(ServerLevel level) {
        return SeasonData.get(level).getSeasonDay();
    }

    /**
     * Returns how many full years have passed.
     * Year 0 = first year, Year 1 = second year, etc.
     */
    public static int getYear(ServerLevel level) {
        return SeasonData.get(level).getYear();
    }

    /**
     * Returns the total ever-incrementing day count.
     */
    public static int getTotalDays(ServerLevel level) {
        return SeasonData.get(level).getTotalDays();
    }

    /**
     * Returns a human-readable label for the current season + phase + year.
     * e.g. "Early Spring, Year 2"
     * Ready to use directly in HUD or chat messages.
     */
    public static String getDisplayLabel(ServerLevel level) {
        return SeasonData.get(level).getDisplayLabel();
    }

    // ── Weather queries (server-side) ─────────────────────────────────────────

    /**
     * Returns the ID of the currently active weather.
     * e.g. "light_rain", "blizzard", "clear"
     */
    public static String getActiveWeatherId(ServerLevel level) {
        return SeasonData.get(level).getActiveWeatherId();
    }

    /**
     * Returns the full WeatherDefinition for the currently active weather.
     * Use this to check hasRain(), hasThunder(), isSpecial() etc.
     */
    public static WeatherDefinition getActiveWeather(ServerLevel level) {
        return WeatherRegistry.INSTANCE.getByName(
                SeasonData.get(level).getActiveWeatherId()
        );
    }

    /**
     * Returns true if it is currently raining or snowing.
     */
    public static boolean isPrecipitating(ServerLevel level) {
        return getActiveWeather(level).hasRain();
    }

    /**
     * Returns true if there is currently a thunderstorm.
     */
    public static boolean isThundering(ServerLevel level) {
        return getActiveWeather(level).hasThunder();
    }

    /**
     * Returns true if the active weather is special
     * (fixed 24000 duration + 1.0 intensity — e.g. blizzard, heatwave).
     */
    public static boolean isSpecialWeather(ServerLevel level) {
        return getActiveWeather(level).isSpecial();
    }

    /**
     * Returns the current weather intensity (0.0 - 1.0).
     */
    public static float getWeatherIntensity(ServerLevel level) {
        return SeasonData.get(level).getActiveIntensity();
    }

    /**
     * Returns how many ticks remaining for the current weather.
     */
    public static int getWeatherTicksRemaining(ServerLevel level) {
        return SeasonData.get(level).getWeatherTicksRemaining();
    }

    // ── Convenience checks ────────────────────────────────────────────────────

    /**
     * Returns true if the current season matches the given season.
     * e.g. SeasonsAPI.isSeason(level, SeasonCalendar.Season.WINTER)
     */
    public static boolean isSeason(ServerLevel level, SeasonCalendar.Season season) {
        return getSeason(level) == season;
    }

    /**
     * Returns true if the current phase matches the given phase.
     */
    public static boolean isPhase(ServerLevel level, SeasonCalendar.Phase phase) {
        return getPhase(level) == phase;
    }

    /**
     * Returns true if it is currently winter AND a special weather is active.
     */
    public static boolean isWinterStorm(ServerLevel level) {
        return isSeason(level, SeasonCalendar.Season.WINTER) && isSpecialWeather(level);
    }

    /**
     * Returns true if a heatwave is currently active.
     */
    public static boolean isHeatwave(ServerLevel level) {
        return getActiveWeatherId(level).equals("heatwave");
    }

    // ── Client-side queries ───────────────────────────────────────────────────
    // TODO: added once ClientSeasonState is built

}