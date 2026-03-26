package net.enderwish.HUD_Visuals_Subpack.core;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

/**
 * Handles persistent storage and logic for the current season in a specific world.
 * Updated for NeoForge 1.21.1 compatibility.
 */
public class SeasonData extends SavedData {
    private static final String IDENTIFIER = "hud_visuals_seasons";
    private static final int DAYS_PER_SEASON = 7;

    private Season currentSeason = Season.SPRING;
    private int daysPassed = 0;
    private long lastDayTime = 0;

    public SeasonData() {}

    /**
     * Logic to progress time and seasons.
     * Called from your LevelTickEvent in the Manager.
     */
    public void tick(ServerLevel level) {
        long currentTime = level.getDayTime();
        long currentDay = currentTime / 24000;
        long lastDay = lastDayTime / 24000;

        if (currentDay > lastDay) {
            daysPassed++;
            lastDayTime = currentTime;

            if (daysPassed >= DAYS_PER_SEASON) {
                transitionSeason();
            }

            // Mark data as changed so it saves to the world folder
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
     * Updated for 1.21.1: Added @Override and Provider.
     */
    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putString("currentSeason", currentSeason.getSerializedName());
        tag.putInt("daysPassed", daysPassed);
        tag.putLong("lastDayTime", lastDayTime);
        return tag;
    }

    /**
     * Loader helper for the Factory pattern.
     */
    public static SeasonData load(CompoundTag tag, HolderLookup.Provider registries) {
        SeasonData data = new SeasonData();
        String seasonName = tag.getString("currentSeason");

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

        // 1.21.1 uses the Factory object to handle instantiation and loading
        SavedData.Factory<SeasonData> factory = new SavedData.Factory<>(
                SeasonData::new,
                SeasonData::load,
                null
        );

        return storage.computeIfAbsent(factory, IDENTIFIER);
    }
}