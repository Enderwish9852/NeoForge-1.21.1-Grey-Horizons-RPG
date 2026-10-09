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
 * Maps packed block position -> the ORIGINAL BlockState from before the first
 * change in the tracking window. Later changes to the same position are
 * ignored, so memory scales with distinct positions changed, not total changes.
 *
 * States are stored as palette indices, and the palette is saved as BlockState
 * NBT (names + properties), so saves survive other mods shifting numeric ids.
 *
 * VERIFY IF COMPILE FAILS -- BlockState.CODEC as the NBT codec for a state.
 */
public class BlockChangeLog {

    // Update clients, skip neighbour shape updates, suppress drops. Neighbours are
    // deliberately NOT notified, so restoring doesn't trigger physics cascades.
    private static final int RESTORE_FLAGS =
            Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;

    private final Long2IntOpenHashMap originalByPos = new Long2IntOpenHashMap();
    private final List<BlockState> palette = new ArrayList<>(); // null = state no longer exists
    private final Object2IntOpenHashMap<BlockState> paletteLookup = new Object2IntOpenHashMap<>();

    public BlockChangeLog() {
        paletteLookup.defaultReturnValue(-1);
    }

    /** Returns true if this position was newly recorded. */
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

    /** Puts every recorded position back to its original state. Returns how many blocks actually changed. */
    public int restoreAll(ServerLevel level) {
        int restored = 0;
        for (Long2IntMap.Entry entry : originalByPos.long2IntEntrySet()) {
            int index = entry.getIntValue();
            if (index < 0 || index >= palette.size()) continue;
            BlockState original = palette.get(index);
            if (original == null) continue;

            BlockPos pos = BlockPos.of(entry.getLongKey());
            if (level.getBlockState(pos) == original) continue;
            if (level.setBlock(pos, original, RESTORE_FLAGS)) restored++;
        }
        return restored;
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
        for (Long2IntMap.Entry entry : originalByPos.long2IntEntrySet()) {
            positions[i] = entry.getLongKey();
            indices[i] = entry.getIntValue();
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
        for (int i = 0; i < count; i++) {
            log.originalByPos.put(positions[i], indices[i]);
        }
        return log;
    }
}
