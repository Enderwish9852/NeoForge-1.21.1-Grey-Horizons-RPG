package net.enderwish.Belliarium_Monstrarium_Subpack.event;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.diary.DayStats;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.diary.DayTracker;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.diary.DayTrackerData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * DayStatsHandler
 *
 * Feeds the Survivor's Diary day summary (DiaryBook#buildSummary): item pickups,
 * block breaks and crafted/smelted items all count as "gathered" while a tracking
 * window is active for the overworld. A no-op whenever DayTracker isn't tracking.
 */
@EventBusSubscriber(modid = BelliariumMonstrariumSubpack.MODID)
public final class DayStatsHandler {
    private DayStatsHandler() {}

    @SubscribeEvent
    public static void onItemPickup(ItemEntityPickupEvent.Post event) {
        if (event.getPlayer() instanceof ServerPlayer player) add(player, event.getOriginalStack());
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel) || !(event.getPlayer() instanceof ServerPlayer player)) return;
        add(player, new ItemStack(event.getState().getBlock()));
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) add(player, event.getCrafting());
    }

    @SubscribeEvent
    public static void onItemSmelted(PlayerEvent.ItemSmeltedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) add(player, event.getSmelting());
    }

    private static void add(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty() || !(player.level() instanceof ServerLevel)) return;
        if (!DayTracker.isTracking()) return;

        DayTrackerData data = DayTracker.getActiveData();
        if (data == null || !player.getUUID().equals(data.getOwner())) return;

        DayStats stats = data.getStats();
        stats.addGathered(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), stack.getCount());
        data.setDirty();
    }
}
