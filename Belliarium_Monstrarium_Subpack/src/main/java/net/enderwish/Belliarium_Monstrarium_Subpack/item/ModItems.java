package net.enderwish.Belliarium_Monstrarium_Subpack.item;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BelliariumMonstrariumSubpack.MODID);

    public static final DeferredItem<Item> SURVIVAL_WATCH = ITEMS.register("survival_watch",
            () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
