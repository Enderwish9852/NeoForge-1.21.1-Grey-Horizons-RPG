package net.enderwish.Belliarium_Monstrarium_Subpack.core.diary;

import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * BlockChangeLog
 *
 * position -> ORIGINAL block state from before the first change in the window (first wins).
 * NEW: beginRestore() returns a cursor that can be stepped under a time budget, so the
 * wake-up rewind can spread block restoration over several ticks.
 */
public class BlockChangeLog {

    private static final int RESTORE_FLAGS =
            Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;

    private final Long2IntOpenHashMap originalByPos = new Long2IntOpenHashMap();
    private final List<BlockState> palette = new ArrayList<>();
    private final Object2IntOpenHashMap<BlockState> paletteLookup = new Object2IntOpenHashMap<>();

    public BlockChangeLog() { paletteLookup.defaultReturnValue(-1); }

    public boolean recordIfAbsent(long packedPos, BlockState original) {
        if (originalByPos.containsKey(packedPos)) return false;
        int index = paletteLookup.getInt(original);
        if (index < 0) {
            index = palette.size();
            palette.add(original);
            paletteLookup.put(original, index);
        }
        originalByPos.put(packedPos, index);
        return true;
    }

    public int size() { return originalByPos.size(); }

    public void clear() {
        originalByPos.clear();
        palette.clear();
        paletteLookup.clear();
    }

    public RestoreCursor beginRestore() { return new RestoreCursor(); }

    public final class RestoreCursor {
        private final long[] positions;
        private final int[] indices;
        private int next = 0;
        private int restored = 0;

        private RestoreCursor() {
            positions = new long[originalByPos.size()];
            indices = new int[originalByPos.size()];
            int i = 0;
            for (Long2IntMap.Entry e : originalByPos.long2IntEntrySet()) {
                positions[i] = e.getLongKey();
                indices[i] = e.getIntValue();
                i++;
            }
        }

        /** Returns true when every block has been processed. Pass Long.MAX_VALUE for "no limit". */
        public boolean step(ServerLevel level, long deadlineNanos) {
            int counter = 0;
            while (next < positions.length) {
                int index = indices[next];
                long packed = positions[next];
                next++;

                if (index >= 0 && index < palette.size()) {
                    BlockState original = palette.get(index);
                    if (original != null) {
                        BlockPos pos = BlockPos.of(packed);
                        if (level.getBlockState(pos) != original && level.setBlock(pos, original, RESTORE_FLAGS)) {
                            restored++;
                        }
                    }
                }
                if ((++counter & 31) == 0 && System.nanoTime() >= deadlineNanos) return next >= positions.length;
            }
            return true;
        }

        public int restored() { return restored; }
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag paletteTag = new ListTag();
        for (BlockState state : palette) {
            Tag encoded = state == null
                    ? new CompoundTag()
                    : BlockState.CODEC.encodeStart(NbtOps.INSTANCE, state).result().orElseGet(CompoundTag::new);
            paletteTag.add(encoded);
        }
        long[] positions = new long[originalByPos.size()];
        int[] indices = new int[originalByPos.size()];
        int i = 0;
        for (Long2IntMap.Entry e : originalByPos.long2IntEntrySet()) {
            positions[i] = e.getLongKey();
            indices[i] = e.getIntValue();
            i++;
        }
        tag.put("palette", paletteTag);
        tag.putLongArray("positions", positions);
        tag.putIntArray("palette_indices", indices);
        return tag;
    }

    public static BlockChangeLog load(CompoundTag tag) {
        BlockChangeLog log = new BlockChangeLog();
        ListTag paletteTag = tag.getList("palette", Tag.TAG_COMPOUND);
        for (int i = 0; i < paletteTag.size(); i++) {
            BlockState state = BlockState.CODEC.parse(NbtOps.INSTANCE, paletteTag.getCompound(i)).result().orElse(null);
            log.palette.add(state);
            if (state != null) log.paletteLookup.put(state, i);
        }
        long[] positions = tag.getLongArray("positions");
        int[] indices = tag.getIntArray("palette_indices");
        int count = Math.min(positions.length, indices.length);
        for (int i = 0; i < count; i++) log.originalByPos.put(positions[i], indices[i]);
        return log;
    }
}
