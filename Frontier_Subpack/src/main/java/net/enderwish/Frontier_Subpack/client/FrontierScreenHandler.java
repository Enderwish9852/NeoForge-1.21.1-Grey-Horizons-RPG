package net.enderwish.Frontier_Subpack.client;

import net.enderwish.Frontier_Subpack.FrontierConfig;
import net.enderwish.Frontier_Subpack.FrontierSubpack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * VERIFY IF COMPILE FAILS — ScreenEvent.Init.Post's exact widget-
 * adding method. Using event.addListener(Button), matching the
 * addEventWidget-consumer pattern seen in Screen.java's own init(),
 * but I haven't seen ScreenEvent.Init.Post's class definition itself.
 */
@EventBusSubscriber(modid = FrontierSubpack.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class FrontierScreenHandler {

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (!(event.getScreen() instanceof TitleScreen)) return;
        if (!FrontierConfig.ADVENTURE_MODE_ENABLED.get()) return;
        event.setNewScreen(new FrontierTitleScreen());
    }

    @SubscribeEvent
    public static void onTitleScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof TitleScreen)) return;
        if (FrontierConfig.ADVENTURE_MODE_ENABLED.get()) return;

        Button button = Button.builder(
                Component.literal("Return to Grey Horizons"),
                b -> {
                    FrontierConfig.ADVENTURE_MODE_ENABLED.set(true);
                    FrontierConfig.ADVENTURE_MODE_ENABLED.save();
                    Minecraft.getInstance().setScreen(new FrontierTitleScreen());
                }
        ).bounds(event.getScreen().width - 160, event.getScreen().height - 30, 150, 20).build();

        event.addListener(button);
    }
}
