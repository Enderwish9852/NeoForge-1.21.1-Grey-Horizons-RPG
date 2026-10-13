package net.enderwish.Belliarium_Monstrarium_Subpack.core.diary;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DejaVuMemory
 *
 * In-memory only (same convention as DiaryLimbo/AdrenalineManager): which
 * container positions this player looted during the tracking window that
 * just rewound. Built once in DayTracker.stepWakeUp from DayTrackerData's
 * looted-containers set, right before that data is wiped by deactivate().
 * Cleared when the Deja Vu status itself expires.
 */
public final class DejaVuMemory {
    public static final DejaVuMemory INSTANCE = new DejaVuMemory();
    private DejaVuMemory() {}

    private final Map<UUID, LongOpenHashSet> lootedPositions = new ConcurrentHashMap<>();

    public void set(UUID playerId, LongOpenHashSet positions) {
        if (positions.isEmpty()) { lootedPositions.remove(playerId); return; }
        lootedPositions.put(playerId, positions);
    }

    public boolean isRemembered(UUID playerId, long posKey) {
        LongOpenHashSet set = lootedPositions.get(playerId);
        return set != null && set.contains(posKey);
    }

    public void clear(UUID playerId) {
        lootedPositions.remove(playerId);
    }

    /** Forgets a single position (its container was just destroyed) without wiping the rest of the player's memory. */
    public void forget(UUID playerId, long posKey) {
        LongOpenHashSet set = lootedPositions.get(playerId);
        if (set == null) return;
        set.remove(posKey);
        if (set.isEmpty()) lootedPositions.remove(playerId);
    }
}
