package net.enderwish.HUD_Visuals_Subpack.core;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

public class SeasonData extends SavedData {
    private static final String IDENTIFIER = "gh_hud_visuals_seasons";
    public static final int DAYS_PER_SEASON = 20;

    private Season currentSeason = Season.SPRING;
    private int daysPassed = 0; // 0 to 19 internal

    public SeasonData() {}

    /**
     * Simplified tick: Just increments and handles the rollover.
     * We moved the "Time Check" to the SeasonManager to avoid double-ticking.
     */
    public void tick(ServerLevel level) {
        this.daysPassed++;

        if (this.daysPassed >= DAYS_PER_SEASON) {
            this.daysPassed = 0;
            this.currentSeason = getNextSeason(this.currentSeason);
        }

        this.setDirty(); // THIS IS THE MOST IMPORTANT LINE FOR SAVING
    }

    private Season getNextSeason(Season current) {
        return switch (current) {
            case SPRING -> Season.SUMMER;
            case SUMMER -> Season.AUTUMN;
            case AUTUMN -> Season.WINTER;
            case WINTER -> Season.SPRING;
        };
    }

    // --- Getters & Setters ---

    public Season getCurrentSeason() { return currentSeason; }

    public void setCurrentSeason(Season season) {
        this.currentSeason = season;
        this.setDirty();
    }

    public int getDisplayDay() { return daysPassed;}

    public void setSeasonDay(int day) {
        this.daysPassed = Math.max(0, Math.min(day, DAYS_PER_SEASON - 1));
        this.setDirty();
    }

    // --- Persistence (NBT) ---

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putString("currentSeason", currentSeason.name());
        tag.putInt("daysPassed", daysPassed);
        return tag;
    }

    // Static load method must match the Factory expectations
    public static SeasonData load(CompoundTag tag, HolderLookup.Provider registries) {
        SeasonData data = new SeasonData();
        String seasonName = tag.getString("currentSeason");
        try {
            data.currentSeason = Season.valueOf(seasonName.isEmpty() ? "SPRING" : seasonName);
        } catch (IllegalArgumentException e) {
            data.currentSeason = Season.SPRING;
        }
        data.daysPassed = tag.getInt("daysPassed");
        return data;
    }

    public static SeasonData get(ServerLevel level) {
        DimensionDataStorage storage = level.getDataStorage();
        // Modern NeoForge Factory pattern
        return storage.computeIfAbsent(new SavedData.Factory<>(
                SeasonData::new,
                SeasonData::load
        ), IDENTIFIER);
    }
}