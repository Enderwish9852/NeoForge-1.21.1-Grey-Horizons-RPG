package net.enderwish.Belliarium_Monstrarium_Subpack.client;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.item.ModItems;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * Registers the GeckoLib first-person renderer for the bandage roll.
 *
 * MISSING REGISTRATION was the "flat sprite bobbing" bug: the geo model
 * class existed but nothing ever replaced the vanilla item renderer.
 *
 * RegisterClientExtensionsEvent is a MOD-BUS event (verified from the
 * NeoForge 1.21.1 jar: registerItem(IClientItemExtensions, Item...)) and
 * fires after registries are bound, so BANDAGE.get() is safe here.
 */
@EventBusSubscriber(modid = BelliariumMonstrariumSubpack.MODID,
        bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class BandageItemRenderRegistration {

    @SubscribeEvent
    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            @Override
            public GeoItemRenderer<?> getCustomRenderer() {
                return new GeoItemRenderer<>(new BandageRollModel());
            }
        }, ModItems.BANDAGE.get());
    }

    private BandageItemRenderRegistration() {}
}
