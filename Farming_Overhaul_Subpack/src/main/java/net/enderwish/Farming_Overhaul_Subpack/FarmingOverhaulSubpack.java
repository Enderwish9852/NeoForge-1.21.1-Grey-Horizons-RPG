package net.enderwish.Farming_Overhaul_Subpack;

import net.enderwish.Farming_Overhaul_Subpack.block.ModBlocks;
import net.enderwish.Farming_Overhaul_Subpack.client.ClayPotScreen;
import net.enderwish.Farming_Overhaul_Subpack.client.SpoilageTooltipHandler;
import net.enderwish.Farming_Overhaul_Subpack.core.claypot.ClayPotRecipeRegistry;
import net.enderwish.Farming_Overhaul_Subpack.core.crop.CropRegistry;
import net.enderwish.Farming_Overhaul_Subpack.core.food.FoodRegistry;
import net.enderwish.Farming_Overhaul_Subpack.core.spoilage.ModDataComponents;
import net.enderwish.Farming_Overhaul_Subpack.core.spoilage.SpoilageHandler;
import net.enderwish.Farming_Overhaul_Subpack.event.CookingSpoilageHandler;
import net.enderwish.Farming_Overhaul_Subpack.event.CraftingSpoilageHandler;
import net.enderwish.Farming_Overhaul_Subpack.event.CropHarvestHandler;
import net.enderwish.Farming_Overhaul_Subpack.event.FoodPickupHandler;
import net.enderwish.Farming_Overhaul_Subpack.init.ModBlockEntities;
import net.enderwish.Farming_Overhaul_Subpack.init.ModMenuTypes;
import net.enderwish.Farming_Overhaul_Subpack.item.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(FarmingOverhaulSubpack.MODID)
public class FarmingOverhaulSubpack {
    public static final String MODID = "gh_farming_overhaul";

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> FARMING_TAB =
            CREATIVE_MODE_TABS.register("farming_overhaul_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("creativetab.gh_farming_overhaul.tab"))
                    .icon(() -> new ItemStack(ModItems.APPLE.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.APPLE.get());
                        output.accept(ModItems.ORANGE.get());
                        output.accept(ModItems.BANANA.get());
                        output.accept(ModItems.ROTTEN_SCRAP.get());
                        output.accept(ModItems.WET_CLAY_POT.get());
                        output.accept(ModItems.CLAY_POT.get());
                    })
                    .build());

    public FarmingOverhaulSubpack(IEventBus modEventBus, ModContainer container) {
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModDataComponents.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModMenuTypes.register(modEventBus);

        modEventBus.addListener(FarmingOverhaulSubpack::onClientReloadListeners);
        modEventBus.addListener(FarmingOverhaulSubpack::onRegisterScreens);

        NeoForge.EVENT_BUS.addListener(FarmingOverhaulSubpack::onAddReloadListeners);
        NeoForge.EVENT_BUS.register(SpoilageHandler.class);
        NeoForge.EVENT_BUS.register(SpoilageTooltipHandler.class);
        NeoForge.EVENT_BUS.register(CropHarvestHandler.class);
        NeoForge.EVENT_BUS.register(FoodPickupHandler.class);
        NeoForge.EVENT_BUS.register(CraftingSpoilageHandler.class);
        NeoForge.EVENT_BUS.register(CookingSpoilageHandler.class);
    }

    private static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.CLAY_POT.get(), ClayPotScreen::new);
    }

    private static void onClientReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(CropRegistry.INSTANCE);
        event.registerReloadListener(FoodRegistry.INSTANCE);
        event.registerReloadListener(ClayPotRecipeRegistry.INSTANCE);
    }

    private static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(CropRegistry.INSTANCE);
        event.addListener(FoodRegistry.INSTANCE);
        event.addListener(ClayPotRecipeRegistry.INSTANCE);
    }
}