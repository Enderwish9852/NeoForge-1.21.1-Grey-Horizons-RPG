package net.enderwish.Belliarium_Monstrarium_Subpack.client;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.client.gui.DeathReportScreen;
import net.enderwish.Belliarium_Monstrarium_Subpack.network.DeathReportPacket;
import net.minecraft.client.gui.screens.DeathScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * DeathReportScreenHandler
 *
 * Same ScreenEvent.Opening interception pattern already used by
 * FrontierScreenHandler for the custom title screen. If for any reason no
 * report data is cached, vanilla's DeathScreen opens normally as a fallback.
 */
@EventBusSubscriber(modid = BelliariumMonstrariumSubpack.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class DeathReportScreenHandler {

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (!(event.getScreen() instanceof DeathScreen)) return;

        DeathReportPacket data = DeathReportClientState.consumePending();
        if (data == null) return;

        event.setNewScreen(new DeathReportScreen(data));
    }
}
