package net.enderwish.Atmospheric_Overhaul_Subpack.api;

import net.enderwish.Atmospheric_Overhaul_Subpack.client.ClientSeasonState;
import net.enderwish.Atmospheric_Overhaul_Subpack.core.season.SeasonCalendar;
import net.enderwish.Atmospheric_Overhaul_Subpack.core.season.SeasonData;
import net.enderwish.Atmospheric_Overhaul_Subpack.core.season.SeasonTemperature;
import net.enderwish.Atmospheric_Overhaul_Subpack.core.weather.WeatherDefinition;
import net.enderwish.Atmospheric_Overhaul_Subpack.core.weather.WeatherRegistry;
import net.enderwish.Atmospheric_Overhaul_Subpack.core.weather.WindDirection;
import net.enderwish.Atmospheric_Overhaul_Subpack.network.ModMessages;
import net.enderwish.Atmospheric_Overhaul_Subpack.network.SeasonSyncPacket;
import net.enderwish.Atmospheric_Overhaul_Subpack.network.WindSyncPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
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

    // ── Local chunk temperature (server-side) ─────────────────────────────────

    /**
     * Returns the local "feels like" temperature at a position.
     * Accounts for seasonal base temperature PLUS nearby hot/cold blocks.
     *
     * Scan area: full 16×16 chunk surface + 5 blocks underground.
     *
     * Use this for:
     *   - Composter speed boost calculations
     *   - Player body temperature system (future)
     *   - Crop frost damage (future)
     *
     * Examples:
     *   Plains, winter, standing 1 block from a lit campfire:
     *     seasonal = -0.55 (winter plains), campfire at dist 1 = +0.4/2 = +0.2
     *     localTemp ≈ -0.35 → "Cold" (~-5°C)
     *
     *   Plains, winter, no heat source, blizzard:
     *     seasonal = -0.55 - 0.4 = -0.95 (blizzard at full intensity)
     *     no hot blocks
     *     localTemp ≈ -0.95 → "Freezing" (~-24°C)
     *
     *   Desert, summer, standing on magma:
     *     seasonal = 1.75 (desert + summer + mid phase)
     *     magma at dist 0 = +0.5
     *     localTemp ≈ 2.25 → "Scorching" (+72°C)
     */
    public static float getLocalChunkTemp(ServerLevel level, BlockPos pos) {
        return net.enderwish.Atmospheric_Overhaul_Subpack.core.season
                .LocalChunkTemperature.calculate(level, pos);
    }

    /**
     * Returns the local chunk temperature in Celsius.
     * For player display and debug.
     */
    public static int getLocalChunkTempCelsius(ServerLevel level, BlockPos pos) {
        return net.enderwish.Atmospheric_Overhaul_Subpack.core.season
                .LocalChunkTemperature.toCelsius(
                        getLocalChunkTemp(level, pos));
    }

    /**
     * Returns a human-readable label for the local chunk temperature.
     * e.g. "Freezing", "Cold", "Warm", "Hot", "Scorching"
     */
    public static String getLocalChunkTempLabel(ServerLevel level, BlockPos pos) {
        return net.enderwish.Atmospheric_Overhaul_Subpack.core.season
                .LocalChunkTemperature.getLabel(
                        getLocalChunkTemp(level, pos));
    }

    /**
     * Returns true if the local chunk is considered "hot" for composter boost.
     * Hot = localChunkTemp > 0.5 (warm threshold)
     */
    public static boolean isLocalChunkHot(ServerLevel level, BlockPos pos) {
        return getLocalChunkTemp(level, pos) > 0.5f;
    }

    // ── Wind queries (server-side) ────────────────────────────────────────────

    /**
     * Returns the current wind state on the server for this level.
     * Use for gameplay logic, particle spawning, structure damage etc.
     */
    public static net.enderwish.Atmospheric_Overhaul_Subpack.core.weather.WindState
    getWindState(ServerLevel level) {
        return net.enderwish.Atmospheric_Overhaul_Subpack.core.weather
                .WindManager.INSTANCE.getState(level);
    }

    /** Returns current wind speed (0.0 calm - 1.0 gale) for this level. */
    public static float getWindSpeed(ServerLevel level) {
        return net.enderwish.Atmospheric_Overhaul_Subpack.core.weather
                .WindManager.INSTANCE.getSpeed(level);
    }

    /** Returns the current wind direction for this level. */
    public static net.enderwish.Atmospheric_Overhaul_Subpack.core.weather.WindDirection
    getWindDirection(ServerLevel level) {
        return net.enderwish.Atmospheric_Overhaul_Subpack.core.weather
                .WindManager.INSTANCE.getDirection(level);
    }

    /** Returns true if wind speed exceeds gale threshold for this level. */
    public static boolean isWindGale(ServerLevel level) {
        return getWindState(level).isGale();
    }

    // ── Wind queries (client-side) ────────────────────────────────────────────

    /** Returns the current wind speed on the client. */
    public static float getClientWindSpeed() {
        return ClientSeasonState.getWindSpeed();
    }

    /** Returns the current wind direction on the client. */
    public static net.enderwish.Atmospheric_Overhaul_Subpack.core.weather.WindDirection
    getClientWindDirection() {
        return ClientSeasonState.getWindDirection();
    }

    /** Returns the effective client-side wind X velocity — for animations, particles. */
    public static float getClientWindDx() {
        return ClientSeasonState.getWindDx();
    }

    /** Returns the effective client-side wind Z velocity. */
    public static float getClientWindDz() {
        return ClientSeasonState.getWindDz();
    }

    /** True if wind is currently strong on the client. */
    public static boolean isClientWindy() {
        return ClientSeasonState.isWindy();
    }

    // ── Environment snapshot / restore (Survivor's Diary) ─────────────────────

    /**
     * Captures everything the Survivor's Diary needs to rewind the environment:
     * time of day, season day, weather (with its remaining duration and
     * intensity) and wind. Returned as an opaque tag; callers store it and hand
     * it back to restoreEnvironment() without looking inside.
     *
     * Absolute game time is deliberately NOT part of this -- it only ever moves forward.
     */
    public static CompoundTag captureEnvironment(ServerLevel level) {
        SeasonData data = SeasonData.get(level);
        CompoundTag tag = new CompoundTag();

        tag.putInt("total_days", data.getTotalDays());
        tag.putInt("ticks_today", data.getTicksToday());
        tag.putLong("day_time", level.getDayTime());

        tag.putString("weather_id", data.getActiveWeatherId());
        tag.putInt("weather_ticks", data.getWeatherTicksRemaining());
        tag.putFloat("weather_intensity", data.getActiveIntensity());

        tag.putString("wind_direction", data.getWindDirection().name());
        tag.putFloat("wind_speed", data.getWindSpeed());
        tag.putFloat("wind_target_speed", data.getWindTargetSpeed());
        tag.putFloat("wind_turbulence", data.getWindTurbulence());
        tag.putFloat("wind_gust", data.getWindGustFactor());
        return tag;
    }

    /**
     * Puts the environment back exactly as captured, applies the matching vanilla
     * weather, and syncs the season and wind state to all clients.
     * Fires no season/phase events -- this is a rewind, not a transition.
     *
     * VERIFY IF COMPILE FAILS -- ServerLevel#setDayTime(long) (it's what vanilla's /time set calls).
     */
    public static void restoreEnvironment(ServerLevel level, CompoundTag tag) {
        if (tag == null || tag.isEmpty()) return;
        SeasonData data = SeasonData.get(level);

        data.setTotalDays(tag.getInt("total_days"));
        data.setTicksToday(tag.getInt("ticks_today"));
        level.setDayTime(tag.getLong("day_time"));

        String weatherId = tag.getString("weather_id");
        if (weatherId.isEmpty()) weatherId = "clear";
        int weatherTicks = tag.getInt("weather_ticks");
        data.setActiveWeather(weatherId, weatherTicks, tag.getFloat("weather_intensity"));

        // Same vanilla-side application SeasonEventHandler uses when it rolls weather.
        WeatherDefinition def = WeatherRegistry.INSTANCE.getByName(weatherId);
        int vanillaDuration = Math.max(1, weatherTicks);
        if (def.hasRain()) {
            level.setWeatherParameters(0, vanillaDuration, true, def.hasThunder());
        } else {
            level.setWeatherParameters(vanillaDuration, 0, false, false);
        }

        WindDirection direction;
        try {
            direction = WindDirection.valueOf(tag.getString("wind_direction"));
        } catch (IllegalArgumentException e) {
            direction = WindDirection.SOUTHWEST;
        }
        data.setWindState(direction,
                tag.getFloat("wind_speed"),
                tag.getFloat("wind_target_speed"),
                tag.getFloat("wind_turbulence"),
                tag.getFloat("wind_gust"));

        ModMessages.sendToAllPlayers(new SeasonSyncPacket(
                data.getTotalDays(),
                data.getYearDay(),
                data.getSeason(),
                data.getPhase(),
                data.getActiveWeatherId(),
                data.getActiveIntensity(),
                data.getYear()
        ));
        WindSyncPacket.sendToAll(level, getWindState(level));
    }
}