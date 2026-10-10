package net.enderwish.Belliarium_Monstrarium_Subpack.core.diary;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;

import java.util.UUID;

/**
 * EntityRestorer
 *
 * Shared by the immediate rollback and the deferred (chunk-load) fixes, so both
 * behave identically. All methods expect to run on the server thread.
 */
public final class EntityRestorer {
    private EntityRestorer() {}

    /**
     * Resets a live entity to its baseline NBT. If the baseline position's chunk
     * isn't loaded, the entity keeps its current position instead of being
     * teleported into an unloaded chunk.
     */
    public static void restoreInPlace(ServerLevel level, Entity entity, CompoundTag baseline) {
        CompoundTag tag = baseline.copy();
        if (!isChunkLoadedAt(level, baseline)) {
            ListTag pos = new ListTag();
            pos.add(DoubleTag.valueOf(entity.getX()));
            pos.add(DoubleTag.valueOf(entity.getY()));
            pos.add(DoubleTag.valueOf(entity.getZ()));
            tag.put("Pos", pos);
        }
        UUID id = entity.getUUID();
        entity.load(tag);
        entity.setUUID(id); // same safeguard the /data command uses
    }

    /** Spawns a fresh copy from NBT (with passengers). Returns null if it couldn't be added. */
    public static Entity recreate(ServerLevel level, CompoundTag tag) {
        Entity entity = EntityType.loadEntityRecursive(tag, level, e -> e);
        if (entity == null) return null;
        return level.tryAddFreshEntityWithPassengers(entity) ? entity : null;
    }

    public static boolean isChunkLoadedAt(ServerLevel level, CompoundTag entityTag) {
        ListTag pos = entityTag.getList("Pos", Tag.TAG_DOUBLE);
        if (pos.size() < 3) return false;
        return level.getChunkSource().getChunkNow(Mth.floor(pos.getDouble(0)) >> 4, Mth.floor(pos.getDouble(2)) >> 4) != null;
    }

    /** Packed chunk key of the entity's saved position, same format as ChunkPos#toLong. */
    public static long chunkKeyAt(CompoundTag entityTag) {
        ListTag pos = entityTag.getList("Pos", Tag.TAG_DOUBLE);
        if (pos.size() < 3) return 0L;
        return ChunkPos.asLong(Mth.floor(pos.getDouble(0)) >> 4, Mth.floor(pos.getDouble(2)) >> 4);
    }
}
