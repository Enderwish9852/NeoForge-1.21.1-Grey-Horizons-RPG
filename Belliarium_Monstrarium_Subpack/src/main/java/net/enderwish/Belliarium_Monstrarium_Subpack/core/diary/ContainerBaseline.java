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
 * Block entity snapshots, first snapshot per position wins. Restore replaces the block
 * entity with a fresh one built from the snapshot. Containers in unloaded chunks become
 * pending fixes. NEW: stepped restore via Cursor (time-budgeted).
 */
public class ContainerBaseline {

    private final Map<Long, CompoundTag> snapshots = new HashMap<>();
    private final LongOpenHashSet capturedChunks = new LongOpenHashSet();

    public boolean isChunkCaptured(long chunkKey) { return capturedChunks.contains(chunkKey); }

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

    public Cursor beginRestore(PendingFixData pending) { return new Cursor(pending); }

    public final class Cursor {
        private final long[] keys;
        private final CompoundTag[] tags;
        private final PendingFixData pending;
        private int next = 0, restored = 0, deferred = 0;

        private Cursor(PendingFixData pending) {
            this.pending = pending;
            keys = new long[snapshots.size()];
            tags = new CompoundTag[snapshots.size()];
            int i = 0;
            for (Map.Entry<Long, CompoundTag> e : snapshots.entrySet()) {
                keys[i] = e.getKey();
                tags[i] = e.getValue();
                i++;
            }
        }

        public boolean step(ServerLevel level, long deadlineNanos) {
            HolderLookup.Provider registries = level.registryAccess();
            int counter = 0;
            while (next < keys.length) {
                long key = keys[next];
                CompoundTag tag = tags[next];
                next++;

                BlockPos pos = BlockPos.of(key);
                int cx = pos.getX() >> 4, cz = pos.getZ() >> 4;
                if (level.getChunkSource().getChunkNow(cx, cz) == null) {
                    pending.addContainer(ChunkPos.asLong(cx, cz), key, tag);
                    deferred++;
                } else if (restoreOne(level, pos, tag, registries)) {
                    restored++;
                }
                if ((++counter & 15) == 0 && System.nanoTime() >= deadlineNanos) return next >= keys.length;
            }
            return true;
        }

        public int restored() { return restored; }
        public int deferred() { return deferred; }
    }

    public static boolean restoreOne(ServerLevel level, BlockPos pos, CompoundTag tag, HolderLookup.Provider registries) {
        BlockEntity existing = level.getBlockEntity(pos);
        if (existing != null && existing.saveWithFullMetadata(registries).equals(tag)) return false;

        BlockState state = level.getBlockState(pos);
        BlockEntity fresh = BlockEntity.loadStatic(pos, state, tag, registries);
        if (fresh == null) return false;

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
        for (long key : tag.getLongArray("captured_chunks")) baseline.capturedChunks.add(key);
        return baseline;
    }
}
