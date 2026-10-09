package net.enderwish.Belliarium_Monstrarium_Subpack.core.diary;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * DayTrackerData
 *
 * The saved-to-world part of the Survivor's Diary. Same SavedData pattern as the
 * Atmospheric subpack's SeasonData. Later slices will hang container/entity
 * baselines and the player snapshot off this same object.
 */
public class DayTrackerData extends SavedData {

    private static final String DATA_NAME = "gh_belliarium_day_tracker";

    private boolean active = false;
    private long startGameTime = 0L;
    private BlockChangeLog blockLog = new BlockChangeLog();

    public static DayTrackerData get(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(DayTrackerData::new, DayTrackerData::load),
                DATA_NAME
        );
    }

    public boolean isActive() { return active; }
    public long getStartGameTime() { return startGameTime; }
    public BlockChangeLog getBlockLog() { return blockLog; }

    public void start(long gameTime) {
        active = true;
        startGameTime = gameTime;
        blockLog.clear();
        setDirty();
    }

    public void deactivate() {
        active = false;
        blockLog.clear();
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putBoolean("active", active);
        tag.putLong("start_game_time", startGameTime);
        tag.put("block_log", blockLog.save());
        return tag;
    }

    public static DayTrackerData load(CompoundTag tag, HolderLookup.Provider provider) {
        DayTrackerData data = new DayTrackerData();
        data.active = tag.getBoolean("active");
        data.startGameTime = tag.getLong("start_game_time");
        data.blockLog = BlockChangeLog.load(tag.getCompound("block_log"));
        return data;
    }
}
