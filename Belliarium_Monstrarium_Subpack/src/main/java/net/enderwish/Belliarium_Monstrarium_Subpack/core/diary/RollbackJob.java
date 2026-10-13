package net.enderwish.Belliarium_Monstrarium_Subpack.core.diary;

import net.enderwish.Atmospheric_Overhaul_Subpack.api.SeasonsAPI;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * RollbackJob
 *
 * The rewind as a resumable state machine: blocks -> containers -> entities ->
 * environment -> player. step() works under a nanosecond deadline, so the wake-up can
 * run it a slice per tick under the black screen. Pass Long.MAX_VALUE to run to the end.
 * afterPlayerRestored runs right after the player snapshot is applied (and the player teleported).
 */
public final class RollbackJob {

    public record Report(int blocks, int containers, int containersDeferred,
                         int entitiesRestored, int entitiesRecreated,
                         int entitiesDiscarded, int entitiesDeferred,
                         boolean environmentRestored, boolean playerRestored) {}

    private enum Stage { BLOCKS, CONTAINERS, ENTITIES, ENVIRONMENT, PLAYER, DONE }

    private final ServerLevel level;
    private final DayTrackerData data;
    private final PendingFixData pending;
    private final Consumer<ServerPlayer> afterPlayerRestored;

    private Stage stage = Stage.BLOCKS;
    private BlockChangeLog.RestoreCursor blockCursor;
    private ContainerBaseline.Cursor containerCursor;
    private int blocks, containers, containersDeferred;
    private int entitiesRestored, entitiesRecreated, entitiesDiscarded, entitiesDeferred;
    private boolean environmentRestored, playerRestored;

    public RollbackJob(ServerLevel level, DayTrackerData data, PendingFixData pending,
                       Consumer<ServerPlayer> afterPlayerRestored) {
        this.level = level;
        this.data = data;
        this.pending = pending;
        this.afterPlayerRestored = afterPlayerRestored;
    }

    /** @return true when the whole rewind is finished. */
    public boolean step(long deadlineNanos) {
        while (stage != Stage.DONE) {
            switch (stage) {
                case BLOCKS -> {
                    if (blockCursor == null) blockCursor = data.getBlockLog().beginRestore();
                    if (!blockCursor.step(level, deadlineNanos)) return false;
                    blocks = blockCursor.restored();
                    stage = Stage.CONTAINERS;
                }
                case CONTAINERS -> {
                    if (containerCursor == null) containerCursor = data.getContainers().beginRestore(pending);
                    if (!containerCursor.step(level, deadlineNanos)) return false;
                    containers = containerCursor.restored();
                    containersDeferred = containerCursor.deferred();
                    stage = Stage.ENTITIES;
                }
                case ENTITIES -> {
                    restoreEntities();
                    stage = Stage.ENVIRONMENT;
                }
                case ENVIRONMENT -> {
                    if (!data.getEnvironment().isEmpty()) {
                        SeasonsAPI.restoreEnvironment(level, data.getEnvironment());
                        environmentRestored = true;
                    }
                    stage = Stage.PLAYER;
                }
                case PLAYER -> {
                    ServerPlayer player = data.getOwner() == null ? null
                            : level.getServer().getPlayerList().getPlayer(data.getOwner());
                    if (player != null && !data.getPlayerSnapshot().isEmpty()) {
                        PlayerSnapshot.restore(player, level, data.getPlayerSnapshot());
                        if (afterPlayerRestored != null) afterPlayerRestored.accept(player);
                        playerRestored = true;
                    }
                    stage = Stage.DONE;
                }
                default -> stage = Stage.DONE;
            }
            if (stage != Stage.DONE && System.nanoTime() >= deadlineNanos) return false;
        }
        return true;
    }

    public Report report() {
        return new Report(blocks, containers, containersDeferred, entitiesRestored, entitiesRecreated,
                entitiesDiscarded, entitiesDeferred, environmentRestored, playerRestored);
    }

    private void restoreEntities() {
        EntityBaseline entities = data.getEntities();

        for (UUID id : new ArrayList<>(entities.getSpawned())) {
            Entity live = level.getEntity(id);
            if (live != null) {
                live.discard();
                entitiesDiscarded++;
            } else {
                pending.addEntityDiscard(id);
                entitiesDeferred++;
            }
        }

        for (Map.Entry<UUID, CompoundTag> entry : new ArrayList<>(entities.getBaseline().entrySet())) {
            UUID id = entry.getKey();
            CompoundTag tag = entry.getValue();

            Entity live = level.getEntity(id);
            boolean wasDying = false;
            if (live instanceof LivingEntity living && living.isDeadOrDying()) {
                live.discard();
                live = null;
                wasDying = true;
            }

            if (live != null) {
                EntityRestorer.restoreInPlace(level, live, tag);
                entitiesRestored++;
            } else if (wasDying || entities.isDestroyed(id)) {
                if (EntityRestorer.isChunkLoadedAt(level, tag) && EntityRestorer.recreate(level, tag) != null) {
                    entitiesRecreated++;
                } else {
                    pending.addEntityRecreate(EntityRestorer.chunkKeyAt(tag), id, tag);
                    entitiesDeferred++;
                }
            } else {
                pending.addEntityRestore(id, tag);
                entitiesDeferred++;
            }
        }
    }
}
