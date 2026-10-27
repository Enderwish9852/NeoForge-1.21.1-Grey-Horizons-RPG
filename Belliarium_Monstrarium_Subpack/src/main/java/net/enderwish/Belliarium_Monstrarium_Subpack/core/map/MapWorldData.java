package net.enderwish.Belliarium_Monstrarium_Subpack.core.map;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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
 * Per-world saved data tracking explored chunks and player-made map
 * drawings. One map per adventure means everything is shared across all
 * map items in the world.
 *
 * Stored as a set of packed chunk positions (x << 32 | z) plus a sampled
 * terrain height per chunk, and a list of MapDraw annotations.
 *
 * Saved to {@code world/gh_map_data.dat} in the world save directory,
 * persisted on world save and loaded on world load.
 */
public class MapWorldData {

    private static final Logger LOGGER = LoggerFactory.getLogger(MapWorldData.class);
    private static final String DATA_NAME = "gh_map_data";

    /** Instances keyed by level dimension, one per loaded world. */
    private static final Map<ResourceKey<Level>, MapWorldData> INSTANCES = new ConcurrentHashMap<>();

    /** Packed chunk positions that have been explored. */
    private final Set<Long> exploredChunks = new HashSet<>();
    /** Sampled average surface height per explored chunk. */
    private final java.util.Map<Long, Integer> chunkHeights = new java.util.HashMap<>();
    /** Player-made drawings on the map. */
    private final List<MapDraw> drawings = new ArrayList<>();
    /** Whether the data has unsaved changes. */
    private boolean dirty = false;

    private MapWorldData() {}

    /** Get or create the data for this level. */
    public static MapWorldData get(Level level) {
        return INSTANCES.computeIfAbsent(level.dimension(), k -> new MapWorldData());
    }

    /** Clear all instances (used on world unload). */
    public static void clearAll() {
        INSTANCES.clear();
    }

    /** Load data for this level from disk. */
    public static void loadFor(Level level) {
        MapWorldData data = INSTANCES.get(level.dimension());
        if (data == null) {
            data = new MapWorldData();
            INSTANCES.put(level.dimension(), data);
        }
        data.loadFrom(level);
    }

    /** Save all dirty instances to disk. */
    public static void saveAll(Level level) {
        INSTANCES.values().forEach(data -> data.saveTo(level));
    }

    private Path dataFile(Level level) {
        return Path.of(System.getProperty("user.dir"), DATA_NAME + ".dat");
    }

    private void saveTo(Level level) {
        if (!dirty) return;
        try {
            CompoundTag tag = save(new CompoundTag(), level.registryAccess());
            CompoundTag outer = new CompoundTag();
            outer.put("data", tag);
            NbtUtils.addCurrentDataVersion(outer);
            Path file = dataFile(level);
            Files.createDirectories(file.getParent());
            NbtIo.writeCompressed(outer, file);
            dirty = false;
        } catch (Exception e) {
            LOGGER.warn("Failed to save map data", e);
        }
    }

    private void loadFrom(Level level) {
        Path file = dataFile(level);
        if (!Files.exists(file)) return;
        try {
            CompoundTag outer = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
            CompoundTag tag = outer.getCompound("data");

            ListTag chunksTag = tag.getList("ExploredChunks", 10);
            for (int i = 0; i < chunksTag.size(); i++) {
                CompoundTag entry = chunksTag.getCompound(i);
                int cx = entry.getInt("CX");
                int cz = entry.getInt("CZ");
                int height = entry.getInt("Height");
                long packed = pack(cx, cz);
                exploredChunks.add(packed);
                chunkHeights.put(packed, height);
            }

            ListTag drawsTag = tag.getList("Drawings", 10);
            for (int i = 0; i < drawsTag.size(); i++) {
                drawings.add(MapDraw.fromTag(drawsTag.getCompound(i)));
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to load map data", e);
        }
    }

    private CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag chunksTag = new ListTag();
        for (long packed : exploredChunks) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("CX", unpackX(packed));
            entry.putInt("CZ", unpackZ(packed));
            entry.putInt("Height", chunkHeights.getOrDefault(packed, 0));
            chunksTag.add(entry);
        }
        tag.put("ExploredChunks", chunksTag);

        ListTag drawsTag = new ListTag();
        for (MapDraw draw : drawings) {
            drawsTag.add(draw.toTag());
        }
        tag.put("Drawings", drawsTag);
        return tag;
    }

    /** Mark a chunk as explored, sampling its surface height. */
    public void markExplored(int chunkX, int chunkZ, int surfaceHeight) {
        long packed = pack(chunkX, chunkZ);
        if (!exploredChunks.contains(packed)) {
            exploredChunks.add(packed);
            chunkHeights.put(packed, surfaceHeight);
            dirty = true;
        }
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
        dirty = true;
    }

    public void removeDrawing(UUID id) {
        drawings.removeIf(d -> d.id().equals(id));
        dirty = true;
    }

    public void clearDrawings() {
        drawings.clear();
        dirty = true;
    }

    private static long pack(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    private static int unpackX(long packed) {
        return (int) (packed >> 32);
    }

    private static int unpackZ(long packed) {
        return (int) (packed & 0xFFFFFFFFL);
    }
}
