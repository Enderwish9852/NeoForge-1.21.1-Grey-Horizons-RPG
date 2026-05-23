package net.enderwish.Farming_Overhaul_Subpack.item;

import net.enderwish.Farming_Overhaul_Subpack.FarmingOverhaulSubpack;
import net.enderwish.Farming_Overhaul_Subpack.block.ModBlocks;
import net.minecraft.world.item.BlockItem;
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

    // ── Clay Pot ──────────────────────────────────────────────────────────────

    public static final DeferredItem<Item> WET_CLAY_POT = ITEMS.register("wet_clay_pot",
            () -> new BlockItem(ModBlocks.WET_CLAY_POT.get(),
                    new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> CLAY_POT = ITEMS.register("clay_pot",
            () -> new BlockItem(ModBlocks.CLAY_POT.get(),
                    new Item.Properties().stacksTo(1)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}