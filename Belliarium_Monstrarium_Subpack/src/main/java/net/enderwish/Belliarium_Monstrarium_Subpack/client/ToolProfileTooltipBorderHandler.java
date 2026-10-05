package net.enderwish.Belliarium_Monstrarium_Subpack.client;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.gear.ToolProfileRegistry;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;

/**
 * ToolProfileTooltipBorderHandler
 *
 * NEW -- the actual colored border/outline from your tooltip spec, kept
 * separate from ToolProfileTooltipHandler since it hooks a completely
 * different event (this one touches the panel's border rendering, not
 * text lines).
 *
 * VERIFY IF COMPILE FAILS -- this is the one piece of this whole batch I'm
 * genuinely unsure of. RenderTooltipEvent.Color is a long-standing Forge/
 * NeoForge event made for exactly this purpose, but I don't have 1.21.1
 * source confirming its exact method names (setBorderStart/setBorderEnd
 * vs. something else, or whether getItemStack() even lives directly on
 * this event). If it fails, ctrl/cmd-click RenderTooltipEvent.Color in
 * IntelliJ and paste me what's actually there -- fastest path to a
 * working version.
 */
@EventBusSubscriber(modid = BelliariumMonstrariumSubpack.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ToolProfileTooltipBorderHandler {

    @SubscribeEvent
    public static void onTooltipColor(RenderTooltipEvent.Color event) {
        ItemStack stack = event.getItemStack();
        String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM
                .getKey(stack.getItem()).toString();

        ToolProfileRegistry.INSTANCE.getProfile(itemId).ifPresent(profile -> {
            int color = profile.rarity().getColor();
            event.setBorderStart(color);
            event.setBorderEnd(color);
        });
    }
}