package net.enderwish.Belliarium_Monstrarium_Subpack.client;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * PocketSlotsClient
 *
 * NEW -- kills the hover/highlight ghosts over the painted-over 27-slot
 * "pockets" area of the survival inventory screen.
 *
 * HOW: your override texture (assets/minecraft/textures/gui/container/
 * inventory.png) paints the dead grid away, but the vanilla menu still
 * OWNS those 27 slots and draws a white hover quad whenever the mouse
 * crosses them. Slot x/y are FINAL (verified from 1.21 sources), so the
 * positions can't be mutated — instead each dead Slot OBJECT is replaced
 * in the client-side menu list with an identical-wrapper dummy parked at
 * -30000: same backing Container and container-slot index (reads stay
 * live, list positions/indices untouched so client<->server slot indices
 * still match), but no on-screen geometry — no hover, no highlight, no
 * item, no click target.
 *
 * Server-side menus are separate objects and are never touched.
 * Creative screen builds its own slots and is unaffected. Re-applied
 * whenever the client player (and with it inventoryMenu) is recreated —
 * respawn, dimension change, relog.
 */
@EventBusSubscriber(modid = BelliariumMonstrariumSubpack.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class PocketSlotsClient {

    /** First dead index = main-inventory grid start (InventoryMenu order:
     * 0 craft result, 1-4 craft grid, 5-8 armor, 9-35 MAIN GRID, 36-44
     * hotbar, 45 offhand — verified from InventoryMenu sources). */
    private static final int FIRST_DEAD = 9;
    private static final int LAST_DEAD = 35;

    private static final int OFFSCREEN = -30000;

    private static InventoryMenu lastMenu = null;

    public static class HiddenSlot extends Slot {
        HiddenSlot(Container container, int containerSlot) {
            super(container, containerSlot, OFFSCREEN, OFFSCREEN);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            lastMenu = null;
            return;
        }
        InventoryMenu menu = player.inventoryMenu;
        if (menu == lastMenu) return;
        lastMenu = menu;

        for (int i = FIRST_DEAD; i <= LAST_DEAD; i++) {
            Slot real = menu.getSlot(i);
            menu.slots.set(i, new HiddenSlot(
                    real.container, real.getContainerSlot()));
        }
    }
}
