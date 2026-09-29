package net.enderwish.Belliarium_Monstrarium_Subpack.client;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * HideVanillaHealthHandler
 *
 * Confirmed via search this round: RegisterGuiLayersEvent is ADD-ONLY in
 * current NeoForge (a NeoForge GitHub issue explicitly states this) -- the
 * correct mechanism to SUPPRESS a vanilla layer is RenderGuiLayerEvent.Pre,
 * checking which layer is about to render and cancelling it.
 *
 * VERIFY IF COMPILE FAILS -- VanillaGuiLayers.PLAYER_HEALTH is my best-
 * confidence name for the health layer's identifier based on that pattern,
 * not something I've seen confirmed directly in this conversation. If the
 * exact constant name differs, IntelliJ's autocomplete on
 * "VanillaGuiLayers." will show the real one immediately.
 */
@EventBusSubscriber(modid = BelliariumMonstrariumSubpack.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class HideVanillaHealthHandler {

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Pre event) {
        if (event.getName().equals(VanillaGuiLayers.PLAYER_HEALTH)) {
            event.setCanceled(true);
        }
    }
}
