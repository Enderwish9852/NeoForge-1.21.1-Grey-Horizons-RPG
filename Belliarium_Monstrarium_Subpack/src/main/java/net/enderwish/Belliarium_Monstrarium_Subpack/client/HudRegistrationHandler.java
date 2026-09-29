package net.enderwish.Belliarium_Monstrarium_Subpack.client;

import net.enderwish.Belliarium_Monstrarium_Subpack.client.gui.BodyHealthHUD;
import net.enderwish.Belliarium_Monstrarium_Subpack.client.gui.CombatTimerHUD;
import net.enderwish.Belliarium_Monstrarium_Subpack.client.gui.SurvivalBarsHUD;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

public class HudRegistrationHandler {
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath("gh_belliarium_monstrarium", "body_health"),
                BodyHealthHUD.LAYER);
        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath("gh_belliarium_monstrarium", "survival_bars"),
                SurvivalBarsHUD.LAYER);
        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath("gh_belliarium_monstrarium", "combat_timer"),
                CombatTimerHUD.LAYER);
    }
}
