package net.enderwish.Belliarium_Monstrarium_Subpack.core.diary;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * EntityBaseline
 *
 * Three sets, all keyed by UUID:
 *   baseline  -- NBT of non-player entities that existed when tracking began
 *                (or were first loaded from disk afterwards)
 *   spawned   -- entities that appeared during the window; removed on rollback
 *   destroyed -- baseline entities that were killed/discarded; recreated on rollback
 *
 * An entity that spawned and then died during the window simply drops out of
 * `spawned` (nothing to undo). Unloading a chunk does not count as destruction.
 */
public class EntityBaseline {

    private final Map<UUID, CompoundTag> baseline = new HashMap<>();
    private final Set<UUID> spawned = new HashSet<>();
    private final Set<UUID> destroyed = new HashSet<>();

    public boolean knows(UUID id) {
        return baseline.containsKey(id) || spawned.contains(id);
    }

    public boolean putBaseline(UUID id, CompoundTag tag) {
        if (knows(id)) return false;
        baseline.put(id, tag);
        return true;
    }

    public boolean markSpawned(UUID id) {
        if (knows(id)) return false;
        return spawned.add(id);
    }

    /** Returns true if anything changed. */
    public boolean onDestroyed(UUID id) {
        if (baseline.containsKey(id)) return destroyed.add(id);
        return spawned.remove(id);
    }

    public boolean isDestroyed(UUID id) { return destroyed.contains(id); }
    public Map<UUID, CompoundTag> getBaseline() { return baseline; }
    public Set<UUID> getSpawned() { return spawned; }

    public int baselineCount() { return baseline.size(); }
    public int spawnedCount() { return spawned.size(); }
    public int destroyedCount() { return destroyed.size(); }

    public void clear() {
        baseline.clear();
        spawned.clear();
        destroyed.clear();
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();

        ListTag list = new ListTag();
        for (Map.Entry<UUID, CompoundTag> entry : baseline.entrySet()) {
            CompoundTag e = new CompoundTag();
            e.putUUID("id", entry.getKey());
            e.put("data", entry.getValue());
            list.add(e);
        }
        tag.put("baseline", list);
        tag.putLongArray("spawned", toLongs(spawned));
        tag.putLongArray("destroyed", toLongs(destroyed));
        return tag;
    }

    public static EntityBaseline load(CompoundTag tag) {
        EntityBaseline result = new EntityBaseline();

        ListTag list = tag.getList("baseline", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag e = list.getCompound(i);
            if (e.hasUUID("id")) result.baseline.put(e.getUUID("id"), e.getCompound("data"));
        }
        result.spawned.addAll(fromLongs(tag.getLongArray("spawned")));
        result.destroyed.addAll(fromLongs(tag.getLongArray("destroyed")));
        return result;
    }

    private static long[] toLongs(Set<UUID> ids) {
        long[] out = new long[ids.size() * 2];
        int i = 0;
        for (UUID id : ids) {
            out[i++] = id.getMostSignificantBits();
            out[i++] = id.getLeastSignificantBits();
        }
        return out;
    }

    private static Set<UUID> fromLongs(long[] in) {
        Set<UUID> out = new HashSet<>();
        for (int i = 0; i + 1 < in.length; i += 2) {
            out.add(new UUID(in[i], in[i + 1]));
        }
        return out;
    }
}
