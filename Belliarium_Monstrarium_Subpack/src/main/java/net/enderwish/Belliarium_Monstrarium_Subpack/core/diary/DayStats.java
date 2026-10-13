package net.enderwish.Belliarium_Monstrarium_Subpack.core.diary;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.HashMap;
import java.util.Map;

/** Running numbers for the day's printed summary (Part 4). Saved with the tracking data. */
public class DayStats {

    private static final double MAX_STEP = 64.0; // bigger jumps are teleports, not travel

    private double blocksTraveled;
    private final Map<String, Integer> gathered = new HashMap<>();
    private double lastX, lastZ;
    private boolean hasLast;

    public double getBlocksTraveled() { return blocksTraveled; }
    public Map<String, Integer> getGathered() { return gathered; }

    public void sample(double x, double z) {
        if (hasLast) {
            double d = Math.hypot(x - lastX, z - lastZ);
            if (d < MAX_STEP) blocksTraveled += d;
        }
        lastX = x;
        lastZ = z;
        hasLast = true;
    }

    public void addGathered(String itemId, int count) { gathered.merge(itemId, count, Integer::sum); }

    public void clear() {
        blocksTraveled = 0;
        gathered.clear();
        hasLast = false;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("blocks", blocksTraveled);
        ListTag list = new ListTag();
        for (Map.Entry<String, Integer> e : gathered.entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putString("id", e.getKey());
            entry.putInt("count", e.getValue());
            list.add(entry);
        }
        tag.put("gathered", list);
        return tag;
    }

    public static DayStats load(CompoundTag tag) {
        DayStats stats = new DayStats();
        stats.blocksTraveled = tag.getDouble("blocks");
        ListTag list = tag.getList("gathered", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            stats.gathered.put(entry.getString("id"), entry.getInt("count"));
        }
        return stats;
    }
}
