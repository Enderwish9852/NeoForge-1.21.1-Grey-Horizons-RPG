package net.enderwish.Atmospheric_Overhaul_Subpack.core.season;

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
    private static final String DATA_NAME = "greyhorizons";

    // State
    private int totalDays = 0;
    private int ticksToday = 0;
    private String activeWeatherId = "clear";
    private int weatherTicksRemaining = 0;
    private  float activeIntensity = 0.0f;

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
    // Save/Laod
    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putInt(KEY_TOTAL_DAYS, totalDays);
        tag.putInt(KEY_TICKS_TODAY, ticksToday);
        tag.putString(KEY_ACTIVE_WEATHER, activeWeatherId);
        tag.putInt(KEY_WEATHER_TICKS, weatherTicksRemaining);
        tag.putFloat("activeIntensity", activeIntensity);
        return tag;
    }
    public static SeasonData load(CompoundTag tag, HolderLookup.Provider provider) {
        SeasonData data = new SeasonData();
        data.totalDays = tag.getInt(KEY_TOTAL_DAYS);
        data.ticksToday = tag.getInt(KEY_TICKS_TODAY);
        data.activeWeatherId = tag.getString(KEY_ACTIVE_WEATHER);
        data.weatherTicksRemaining = tag.getInt(KEY_WEATHER_TICKS);
        data.activeIntensity = tag.getFloat("activeIntensity");
        return data;
    }
}
