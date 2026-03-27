package net.enderwish.HUD_Visuals_Subpack.core;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

/**
 * Handles persistent storage and logic for the current season in a specific world.
 * Updated for NeoForge 1.21.1 and synced with HUD requirements (20-day cycle).
 */
public class SeasonData extends SavedData {
    private static final String IDENTIFIER = "hud_visuals_seasons";

    // Updated to 20 days per season as per the design goal
    public static final int DAYS_PER_SEASON = 20;

    private Season currentSeason = Season.SPRING;
    private int daysPassed = 0; // Tracks progress within the current season (0 to 19)
    private long lastDayTime = 0;

    public SeasonData() {}

    /**
     * Logic to progress time and seasons.
     * Checks if the Minecraft world day has incremented since the last tick.
     */
    public void tick(ServerLevel level) {
        long currentTime = level.getDayTime();
        long currentDay = currentTime / 24000;
        long lastDay = lastDayTime / 24000;

        // If the sun has risen on a new day
        if (currentDay > lastDay) {
            daysPassed++;
            lastDayTime = currentTime;

            // Check if it is time to transition to the next season
            if (daysPassed >= DAYS_PER_SEASON) {
                transitionSeason();
            }

            // Mark data as dirty so NeoForge saves it to disk
            this.setDirty();
        }
    }

    private void transitionSeason() {
        this.currentSeason = this.currentSeason.next();
        this.daysPassed = 0;
    }

    public Season getCurrentSeason() {
        return currentSeason;
    }

    /**
     * Manually set the season (called by SeasonCommand).
     */
    public void setCurrentSeason(Season season) {
        this.currentSeason = season;
        this.setDirty();
    }

    /**
     * Manually set the day of the season (called by SeasonCommand).
     * Converts 1-indexed input back to 0-indexed internal logic.
     */
    public void setSeasonDay(int day) {
        // Clamp between 1 and 20, then subtract 1 for internal 0-19 tracking
        this.daysPassed = Math.max(0, Math.min(DAYS_PER_SEASON - 1, day - 1));
        this.setDirty();
    }

    /**
     * @return The 1-indexed day of the season (1-20) for HUD display.
     */
    public int getDisplayDay() {
        return daysPassed + 1;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putString("currentSeason", currentSeason.getSerializedName());
        tag.putInt("daysPassed", daysPassed);
        tag.putLong("lastDayTime", lastDayTime);
        return tag;
    }

    /**
     * Static loader for the SavedData Factory.
     */
    public static SeasonData load(CompoundTag tag, HolderLookup.Provider registries) {
        SeasonData data = new SeasonData();
        String seasonName = tag.getString("currentSeason");

        // Match the string back to the Enum
        for (Season s : Season.values()) {
            if (s.getSerializedName().equals(seasonName)) {
                data.currentSeason = s;
                break;
            }
        }

        data.daysPassed = tag.getInt("daysPassed");
        data.lastDayTime = tag.getLong("lastDayTime");
        return data;
    }

    /**
     * Retrieves or creates the SeasonData using the 1.21.1 Factory pattern.
     */
    public static SeasonData get(ServerLevel level) {
        DimensionDataStorage storage = level.getDataStorage();

        // Factory pattern: Constructor, Loader, and DataFixerType (null)
        SavedData.Factory<SeasonData> factory = new SavedData.Factory<>(
                SeasonData::new,
                SeasonData::load,
                null
        );

        return storage.computeIfAbsent(factory, IDENTIFIER);
    }
}