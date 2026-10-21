package net.enderwish.Belliarium_Monstrarium_Subpack.client;

import net.enderwish.Belliarium_Monstrarium_Subpack.client.gui.BodyHealthHUD;
import net.enderwish.Belliarium_Monstrarium_Subpack.client.gui.BackpackExtensionHud;
import net.enderwish.Belliarium_Monstrarium_Subpack.client.gui.CombatLogHUD;
import net.enderwish.Belliarium_Monstrarium_Subpack.client.gui.CombatTimerHUD;
import net.enderwish.Belliarium_Monstrarium_Subpack.client.gui.DejaVuOverlayHUD;
import net.enderwish.Belliarium_Monstrarium_Subpack.client.gui.SurvivalBarsHUD;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

public class HudRegistrationHandler {
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        // The body-health diagram renders one layer UNDER the F3 debug
        // overlay: registerAboveAll drew it on top of the debug text.
        event.registerBelow(VanillaGuiLayers.DEBUG_OVERLAY,
                ResourceLocation.fromNamespaceAndPath("gh_belliarium_monstrarium", "body_health"),
                BodyHealthHUD.LAYER);
        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath("gh_belliarium_monstrarium", "survival_bars"),
                SurvivalBarsHUD.LAYER);
        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath("gh_belliarium_monstrarium", "combat_timer"),
                CombatTimerHUD.LAYER);
        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath("gh_belliarium_monstrarium", "combat_log"),
                CombatLogHUD.LAYER);
        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath("gh_belliarium_monstrarium", "deja_vu_overlay"),
                DejaVuOverlayHUD.LAYER);
        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath("gh_belliarium_monstrarium", "backpack_extension"),
                BackpackExtensionHud.LAYER);
    }
}
