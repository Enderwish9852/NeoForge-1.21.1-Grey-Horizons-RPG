package net.enderwish.HUD_Visuals_Subpack.item; // Matches your folder structure in the photo

import net.enderwish.HUD_Visuals_Subpack.HUDVisualsSubpack;
import net.enderwish.HUD_Visuals_Subpack.common.items.SportsWatchItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Updated to use the custom SportsWatchItem class.
 */
public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HUDVisualsSubpack.MOD_ID);

    // Change "new Item" to "new SportsWatchItem"
    public static final DeferredItem<SportsWatchItem> SPORTS_WATCH = ITEMS.register("sports_watch",
            () -> new SportsWatchItem(new Item.Properties().stacksTo(1)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}