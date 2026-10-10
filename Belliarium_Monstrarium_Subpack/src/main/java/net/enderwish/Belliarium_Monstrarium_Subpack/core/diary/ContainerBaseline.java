package net.enderwish.Belliarium_Monstrarium_Subpack.core.diary;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * ContainerBaseline
 *
 * Full NBT snapshots of every block entity (chests, furnaces, barrels, clay pots,
 * cutting boards, signs...) as they were when tracking began, or when their chunk
 * was first seen afterwards. First snapshot per position wins.
 *
 * Restore REPLACES the block entity with a fresh one built from the snapshot,
 * rather than loading into the existing instance. Some of this mod's own block
 * entities (clay pot, cutting board) don't clear their item lists before loading,
 * so loading into an old instance would leave stale slots behind.
 *
 * NEW (slice 1d) -- containers in chunks that aren't loaded at rollback time are
 * handed to PendingFixData instead of being skipped, and applied via restoreOne()
 * when the chunk loads.
 */
public class ContainerBaseline {

    public record Result(int restored, int deferred) {}

    private final Map<Long, CompoundTag> snapshots = new HashMap<>();
    private final LongOpenHashSet capturedChunks = new LongOpenHashSet();

    public boolean isChunkCaptured(long chunkKey) {
        return capturedChunks.contains(chunkKey);
    }

    public void captureChunk(LevelChunk chunk, HolderLookup.Provider registries) {
        capturedChunks.add(chunk.getPos().toLong());
        for (BlockEntity be : new ArrayList<>(chunk.getBlockEntities().values())) {
            if (be.isRemoved()) continue;
            snapshots.putIfAbsent(be.getBlockPos().asLong(), be.saveWithFullMetadata(registries));
        }
    }

    public int size() { return snapshots.size(); }

    public void clear() {
        snapshots.clear();
        capturedChunks.clear();
    }

    public Result restoreAll(ServerLevel level, PendingFixData pending) {
        HolderLookup.Provider registries = level.registryAccess();
        int restored = 0;
        int deferred = 0;

        for (Map.Entry<Long, CompoundTag> entry : snapshots.entrySet()) {
            BlockPos pos = BlockPos.of(entry.getKey());
            int chunkX = pos.getX() >> 4;
            int chunkZ = pos.getZ() >> 4;

            if (level.getChunkSource().getChunkNow(chunkX, chunkZ) == null) {
                pending.addContainer(ChunkPos.asLong(chunkX, chunkZ), entry.getKey(), entry.getValue());
                deferred++;
                continue;
            }
            if (restoreOne(level, pos, entry.getValue(), registries)) restored++;
        }
        return new Result(restored, deferred);
    }

    /** Puts one container back from a snapshot. Returns true if it actually changed anything. */
    public static boolean restoreOne(ServerLevel level, BlockPos pos, CompoundTag tag,
                                     HolderLookup.Provider registries) {
        BlockEntity existing = level.getBlockEntity(pos);
        if (existing != null && existing.saveWithFullMetadata(registries).equals(tag)) {
            return false; // already identical
        }

        BlockState state = level.getBlockState(pos);
        BlockEntity fresh = BlockEntity.loadStatic(pos, state, tag, registries);
        if (fresh == null) return false; // block there no longer fits this block entity type

        level.setBlockEntity(fresh);
        level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
        return true;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (Map.Entry<Long, CompoundTag> entry : snapshots.entrySet()) {
            CompoundTag e = new CompoundTag();
            e.putLong("pos", entry.getKey());
            e.put("data", entry.getValue());
            list.add(e);
        }
        tag.put("snapshots", list);
        tag.putLongArray("captured_chunks", capturedChunks.toLongArray());
        return tag;
    }

    public static ContainerBaseline load(CompoundTag tag) {
        ContainerBaseline baseline = new ContainerBaseline();
        ListTag list = tag.getList("snapshots", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag e = list.getCompound(i);
            baseline.snapshots.put(e.getLong("pos"), e.getCompound("data"));
        }
        for (long key : tag.getLongArray("captured_chunks")) {
            baseline.capturedChunks.add(key);
        }
        return baseline;
    }
}
