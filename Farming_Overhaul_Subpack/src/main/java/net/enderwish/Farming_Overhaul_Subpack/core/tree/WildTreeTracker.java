package net.enderwish.Farming_Overhaul_Subpack.core.tree;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * WildTreeTracker — the thread bridge for worldgen tree tracking.
 *
 * Chunk decoration (including feature placement) runs on worker
 * threads, but TreeGrowthManager is SavedData — main-thread only.
 * CustomTreeFeature enqueues each wild tree here; WildTreeTrackHandler
 * drains the queue on the server tick, turning every wild tree into a
 * fully tracked one: seasons recolor it instantly and /treestage
 * works on it, while the wild flag keeps it from ever growing.
 */
public final class WildTreeTracker {

    /** A wild tree waiting to be registered on the main thread. */
    public record Pending(ResourceKey<Level> dimension, BlockPos origin,
                          String species, TreeStage stage, long seed,
                          long[] voxels) {}

    private static final Queue<Pending> QUEUE = new ConcurrentLinkedQueue<>();

    private WildTreeTracker() {}

    /** Called from feature placement (worker threads). Never touches level state. */
    public static void enqueue(ResourceKey<Level> dimension, BlockPos origin,
                               String species, TreeStage stage, long seed,
                               List<BlockPos> placed) {
        long[] voxels = new long[placed.size()];
        for (int i = 0; i < placed.size(); i++) {
            voxels[i] = placed.get(i).asLong();
        }
        QUEUE.add(new Pending(dimension, origin.immutable(), species,
                stage, seed, voxels));
    }

    /** Called once per server tick; registers up to 64 wild trees per tick. */
    public static void drain(MinecraftServer server) {
        int budget = 64;
        while (budget-- > 0) {
            Pending p = QUEUE.poll();
            if (p == null) return;
            ServerLevel level = server.getLevel(p.dimension());
            if (level == null) continue;   // dimension no longer exists
            TreeGrowthManager.get(level).trackWild(level, p.origin(),
                    p.species(), p.stage(), p.seed(), p.voxels());
        }
    }
}
