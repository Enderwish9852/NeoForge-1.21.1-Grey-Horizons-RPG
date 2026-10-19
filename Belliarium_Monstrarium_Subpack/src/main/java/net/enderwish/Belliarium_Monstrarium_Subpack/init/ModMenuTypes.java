package net.enderwish.Belliarium_Monstrarium_Subpack.init;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.gui.BackpackMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * ModMenuTypes
 *
 * ONE shared MenuType for every backpack tier — the per-tier differences
 * (slot count, special slots, layout) arrive in the menu-open buffer as a
 * full BackpackConfig, so a single factory works for all four bags. Same
 * IMenuTypeExtension pattern as the Farming subpack's menus.
 */
public class ModMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, BelliariumMonstrariumSubpack.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<BackpackMenu>> BACKPACK =
            MENUS.register("backpack", () ->
                    IMenuTypeExtension.create(
                            (windowId, inv, buf) -> new BackpackMenu(windowId, inv, buf)
                    ));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
