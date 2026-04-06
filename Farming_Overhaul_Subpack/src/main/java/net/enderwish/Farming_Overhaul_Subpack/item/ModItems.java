package net.enderwish.Farming_Overhaul_Subpack.item;

import net.enderwish.Farming_Overhaul_Subpack.FarmingOverhaulSubpack;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(FarmingOverhaulSubpack.MODID);

    // Tree Fruits (Year-round growth for the tree, seasonal for the fruit)
    public static final DeferredItem<Item> APPLE = ITEMS.register("apple",
            () -> new Item(new Item.Properties().stacksTo(16))); // Weight: 0.20kg, Spoilage: 7 Days

    public static final DeferredItem<Item> ORANGE = ITEMS.register("orange",
            () -> new Item(new Item.Properties().stacksTo(16))); // Weight: 0.15kg, Spoilage: 10 Days

    public static final DeferredItem<Item> BANANA = ITEMS.register("banana",
            () -> new Item(new Item.Properties().stacksTo(16))); // Weight: 0.15kg, Spoilage: 5 Days
}