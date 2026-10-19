package net.enderwish.Belliarium_Monstrarium_Subpack.event;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.gear.backpack.BackpackExtensionHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * BackpackExtensionTickHandler
 *
 * Server-side consistency pass for the backpack special-slot extension,
 * every 10 ticks while it's active (see BackpackExtensionHelper.
 * syncExtension): pushes in-place mutations of the virtual held stack
 * (durability, count) back into the live special slot, and exits the
 * extension when the held item was used up or the slot/bag changed out
 * from under it (backpack GUI, unequip).
 */
@EventBusSubscriber(modid = BelliariumMonstrariumSubpack.MODID)
public class BackpackExtensionTickHandler {

    private static final int SYNC_INTERVAL_TICKS = 10;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!BackpackExtensionHelper.isActive(player)) return;
        if (player.level().getGameTime() % SYNC_INTERVAL_TICKS != 0) return;

        BackpackExtensionHelper.syncExtension(player);
    }
}
