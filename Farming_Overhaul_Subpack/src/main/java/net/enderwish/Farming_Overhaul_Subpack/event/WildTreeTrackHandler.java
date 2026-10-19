package net.enderwish.Farming_Overhaul_Subpack.event;

import net.enderwish.Farming_Overhaul_Subpack.core.tree.WildTreeTracker;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * WildTreeTrackHandler — drains the wild-tree queue once per server
 * tick (budget 64/tick). CustomTreeFeature enqueues on chunk-decoration
 * worker threads; SavedData (TreeGrowthManager) is main-thread only.
 */
public final class WildTreeTrackHandler {

    private WildTreeTrackHandler() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        WildTreeTracker.drain(event.getServer());
    }
}
