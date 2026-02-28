package net.enderwish.HUD_Visuals_Subpack;

import net.enderwish.HUD_Visuals_Subpack.client.gui.SportsWatchHUD;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Handles the "Client-Side" setup for the HUD.
 */
public class ExampleModClient {

    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(
                VanillaGuiLayers.HOTBAR,
                ResourceLocation.fromNamespaceAndPath(HUDVisualsSubpack.MOD_ID, "sports_watch_layer"),
                SportsWatchHUD.SPORTS_WATCH_ELEMENT
        );


    }
}