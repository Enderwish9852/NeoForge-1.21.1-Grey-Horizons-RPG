package net.enderwish.Belliarium_Monstrarium_Subpack.event;

import net.enderwish.Belliarium_Monstrarium_Subpack.core.gear.backpack.BackpackHandler;
import net.enderwish.Belliarium_Monstrarium_Subpack.item.gear.BackpackItem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * PocketsHandler
 *
 * "Pockets" rule: the player only carries the HOTBAR (plus armor/offhand).
 * Slots 9-35 of the vanilla inventory are non-functional and anything that
 * lands there (chest shift-click, crafting overflow, /give) is relocated
 * twice a second: into the equipped backpack if it fits, otherwise dropped
 * at the player's feet. Creative keeps its full vanilla inventory.
 */
@EventBusSubscriber(modid = BelliariumMonstrariumSubpack.MODID)
public class PocketsHandler {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)) return;
        if (player.isCreative()) return; // full vanilla inventory in creative
        if (player.isDeadOrDying()) return;
        if (player.level().getGameTime() % 10L != 0L) return;

        Inventory inv = player.getInventory();
        ItemStack backpack = BackpackHandler.getEquipped(player);
        BackpackItem item = backpack.getItem() instanceof BackpackItem bp ? bp : null;

        for (int i = 9; i < 36; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;

            inv.setItem(i, ItemStack.EMPTY);
            if (item != null && item.hasRoomFor(backpack, stack)) {
                item.insert(backpack, stack);
            } else {
                player.drop(stack, false);
            }
        }
    }
}
