package net.enderwish.Belliarium_Monstrarium_Subpack.event;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;

/**
 * ArmorAttributeHandler
 *
 * Strips the ARMOR and ARMOR_TOUGHNESS attribute modifiers vanilla
 * bakes into every ArmorItem based on its material.
 */
@EventBusSubscriber(modid = BelliariumMonstrariumSubpack.MODID)
public class ArmorAttributeHandler {

    @SubscribeEvent
    public static void onGatherAttributes(ItemAttributeModifierEvent event) {
        if (!(event.getItemStack().getItem() instanceof ArmorItem)) return;
        event.removeAllModifiersFor(Attributes.ARMOR);
        event.removeAllModifiersFor(Attributes.ARMOR_TOUGHNESS);
    }
}
