package net.enderwish.Belliarium_Monstrarium_Subpack.core.integration;

import net.minecraft.world.entity.player.Player;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.concurrent.atomic.AtomicBoolean;

public final class CuriosHooks {

    private CuriosHooks() {}

    public static boolean hasWatchEquipped(Player player) {
        AtomicBoolean found = new AtomicBoolean(false);
        CuriosApi.getCuriosInventory(player).ifPresent(inventory ->
                inventory.getStacksHandler("wrist").ifPresent(handler -> {
                    for (int i = 0; i < handler.getStacks().getSlots(); i++) {
                        if (handler.getStacks().getStackInSlot(i).is(
                                net.enderwish.Belliarium_Monstrarium_Subpack.item.ModItems.SURVIVAL_WATCH.get())) {
                            found.set(true);
                            break;
                        }
                    }
                }));
        return found.get();
    }
}
