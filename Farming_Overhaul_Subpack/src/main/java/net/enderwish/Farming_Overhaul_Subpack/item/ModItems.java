package net.enderwish.Farming_Overhaul_Subpack.item;

import net.enderwish.Farming_Overhaul_Subpack.FarmingOverhaulSubpack;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(FarmingOverhaulSubpack.MODID);

    // ── Placeholder fruits (will be replaced by full crop system) ─────────────
    public static final DeferredItem<Item> APPLE = ITEMS.register("apple",
            () -> new Item(new Item.Properties().stacksTo(16)));

    public static final DeferredItem<Item> ORANGE = ITEMS.register("orange",
            () -> new Item(new Item.Properties().stacksTo(16)));

    public static final DeferredItem<Item> BANANA = ITEMS.register("banana",
            () -> new Item(new Item.Properties().stacksTo(16)));

    public static final DeferredItem<Item> ROTTEN_SCRAP = ITEMS.register("rotten_scrap",
            () -> new Item(new Item.Properties().stacksTo(64)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}