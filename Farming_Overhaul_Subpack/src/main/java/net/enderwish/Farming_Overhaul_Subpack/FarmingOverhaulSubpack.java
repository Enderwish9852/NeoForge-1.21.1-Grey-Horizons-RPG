package net.enderwish.Farming_Overhaul_Subpack;

import net.enderwish.Farming_Overhaul_Subpack.block.ModBlocks;
import net.enderwish.Farming_Overhaul_Subpack.client.SpoilageTooltipHandler;
import net.enderwish.Farming_Overhaul_Subpack.core.crop.CropRegistry;
import net.enderwish.Farming_Overhaul_Subpack.core.food.FoodRegistry;
import net.enderwish.Farming_Overhaul_Subpack.core.spoilage.ModDataComponents;
import net.enderwish.Farming_Overhaul_Subpack.core.spoilage.SpoilageHandler;
import net.enderwish.Farming_Overhaul_Subpack.event.CropHarvestHandler;
import net.enderwish.Farming_Overhaul_Subpack.item.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
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
                        // Items will be added here as we register them
                    })
                    .build());

    public FarmingOverhaulSubpack(IEventBus modEventBus, ModContainer container) {
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModDataComponents.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        // Register CropRegistry as a resource reload listener
        // This fires every time datapacks reload (/reload command or world load)
        NeoForge.EVENT_BUS.addListener(FarmingOverhaulSubpack::onAddReloadListeners);
        NeoForge.EVENT_BUS.register(SpoilageHandler.class);
        NeoForge.EVENT_BUS.register(SpoilageTooltipHandler.class);
        NeoForge.EVENT_BUS.register(CropHarvestHandler.class);
    }

    private static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(CropRegistry.INSTANCE);
        event.addListener(FoodRegistry.INSTANCE);
    }
}