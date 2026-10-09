package net.enderwish.Belliarium_Monstrarium_Subpack.core.diary;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * DayTracker
 *
 * Static facade for the Survivor's Diary world tracking. Everything outside
 * core/diary should go through this class, never DayTrackerData directly.
 *
 * Overworld only, as this mod has no other reachable dimension.
 *
 * `current` is non-null ONLY while tracking is active, so the mixin's hot-path
 * check is a single volatile read. `owner` ties that state to one server instance,
 * so leaving a singleplayer world and opening another can't leak state between them.
 */
public final class DayTracker {
    private DayTracker() {}

    public static final long TRACKING_WINDOW_TICKS = 24000L;

    public enum ActivationResult { STARTED, ALREADY_ACTIVE, DISABLED_BY_CONFIG }

    private static volatile DayTrackerData current = null; // null = not tracking
    private static volatile boolean rollingBack = false;
    private static MinecraftServer owner = null;

    // ── Hot path (called from LevelSetBlockMixin for every setBlock) ──────────

    public static boolean isTracking() {
        return current != null && !rollingBack;
    }

    public static void record(ServerLevel level, BlockPos pos, BlockState newState) {
        DayTrackerData data = current;
        if (data == null || rollingBack || level.getServer() != owner) return;

        BlockState old = level.getBlockState(pos);
        if (old == newState) return; // no actual change

        if (data.getBlockLog().recordIfAbsent(pos.asLong(), old)) {
            data.setDirty();
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    public static ActivationResult activate(ServerLevel overworld) {
        ensureLoaded(overworld);
        if (!BelliariumConfig.WORLD_TRACKING_ENABLED.get()) return ActivationResult.DISABLED_BY_CONFIG;
        if (current != null) return ActivationResult.ALREADY_ACTIVE;

        DayTrackerData data = DayTrackerData.get(overworld);
        data.start(overworld.getGameTime());
        current = data;
        return ActivationResult.STARTED;
    }

    /** Ends tracking and discards the log WITHOUT restoring anything. */
    public static void deactivate(ServerLevel overworld) {
        ensureLoaded(overworld);
        DayTrackerData data = current;
        current = null;
        if (data != null) data.deactivate();
    }

    /**
     * Restores every tracked block, then ends tracking. Returns blocks restored,
     * or -1 if tracking wasn't active. Runs synchronously for now -- the real
     * "wake up" flow will spread this over several ticks under a black screen.
     */
    public static int rollbackBlocks(ServerLevel overworld) {
        ensureLoaded(overworld);
        DayTrackerData data = current;
        if (data == null) return -1;

        int restored;
        rollingBack = true; // stops the mixin logging our own restore writes
        try {
            restored = data.getBlockLog().restoreAll(overworld);
        } finally {
            rollingBack = false;
        }

        current = null;
        data.deactivate();
        return restored;
    }

    public static void tick(ServerLevel overworld) {
        ensureLoaded(overworld);
        DayTrackerData data = current;
        if (data == null) return;
        if (overworld.getGameTime() % 20 != 0) return;

        if (overworld.getGameTime() - data.getStartGameTime() >= TRACKING_WINDOW_TICKS) {
            current = null;
            data.deactivate();
        }
    }

    // ── Status (for the debug command and later UI) ───────────────────────────

    public static DayTrackerData getActiveData() { return current; }

    // ── Internals ─────────────────────────────────────────────────────────────

    private static void ensureLoaded(ServerLevel overworld) {
        MinecraftServer server = overworld.getServer();
        if (owner == server) return;
        owner = server;

        DayTrackerData data = DayTrackerData.get(overworld);
        if (data.isActive() && BelliariumConfig.WORLD_TRACKING_ENABLED.get()) {
            current = data;
        } else {
            current = null;
            if (data.isActive()) data.deactivate(); // config was switched off while a log existed
        }
    }
}
