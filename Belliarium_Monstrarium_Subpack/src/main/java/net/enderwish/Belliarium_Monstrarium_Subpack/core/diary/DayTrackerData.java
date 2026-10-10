package net.enderwish.Belliarium_Monstrarium_Subpack.core.diary;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.UUID;

/**
 * DayTrackerData
 *
 * The saved-to-world part of the Survivor's Diary: block log, container
 * baseline, entity baseline, plus the owner's player snapshot and the
 * environment snapshot (time, weather, season day, wind).
 */
public class DayTrackerData extends SavedData {

    private static final String DATA_NAME = "gh_belliarium_day_tracker";

    private boolean active = false;
    private long startGameTime = 0L;
    private UUID owner = null;
    private CompoundTag playerSnapshot = new CompoundTag();
    private CompoundTag environment = new CompoundTag();
    private BlockChangeLog blockLog = new BlockChangeLog();
    private ContainerBaseline containers = new ContainerBaseline();
    private EntityBaseline entities = new EntityBaseline();

    public static DayTrackerData get(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(DayTrackerData::new, DayTrackerData::load),
                DATA_NAME
        );
    }

    public boolean isActive() { return active; }
    public long getStartGameTime() { return startGameTime; }
    public UUID getOwner() { return owner; }
    public CompoundTag getPlayerSnapshot() { return playerSnapshot; }
    public CompoundTag getEnvironment() { return environment; }
    public BlockChangeLog getBlockLog() { return blockLog; }
    public ContainerBaseline getContainers() { return containers; }
    public EntityBaseline getEntities() { return entities; }

    public void begin(long gameTime, UUID owner, CompoundTag playerSnapshot, CompoundTag environment) {
        clearAll();
        this.active = true;
        this.startGameTime = gameTime;
        this.owner = owner;
        this.playerSnapshot = playerSnapshot;
        this.environment = environment;
        setDirty();
    }

    public void deactivate() {
        active = false;
        clearAll();
        setDirty();
    }

    private void clearAll() {
        owner = null;
        playerSnapshot = new CompoundTag();
        environment = new CompoundTag();
        blockLog.clear();
        containers.clear();
        entities.clear();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putBoolean("active", active);
        tag.putLong("start_game_time", startGameTime);
        if (owner != null) tag.putUUID("owner", owner);
        tag.put("player_snapshot", playerSnapshot);
        tag.put("environment", environment);
        tag.put("block_log", blockLog.save());
        tag.put("containers", containers.save());
        tag.put("entities", entities.save());
        return tag;
    }

    public static DayTrackerData load(CompoundTag tag, HolderLookup.Provider provider) {
        DayTrackerData data = new DayTrackerData();
        data.active = tag.getBoolean("active");
        data.startGameTime = tag.getLong("start_game_time");
        data.owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
        data.playerSnapshot = tag.getCompound("player_snapshot");
        data.environment = tag.getCompound("environment");
        data.blockLog = BlockChangeLog.load(tag.getCompound("block_log"));
        data.containers = ContainerBaseline.load(tag.getCompound("containers"));
        data.entities = EntityBaseline.load(tag.getCompound("entities"));
        return data;
    }
}
