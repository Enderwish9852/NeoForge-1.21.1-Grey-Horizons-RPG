package net.enderwish.Belliarium_Monstrarium_Subpack.core.map;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MapWorldData
 *
 * Per-world in-memory data tracking explored chunks and player-made map
 * drawings. One map per adventure means everything is shared across all
 * map items in the world.
 *
 * Stored as a set of packed chunk positions (x << 32 | z) plus a sampled
 * terrain height per chunk, and a list of MapDraw annotations.
 *
 * Disk persistence is deferred to a later phase; this phase keeps data
 * in memory for the lifetime of the world.
 */
public class MapWorldData {

    /** Instances keyed by level dimension, one per loaded world. */
    private static final Map<ResourceKey<Level>, MapWorldData> INSTANCES = new ConcurrentHashMap<>();

    /** Packed chunk positions that have been explored. */
    private final Set<Long> exploredChunks = new HashSet<>();
    /** Sampled average surface height per explored chunk. */
    private final java.util.Map<Long, Integer> chunkHeights = new java.util.HashMap<>();
    /** Player-made drawings on the map. */
    private final List<MapDraw> drawings = new ArrayList<>();

    private MapWorldData() {}

    /** Get or create the data for this level. */
    public static MapWorldData get(Level level) {
        return INSTANCES.computeIfAbsent(level.dimension(), k -> new MapWorldData());
    }

    /** Clear all instances (used on world unload). */
    public static void clearAll() {
        INSTANCES.clear();
    }

    /** Mark a chunk as explored, sampling its surface height. */
    public void markExplored(int chunkX, int chunkZ, int surfaceHeight) {
        long packed = pack(chunkX, chunkZ);
        exploredChunks.add(packed);
        chunkHeights.put(packed, surfaceHeight);
    }

    public boolean isExplored(int chunkX, int chunkZ) {
        return exploredChunks.contains(pack(chunkX, chunkZ));
    }

    public int getSurfaceHeight(int chunkX, int chunkZ) {
        return chunkHeights.getOrDefault(pack(chunkX, chunkZ), 0);
    }

    /** Explored chunk count for diagnostics. */
    public int exploredCount() {
        return exploredChunks.size();
    }

    // ── Drawings ──────────────────────────────────────────────────────────

    public List<MapDraw> drawings() {
        return List.copyOf(drawings);
    }

    public void addDrawing(MapDraw draw) {
        drawings.add(draw);
    }

    public void removeDrawing(UUID id) {
        drawings.removeIf(d -> d.id().equals(id));
    }

    public void clearDrawings() {
        drawings.clear();
    }

    private static long pack(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }
}
