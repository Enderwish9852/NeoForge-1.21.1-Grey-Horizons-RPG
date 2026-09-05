package net.enderwish.Atmospheric_Overhaul_Subpack.core.season;

import net.enderwish.Atmospheric_Overhaul_Subpack.core.weather.WindDirection;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public class SeasonData extends SavedData{
    // NBT keys
    private static final String KEY_TOTAL_DAYS = "totalDays";
    private static final String KEY_TICKS_TODAY = "ticksToday";
    private static final String KEY_ACTIVE_WEATHER = "activeWeather";
    private static final String KEY_WEATHER_TICKS = "weatherTicks";
    private static final String KEY_WIND_DIRECTION = "windDirection";
    private static final String KEY_WIND_SPEED = "windSpeed";
    private static final String KEY_WIND_TARGET_SPEED = "windTargetSpeed";
    private static final String KEY_WIND_TURBULENCE = "windTurbulence";
    private static final String KEY_WIND_GUST_FACTOR = "windGustFactor";
    private static final String DATA_NAME = "greyhorizons";

    // State
    private int totalDays = 0;
    private int ticksToday = 0;
    private String activeWeatherId = "clear";
    private int weatherTicksRemaining = 0;
    private  float activeIntensity = 0.0f;

    // Wind state — persisted so wind doesn't reset to defaults on relog
    private WindDirection windDirection = WindDirection.SOUTHWEST;
    private float windSpeed = 0.1f;
    private float windTargetSpeed = 0.1f;
    private float windTurbulence = 0.1f;
    private float windGustFactor = 1.0f;

    // Access
    // Get the SeasonData for the given Serverlevel
    public static SeasonData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(
                        SeasonData::new,
                        SeasonData::load
                ),
                DATA_NAME
        );
    }
    // TICK
    // Call this every server tick from ClimateEventHandler
    public TickResult tick() {
        ticksToday++;
        weatherTicksRemaining = Math.max(0, weatherTicksRemaining - 1);
        TickResult result = TickResult.NOTHING;
        // Day boundary - Minecraft day
        if (ticksToday >= 24000) {
            ticksToday = 0;

            SeasonCalendar.Season oldSeason = getSeason();
            SeasonCalendar.Phase oldPhase = getPhase();

            totalDays++;
            setDirty();

            SeasonCalendar.Season newSeason = getSeason();
            SeasonCalendar.Phase newPhase = getPhase();

            if (newSeason != oldSeason) {
                result = TickResult.SEASON_CHANGED;
            } else if (newPhase != oldPhase) {
                result = TickResult.PHASE_CHANGED;
            } else {
                result = TickResult.DAY_CHANGED;
            }
        }
        return result;
    }
    // What changed this tick
    public enum TickResult {
        NOTHING, // mid-day
        DAY_CHANGED, // new day
        PHASE_CHANGED, // season phase changed
        SEASON_CHANGED, // season changed
    }

    // Queries
    // Current season derived from totalDays
    public SeasonCalendar.Season getSeason() {
        return SeasonCalendar.getSeason(getYearDay());
    }
    // Current phase
    public SeasonCalendar.Phase getPhase() {
        return SeasonCalendar.getPhase(getYearDay());
    }
    // Day within the current year (0-79)
    public int getYearDay() {
        return SeasonCalendar.toYearDay(totalDays);
    }
    // Day within the current season (0-79)
    public int getSeasonDay() {
        return SeasonCalendar.getDayInSeason(getYearDay());
    }
    // How many full years have passed
    public int getYear() {
        return SeasonCalendar.getYear(totalDays);
    }
    // The ever-incrementing total day count
    public int getTotalDays() {
        return totalDays;
    }
    // Ticks elapsed in the current in-game day
    public int getTicksToday() {
        return ticksToday;
    }
    // ID of the currently active weather
    public String getActiveWeatherId() {
        return  activeWeatherId;
    }
    // Ticks remaining for current weather
    public int getWeatherTicksRemaining() {
        return weatherTicksRemaining;
    }
    // Current weather intensity
    public float getActiveIntensity() {
        return activeIntensity;
    }
    // True if a weather re-roll is needed this tick
    public boolean needsWeatherRoll() {
        return weatherTicksRemaining <= 0;
    }
    // Human-readable label
    public String getDisplayLabel() {
        return SeasonCalendar.getDisplayLabel(totalDays);
    }

    // ── Wind queries ──────────────────────────────────────────────────────────
    public WindDirection getWindDirection() { return windDirection; }
    public float getWindSpeed()             { return windSpeed; }
    public float getWindTargetSpeed()       { return windTargetSpeed; }
    public float getWindTurbulence()        { return windTurbulence; }
    public float getWindGustFactor()        { return windGustFactor; }

    // Setters
    // Called by WeatherRoller after picking the next weather
    public void setActiveWeather(String weatherId, int durationTicks, float intensity) {
        this.activeWeatherId = weatherId;
        this.weatherTicksRemaining = durationTicks;
        this.activeIntensity = intensity;
        setDirty();
    }
    // Force-set the season day command
    public void setTotalDays(int totalDays) {
        this.totalDays = Math.max(0, totalDays);
        setDirty();
    }

    // ── Wind setters ──────────────────────────────────────────────────────────
    // Called by WindManager every tick after advancing wind state. Marks
    // dirty only on meaningful change to avoid spamming disk writes every
    // single tick from tiny gust/interpolation drift — see WindManager.
    public void setWindState(WindDirection direction, float speed, float targetSpeed,
                             float turbulence, float gustFactor) {
        this.windDirection = direction;
        this.windSpeed = speed;
        this.windTargetSpeed = targetSpeed;
        this.windTurbulence = turbulence;
        this.windGustFactor = gustFactor;
        setDirty();
    }

    // Save/Laod
    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putInt(KEY_TOTAL_DAYS, totalDays);
        tag.putInt(KEY_TICKS_TODAY, ticksToday);
        tag.putString(KEY_ACTIVE_WEATHER, activeWeatherId);
        tag.putInt(KEY_WEATHER_TICKS, weatherTicksRemaining);
        tag.putFloat("activeIntensity", activeIntensity);
        tag.putString(KEY_WIND_DIRECTION, windDirection.name());
        tag.putFloat(KEY_WIND_SPEED, windSpeed);
        tag.putFloat(KEY_WIND_TARGET_SPEED, windTargetSpeed);
        tag.putFloat(KEY_WIND_TURBULENCE, windTurbulence);
        tag.putFloat(KEY_WIND_GUST_FACTOR, windGustFactor);
        return tag;
    }
    public static SeasonData load(CompoundTag tag, HolderLookup.Provider provider) {
        SeasonData data = new SeasonData();
        data.totalDays = tag.getInt(KEY_TOTAL_DAYS);
        data.ticksToday = tag.getInt(KEY_TICKS_TODAY);
        data.activeWeatherId = tag.getString(KEY_ACTIVE_WEATHER);
        data.weatherTicksRemaining = tag.getInt(KEY_WEATHER_TICKS);
        data.activeIntensity = tag.getFloat("activeIntensity");
        if (tag.contains(KEY_WIND_DIRECTION)) {
            try {
                data.windDirection = WindDirection.valueOf(tag.getString(KEY_WIND_DIRECTION));
            } catch (IllegalArgumentException ignored) {
                data.windDirection = WindDirection.SOUTHWEST;
            }
        }
        data.windSpeed = tag.getFloat(KEY_WIND_SPEED);
        data.windTargetSpeed = tag.getFloat(KEY_WIND_TARGET_SPEED);
        data.windTurbulence = tag.getFloat(KEY_WIND_TURBULENCE);
        data.windGustFactor = tag.contains(KEY_WIND_GUST_FACTOR) ? tag.getFloat(KEY_WIND_GUST_FACTOR) : 1.0f;
        return data;
    }
}
