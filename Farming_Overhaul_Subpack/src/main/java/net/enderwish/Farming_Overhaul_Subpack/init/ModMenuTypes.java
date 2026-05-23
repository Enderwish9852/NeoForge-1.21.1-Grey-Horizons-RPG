package net.enderwish.Farming_Overhaul_Subpack.init;

import net.enderwish.Farming_Overhaul_Subpack.FarmingOverhaulSubpack;
import net.enderwish.Farming_Overhaul_Subpack.gui.ClayPotMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, FarmingOverhaulSubpack.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<ClayPotMenu>> CLAY_POT =
            MENUS.register("clay_pot", () ->
                    IMenuTypeExtension.create(
                            (windowId, inv, buf) -> new ClayPotMenu(windowId, inv, buf)
                    ));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
