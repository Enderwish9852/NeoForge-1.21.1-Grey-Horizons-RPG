package net.enderwish.Belliarium_Monstrarium_Subpack.core.diary;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * PendingFixData
 *
 * Rewind fixes that couldn't be applied at rollback time because their chunk
 * wasn't loaded. Deliberately a SEPARATE SavedData from DayTrackerData: a window
 * ending or a new one starting must never wipe these, since a later day's
 * rollback may be queued up behind them.
 *
 *   containers      chunk -> (block pos -> block entity NBT to put back)
 *   entityRestore   uuid  -> baseline NBT to reset the entity to when it next loads
 *   entityDiscard   uuid  -> entity that appeared during the rewound day; remove on load
 *   entityRecreate  chunk -> (uuid -> NBT of an entity that died and must be recreated)
 *
 * Entries that never match anything stay in the file harmlessly.
 */
public class PendingFixData extends SavedData {

    private static final String DATA_NAME = "gh_belliarium_pending_fixes";

    private final Map<Long, Map<Long, CompoundTag>> containers = new HashMap<>();
    private final Map<UUID, CompoundTag> entityRestore = new HashMap<>();
    private final Set<UUID> entityDiscard = new HashSet<>();
    private final Map<Long, Map<UUID, CompoundTag>> entityRecreate = new HashMap<>();

    public static PendingFixData get(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(PendingFixData::new, PendingFixData::load),
                DATA_NAME
        );
    }

    // ── Containers ────────────────────────────────────────────────────────────

    public void addContainer(long chunkKey, long blockPos, CompoundTag tag) {
        containers.computeIfAbsent(chunkKey, k -> new HashMap<>()).put(blockPos, tag);
        setDirty();
    }

    public Set<Long> containerChunkKeys() { return new HashSet<>(containers.keySet()); }

    public Map<Long, CompoundTag> removeContainerChunk(long chunkKey) {
        Map<Long, CompoundTag> removed = containers.remove(chunkKey);
        if (removed != null) setDirty();
        return removed == null ? Map.of() : removed;
    }

    // ── Entities ──────────────────────────────────────────────────────────────

    public void addEntityRestore(UUID id, CompoundTag tag) {
        entityRestore.put(id, tag);
        setDirty();
    }

    public CompoundTag takeEntityRestore(UUID id) {
        CompoundTag removed = entityRestore.remove(id);
        if (removed != null) setDirty();
        return removed;
    }

    public void addEntityDiscard(UUID id) {
        if (entityDiscard.add(id)) setDirty();
    }

    public boolean takeEntityDiscard(UUID id) {
        boolean removed = entityDiscard.remove(id);
        if (removed) setDirty();
        return removed;
    }

    public boolean hasEntityFix(UUID id) {
        return entityRestore.containsKey(id) || entityDiscard.contains(id);
    }

    public void addEntityRecreate(long chunkKey, UUID id, CompoundTag tag) {
        entityRecreate.computeIfAbsent(chunkKey, k -> new HashMap<>()).put(id, tag);
        setDirty();
    }

    public Set<Long> recreateChunkKeys() { return new HashSet<>(entityRecreate.keySet()); }

    public Map<UUID, CompoundTag> removeEntityRecreateChunk(long chunkKey) {
        Map<UUID, CompoundTag> removed = entityRecreate.remove(chunkKey);
        if (removed != null) setDirty();
        return removed == null ? Map.of() : removed;
    }

    // ── Status ────────────────────────────────────────────────────────────────

    public boolean isEmpty() {
        return containers.isEmpty() && entityRestore.isEmpty()
                && entityDiscard.isEmpty() && entityRecreate.isEmpty();
    }

    public int containerCount() {
        int total = 0;
        for (Map<Long, CompoundTag> chunk : containers.values()) total += chunk.size();
        return total;
    }

    public int entityRestoreCount() { return entityRestore.size(); }
    public int entityDiscardCount() { return entityDiscard.size(); }

    public int entityRecreateCount() {
        int total = 0;
        for (Map<UUID, CompoundTag> chunk : entityRecreate.values()) total += chunk.size();
        return total;
    }

    // ── Save / load ───────────────────────────────────────────────────────────

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag containerList = new ListTag();
        for (Map.Entry<Long, Map<Long, CompoundTag>> chunk : containers.entrySet()) {
            CompoundTag c = new CompoundTag();
            c.putLong("chunk", chunk.getKey());
            ListTag entries = new ListTag();
            for (Map.Entry<Long, CompoundTag> e : chunk.getValue().entrySet()) {
                CompoundTag entry = new CompoundTag();
                entry.putLong("pos", e.getKey());
                entry.put("data", e.getValue());
                entries.add(entry);
            }
            c.put("entries", entries);
            containerList.add(c);
        }
        tag.put("containers", containerList);

        ListTag restoreList = new ListTag();
        for (Map.Entry<UUID, CompoundTag> e : entityRestore.entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("id", e.getKey());
            entry.put("data", e.getValue());
            restoreList.add(entry);
        }
        tag.put("entity_restore", restoreList);

        ListTag discardList = new ListTag();
        for (UUID id : entityDiscard) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("id", id);
            discardList.add(entry);
        }
        tag.put("entity_discard", discardList);

        ListTag recreateList = new ListTag();
        for (Map.Entry<Long, Map<UUID, CompoundTag>> chunk : entityRecreate.entrySet()) {
            CompoundTag c = new CompoundTag();
            c.putLong("chunk", chunk.getKey());
            ListTag entries = new ListTag();
            for (Map.Entry<UUID, CompoundTag> e : chunk.getValue().entrySet()) {
                CompoundTag entry = new CompoundTag();
                entry.putUUID("id", e.getKey());
                entry.put("data", e.getValue());
                entries.add(entry);
            }
            c.put("entries", entries);
            recreateList.add(c);
        }
        tag.put("entity_recreate", recreateList);
        return tag;
    }

    public static PendingFixData load(CompoundTag tag, HolderLookup.Provider provider) {
        PendingFixData data = new PendingFixData();

        ListTag containerList = tag.getList("containers", Tag.TAG_COMPOUND);
        for (int i = 0; i < containerList.size(); i++) {
            CompoundTag c = containerList.getCompound(i);
            long chunkKey = c.getLong("chunk");
            ListTag entries = c.getList("entries", Tag.TAG_COMPOUND);
            for (int j = 0; j < entries.size(); j++) {
                CompoundTag entry = entries.getCompound(j);
                data.containers.computeIfAbsent(chunkKey, k -> new HashMap<>())
                        .put(entry.getLong("pos"), entry.getCompound("data"));
            }
        }

        ListTag restoreList = tag.getList("entity_restore", Tag.TAG_COMPOUND);
        for (int i = 0; i < restoreList.size(); i++) {
            CompoundTag entry = restoreList.getCompound(i);
            if (entry.hasUUID("id")) data.entityRestore.put(entry.getUUID("id"), entry.getCompound("data"));
        }

        ListTag discardList = tag.getList("entity_discard", Tag.TAG_COMPOUND);
        for (int i = 0; i < discardList.size(); i++) {
            CompoundTag entry = discardList.getCompound(i);
            if (entry.hasUUID("id")) data.entityDiscard.add(entry.getUUID("id"));
        }

        ListTag recreateList = tag.getList("entity_recreate", Tag.TAG_COMPOUND);
        for (int i = 0; i < recreateList.size(); i++) {
            CompoundTag c = recreateList.getCompound(i);
            long chunkKey = c.getLong("chunk");
            ListTag entries = c.getList("entries", Tag.TAG_COMPOUND);
            for (int j = 0; j < entries.size(); j++) {
                CompoundTag entry = entries.getCompound(j);
                if (!entry.hasUUID("id")) continue;
                data.entityRecreate.computeIfAbsent(chunkKey, k -> new HashMap<>())
                        .put(entry.getUUID("id"), entry.getCompound("data"));
            }
        }
        return data;
    }
}
