package net.enderwish.Frontier_Subpack.client;

import net.enderwish.Frontier_Subpack.FrontierConfig;
import net.enderwish.Frontier_Subpack.FrontierSubpack;
import net.minecraft.client.gui.screens.TitleScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = FrontierSubpack.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class FrontierScreenHandler {

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (!FrontierConfig.ADVENTURE_MODE_ENABLED.get()) return;
        if (event.getScreen() instanceof TitleScreen) {
            event.setNewScreen(new FrontierTitleScreen());
        }
    }
}
