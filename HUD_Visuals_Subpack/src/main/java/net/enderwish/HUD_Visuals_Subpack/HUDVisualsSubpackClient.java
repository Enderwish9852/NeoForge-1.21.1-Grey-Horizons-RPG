package net.enderwish.HUD_Visuals_Subpack;

import net.enderwish.HUD_Visuals_Subpack.client.gui.SportsWatchHUD;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Handles the "Client-Side" setup for the HUD.
 * Updated for NeoForge 1.21.1: 'bus' is inferred from RegisterGuiLayersEvent.
 */
@EventBusSubscriber(modid = HUDVisualsSubpack.MOD_ID, value = Dist.CLIENT)
public class HUDVisualsSubpackClient {

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        // Registering the HUD layer above the Hotbar
        event.registerAbove(
                VanillaGuiLayers.HOTBAR,
                ResourceLocation.fromNamespaceAndPath(HUDVisualsSubpack.MOD_ID, "sports_watch_layer"),
                SportsWatchHUD.SPORTS_WATCH_ELEMENT
        );
    }
}