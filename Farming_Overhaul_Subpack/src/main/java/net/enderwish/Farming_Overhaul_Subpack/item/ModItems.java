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

    // --- TREE FRUITS ---
    public static final DeferredItem<Item> APPLE = ITEMS.register("apple",
            () -> new Item(new Item.Properties().stacksTo(16)));

    public static final DeferredItem<Item> ORANGE = ITEMS.register("orange",
            () -> new Item(new Item.Properties().stacksTo(16)));

    public static final DeferredItem<Item> BANANA = ITEMS.register("banana",
            () -> new Item(new Item.Properties().stacksTo(16)));

    // --- SAPLINGS ---
    public static final DeferredItem<Item> OAK_ADAPTIVE_SAPLING = ITEMS.register("oak_adaptive_sapling",
            () -> new BlockItem(ModBlocks.OAK_ADAPTIVE_SAPLING.get(), new Item.Properties()));

    // THIS IS THE CRITICAL METHOD: It connects this file to your Main Mod Class
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}