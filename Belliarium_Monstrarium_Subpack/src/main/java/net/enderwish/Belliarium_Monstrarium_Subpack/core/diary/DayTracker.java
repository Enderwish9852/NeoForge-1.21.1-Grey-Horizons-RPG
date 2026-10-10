package net.enderwish.Belliarium_Monstrarium_Subpack.core.diary;

import net.enderwish.Atmospheric_Overhaul_Subpack.api.SeasonsAPI;
import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DayTracker
 *
 * Static facade for the Survivor's Diary world tracking. Everything outside
 * core/diary should go through this class.
 *
 * Rollback order matters: blocks first (so frames/paintings have walls again and
 * chests exist), then containers, then entities, then the environment (time,
 * weather, season day), and the PLAYER LAST so you land in an already-rewound world.
 *
 * Entities: "joined as a new entity while near a player" = spawned during the day.
 * Entities that join with no player within WORLDGEN_EXEMPT_DISTANCE are treated as
 * chunk-generation population and left alone, so a rollback doesn't permanently
 * delete the starting animals/villagers of newly generated terrain.
 *
 * NEW (slice 1d) -- anything in a chunk that isn't loaded at rollback time becomes
 * a saved pending fix (PendingFixData) instead of being skipped:
 *   - containers and recreated entities: applied by a poll every 10 ticks once
 *     their chunk is loaded
 *   - entity resets and removals: applied one tick after the entity joins from
 *     disk (the join event can fire before its chunk is fully ready, so nothing
 *     is touched inside the event itself)
 * While pending fixes are being applied, `rollingBack` suppresses tracking. If a
 * new tracking window is open at that moment, the fixed entity is adopted into its
 * baseline, so a later rollback doesn't undo an earlier one.
 */
public final class DayTracker {
    private DayTracker() {}

    public static final long TRACKING_WINDOW_TICKS = 24000L;
    private static final double WORLDGEN_EXEMPT_DISTANCE = 128.0;

    public enum ActivationResult { STARTED, ALREADY_ACTIVE, DISABLED_BY_CONFIG }

    public record RollbackReport(int blocks, int containers, int containersDeferred,
                                 int entitiesRestored, int entitiesRecreated,
                                 int entitiesDiscarded, int entitiesDeferred,
                                 boolean environmentRestored, boolean playerRestored) {}

    private record EntityResult(int restored, int recreated, int discarded, int deferred) {}

    private static volatile DayTrackerData current = null; // null = not tracking
    private static volatile boolean rollingBack = false;   // also true while pending fixes are applied
    private static MinecraftServer owner = null;
    private static PendingFixData pending = null;
    private static final List<Entity> queuedEntityFixes = new ArrayList<>();

    // ── Hot path (called from LevelSetBlockMixin for every setBlock) ──────────

    public static boolean isTracking() {
        return current != null && !rollingBack;
    }

    public static void record(ServerLevel level, BlockPos pos, BlockState newState) {
        DayTrackerData data = current;
        if (data == null || rollingBack || level.getServer() != owner) return;

        BlockState old = level.getBlockState(pos);
        if (old == newState) return;

        if (data.getBlockLog().recordIfAbsent(pos.asLong(), old)) {
            data.setDirty();
        }
    }

    // ── Entity events (called from DayTrackerHandler) ─────────────────────────

    public static void onEntityJoin(ServerLevel level, Entity entity, boolean loadedFromDisk) {
        if (entity instanceof Player) return;
        if (!level.getServer().isSameThread()) return;
        ensureLoaded(level);
        if (rollingBack) return;

        // A fix left over from an earlier rollback takes priority; it's applied next tick.
        if (loadedFromDisk && pending.hasEntityFix(entity.getUUID())) {
            queuedEntityFixes.add(entity);
            return;
        }

        DayTrackerData data = current;
        if (data == null) return;

        EntityBaseline entities = data.getEntities();
        UUID id = entity.getUUID();
        if (entities.knows(id)) return;

        if (loadedFromDisk) {
            if (captureEntity(entity, entities)) data.setDirty();
        } else if (level.getNearestPlayer(entity, WORLDGEN_EXEMPT_DISTANCE) != null) {
            if (entities.markSpawned(id)) data.setDirty();
        }
    }

    public static void onEntityLeave(ServerLevel level, Entity entity) {
        if (entity instanceof Player) return;
        if (!level.getServer().isSameThread()) return;
        ensureLoaded(level);

        DayTrackerData data = current;
        if (data == null || rollingBack) return;

        Entity.RemovalReason reason = entity.getRemovalReason();
        if (reason == null || !reason.shouldDestroy()) return; // unloading is not destruction

        if (data.getEntities().onDestroyed(entity.getUUID())) data.setDirty();
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /** Starts the tracking window for this player: snapshots the player, the environment, containers and entities. */
    public static ActivationResult activate(ServerLevel overworld, ServerPlayer player) {
        ensureLoaded(overworld);
        if (!BelliariumConfig.WORLD_TRACKING_ENABLED.get()) return ActivationResult.DISABLED_BY_CONFIG;
        if (current != null) return ActivationResult.ALREADY_ACTIVE;

        DayTrackerData data = DayTrackerData.get(overworld);
        data.begin(overworld.getGameTime(), player.getUUID(),
                PlayerSnapshot.capture(player), SeasonsAPI.captureEnvironment(overworld));

        captureNearbyChunks(overworld, data);
        for (Entity entity : overworld.getAllEntities()) {
            captureEntity(entity, data.getEntities());
        }
        data.setDirty();

        current = data;
        return ActivationResult.STARTED;
    }

    /** Ends tracking and discards the logs WITHOUT restoring anything. */
    public static void deactivate(ServerLevel overworld) {
        ensureLoaded(overworld);
        DayTrackerData data = current;
        current = null;
        if (data != null) data.deactivate();
    }

    /** Rewinds the world, environment and player, and ends tracking. Returns null if not tracking. */
    public static RollbackReport rollback(ServerLevel overworld) {
        ensureLoaded(overworld);
        DayTrackerData data = current;
        if (data == null) return null;

        int blocks;
        ContainerBaseline.Result containerResult;
        EntityResult entityResult;
        boolean environmentRestored = false;
        boolean playerRestored = false;

        rollingBack = true; // stops our own restore writes from being logged
        try {
            blocks = data.getBlockLog().restoreAll(overworld);
            containerResult = data.getContainers().restoreAll(overworld, pending);
            entityResult = restoreEntities(overworld, data.getEntities());

            if (!data.getEnvironment().isEmpty()) {
                SeasonsAPI.restoreEnvironment(overworld, data.getEnvironment());
                environmentRestored = true;
            }

            ServerPlayer player = data.getOwner() == null
                    ? null
                    : overworld.getServer().getPlayerList().getPlayer(data.getOwner());
            if (player != null && !data.getPlayerSnapshot().isEmpty()) {
                PlayerSnapshot.restore(player, overworld, data.getPlayerSnapshot());
                playerRestored = true;
            }
        } finally {
            rollingBack = false;
        }

        current = null;
        data.deactivate();

        return new RollbackReport(blocks, containerResult.restored(), containerResult.deferred(),
                entityResult.restored(), entityResult.recreated(),
                entityResult.discarded(), entityResult.deferred(),
                environmentRestored, playerRestored);
    }

    public static void tick(ServerLevel overworld) {
        ensureLoaded(overworld);
        long now = overworld.getGameTime();

        // Pending fixes first, so a chunk's containers are fixed BEFORE a new window captures them.
        if (!queuedEntityFixes.isEmpty()) applyQueuedEntityFixes(overworld);
        if (now % 10 == 0 && !pending.isEmpty()) applyPendingChunkFixes(overworld);

        DayTrackerData data = current;
        if (data == null) return;

        // Containers in chunks that load after activation get their baseline here.
        if (now % 10 == 0) captureNearbyChunks(overworld, data);

        if (now % 20 == 0 && now - data.getStartGameTime() >= TRACKING_WINDOW_TICKS) {
            current = null;
            data.deactivate();
        }
    }

    public static DayTrackerData getActiveData() { return current; }

    // ── Entity restore (immediate part of a rollback) ─────────────────────────

    private static EntityResult restoreEntities(ServerLevel level, EntityBaseline entities) {
        int restored = 0, recreated = 0, discarded = 0, deferred = 0;

        // 1. Remove everything that appeared during the window.
        for (UUID id : new ArrayList<>(entities.getSpawned())) {
            Entity live = level.getEntity(id);
            if (live != null) {
                live.discard();
                discarded++;
            } else {
                pending.addEntityDiscard(id); // sitting in an unloaded chunk
                deferred++;
            }
        }

        // 2. Restore or recreate entities that existed at the start.
        for (Map.Entry<UUID, CompoundTag> entry : new ArrayList<>(entities.getBaseline().entrySet())) {
            UUID id = entry.getKey();
            CompoundTag tag = entry.getValue();

            Entity live = level.getEntity(id);
            boolean wasDying = false;
            if (live instanceof LivingEntity living && living.isDeadOrDying()) {
                // Mid death animation: remove it so a clean copy can take its UUID.
                live.discard();
                live = null;
                wasDying = true;
            }

            if (live != null) {
                EntityRestorer.restoreInPlace(level, live, tag);
                restored++;
            } else if (wasDying || entities.isDestroyed(id)) {
                if (EntityRestorer.isChunkLoadedAt(level, tag) && EntityRestorer.recreate(level, tag) != null) {
                    recreated++;
                } else {
                    pending.addEntityRecreate(EntityRestorer.chunkKeyAt(tag), id, tag);
                    deferred++;
                }
            } else {
                pending.addEntityRestore(id, tag); // alive but in an unloaded chunk
                deferred++;
            }
        }

        return new EntityResult(restored, recreated, discarded, deferred);
    }

    // ── Deferred fixes (applied later, from tick) ─────────────────────────────

    private static void applyQueuedEntityFixes(ServerLevel level) {
        List<Entity> batch = new ArrayList<>(queuedEntityFixes);
        queuedEntityFixes.clear();

        rollingBack = true;
        try {
            for (Entity entity : batch) {
                if (entity.isRemoved()) continue; // unloaded again before we got to it; the fix stays pending

                UUID id = entity.getUUID();
                if (pending.takeEntityDiscard(id)) {
                    entity.discard();
                    continue;
                }
                CompoundTag baseline = pending.takeEntityRestore(id);
                if (baseline != null) {
                    EntityRestorer.restoreInPlace(level, entity, baseline);
                    adoptIntoActiveWindow(entity);
                }
            }
        } finally {
            rollingBack = false;
        }
    }

    private static void applyPendingChunkFixes(ServerLevel level) {
        HolderLookup.Provider registries = level.registryAccess();

        rollingBack = true;
        try {
            for (long chunkKey : pending.containerChunkKeys()) {
                if (!isChunkLoaded(level, chunkKey)) continue;
                for (Map.Entry<Long, CompoundTag> e : pending.removeContainerChunk(chunkKey).entrySet()) {
                    ContainerBaseline.restoreOne(level, BlockPos.of(e.getKey()), e.getValue(), registries);
                }
            }

            for (long chunkKey : pending.recreateChunkKeys()) {
                if (!isChunkLoaded(level, chunkKey)) continue;
                for (Map.Entry<UUID, CompoundTag> e : pending.removeEntityRecreateChunk(chunkKey).entrySet()) {
                    Entity created = EntityRestorer.recreate(level, e.getValue());
                    if (created != null) adoptIntoActiveWindow(created);
                }
            }
        } finally {
            rollingBack = false;
        }
    }

    private static boolean isChunkLoaded(ServerLevel level, long chunkKey) {
        int chunkX = (int) chunkKey;          // low 32 bits, same packing as ChunkPos#toLong
        int chunkZ = (int) (chunkKey >>> 32);
        return level.getChunkSource().getChunkNow(chunkX, chunkZ) != null;
    }

    /** If a new tracking window is open, a fixed entity counts as part of its baseline. */
    private static void adoptIntoActiveWindow(Entity entity) {
        DayTrackerData data = current;
        if (data != null && captureEntity(entity, data.getEntities())) data.setDirty();
    }

    // ── Capture helpers ───────────────────────────────────────────────────────

    private static boolean captureEntity(Entity entity, EntityBaseline entities) {
        if (entity instanceof Player || entity.isRemoved()) return false;
        CompoundTag tag = new CompoundTag();
        if (!entity.save(tag)) return false; // passengers are saved with their vehicle
        return entities.putBaseline(entity.getUUID(), tag);
    }

    private static void captureNearbyChunks(ServerLevel level, DayTrackerData data) {
        int radius = level.getServer().getPlayerList().getViewDistance();
        HolderLookup.Provider registries = level.registryAccess();
        ContainerBaseline containers = data.getContainers();
        boolean any = false;

        for (ServerPlayer player : level.players()) {
            ChunkPos center = player.chunkPosition();
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int chunkX = center.x + dx;
                    int chunkZ = center.z + dz;
                    if (containers.isChunkCaptured(ChunkPos.asLong(chunkX, chunkZ))) continue;

                    LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                    if (chunk == null) continue;

                    containers.captureChunk(chunk, registries);
                    any = true;
                }
            }
        }
        if (any) data.setDirty();
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    private static void ensureLoaded(ServerLevel overworld) {
        MinecraftServer server = overworld.getServer();
        if (owner == server) return;
        owner = server;
        queuedEntityFixes.clear();
        pending = PendingFixData.get(overworld);

        DayTrackerData data = DayTrackerData.get(overworld);
        if (data.isActive() && BelliariumConfig.WORLD_TRACKING_ENABLED.get()) {
            current = data;
        } else {
            current = null;
            if (data.isActive()) data.deactivate();
        }
    }
}
