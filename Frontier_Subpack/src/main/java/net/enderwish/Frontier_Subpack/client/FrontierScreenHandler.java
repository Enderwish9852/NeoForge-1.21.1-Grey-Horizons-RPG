package net.enderwish.Frontier_Subpack.client;

import net.enderwish.Frontier_Subpack.FrontierSubpack;
import net.minecraft.client.gui.screens.TitleScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * FrontierScreenHandler
 *
 * Intercepts vanilla's TitleScreen opening and replaces it with
 * FrontierTitleScreen instead.
 *
 * TODO: gate this behind an "Adventure Mode enabled" config toggle once
 * that setting exists — right now it always replaces the title screen.
 */
@EventBusSubscriber(modid = FrontierSubpack.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class FrontierScreenHandler {

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (event.getScreen() instanceof TitleScreen) {
            event.setNewScreen(new FrontierTitleScreen());
        }
    }
}
