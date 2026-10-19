package net.enderwish.Belliarium_Monstrarium_Subpack.client;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.gear.backpack.BackpackExtensionHelper;
import net.enderwish.Belliarium_Monstrarium_Subpack.network.BackpackExtensionSelectPacket;
import net.enderwish.Belliarium_Monstrarium_Subpack.network.ModMessages;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;

/**
 * BackpackExtensionClient
 *
 * Input + safety side of the backpack special-slot hotbar extension
 * (Unit C):
 *
 *  - Scroll: past hotbar slot 0 (positive deltaY) enters the LEFT
 *    extension; past slot 8 (negative deltaY) enters the RIGHT one; any
 *    scroll while an extension is held exits back to the nearest real
 *    slot. Sign matches vanilla swapPaint (positive deltaY moves toward
 *    lower slot index). Cancelling the NeoForge scroll event fully
 *    suppresses the vanilla hotbar scroll (MouseHandler.onScroll checks
 *    ClientHooks.onMouseScroll first).
 *
 *  - Keys: LEFT / RIGHT arrow quick-slot keybinds (ModKeyMappings) select
 *    or exit an extension from the tick handler.
 *
 *  - Safety clears every tick: a screen open, a hotbar selection change,
 *    or the active special slot emptying (item used up / broken) all exit
 *    the extension.
 *
 * Every local state change is mirrored to the server via
 * BackpackExtensionSelectPacket; the attachment itself (set locally for
 * prediction) is the single source of the client's own behavior.
 */
@EventBusSubscriber(modid = BelliariumMonstrariumSubpack.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class BackpackExtensionClient {

    private static int lastSelectedSlot = -1;

    @SubscribeEvent
    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.screen != null) return;

        double delta = event.getScrollDeltaY();
        if (delta == 0.0) return;

        if (BackpackExtensionHelper.isActive(player)) {
            // any scroll exits to the nearest real slot
            player.getInventory().selected =
                    BackpackExtensionHelper.side(player) == BackpackExtensionHelper.LEFT ? 0 : 8;
            exit(player);
            event.setCanceled(true);
            return;
        }

        int selected = player.getInventory().selected;
        if (selected == 0 && delta > 0.0
                && BackpackExtensionHelper.isAvailable(player, BackpackExtensionHelper.LEFT)) {
            select(player, BackpackExtensionHelper.LEFT);
            event.setCanceled(true);
        } else if (selected == 8 && delta < 0.0
                && BackpackExtensionHelper.isAvailable(player, BackpackExtensionHelper.RIGHT)) {
            select(player, BackpackExtensionHelper.RIGHT);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            lastSelectedSlot = -1;
            return;
        }

        if (mc.screen != null) {
            exitIfActive(player);
            return;
        }

        // hotbar selection changed (number keys / pick block, etc.) -> exit
        if (player.getInventory().selected != lastSelectedSlot) {
            lastSelectedSlot = player.getInventory().selected;
            exitIfActive(player);
        }

        // the active extension's special slot emptied (item used/broke) -> exit
        if (BackpackExtensionHelper.isActive(player)
                && BackpackExtensionHelper.getExtensionStack(player).isEmpty()) {
            exit(player);
        }

        // LEFT / RIGHT arrow quick-slot keybinds
        while (ModKeyMappings.QUICK_SLOT_LEFT.consumeClick()) {
            if (BackpackExtensionHelper.isAvailable(player, BackpackExtensionHelper.LEFT)) {
                select(player, BackpackExtensionHelper.LEFT);
            } else if (BackpackExtensionHelper.side(player) == BackpackExtensionHelper.LEFT) {
                player.getInventory().selected = 0;
                exit(player);
            }
        }
        while (ModKeyMappings.QUICK_SLOT_RIGHT.consumeClick()) {
            if (BackpackExtensionHelper.isAvailable(player, BackpackExtensionHelper.RIGHT)) {
                select(player, BackpackExtensionHelper.RIGHT);
            } else if (BackpackExtensionHelper.side(player) == BackpackExtensionHelper.RIGHT) {
                player.getInventory().selected = 8;
                exit(player);
            }
        }
    }

    private static void select(LocalPlayer player, byte side) {
        BackpackExtensionHelper.select(player, side);
        ModMessages.sendToServer(new BackpackExtensionSelectPacket(side));
    }

    private static void exit(LocalPlayer player) {
        BackpackExtensionHelper.clear(player);
        ModMessages.sendToServer(new BackpackExtensionSelectPacket(BackpackExtensionHelper.NONE));
    }

    private static void exitIfActive(LocalPlayer player) {
        if (BackpackExtensionHelper.isActive(player)) {
            exit(player);
        }
    }
}
