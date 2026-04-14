package net.enderwish.Farming_Overhaul_Subpack;

import net.enderwish.Farming_Overhaul_Subpack.block.ModBlocks;
import net.enderwish.Farming_Overhaul_Subpack.event.SeasonGrowthListener;
import net.enderwish.Farming_Overhaul_Subpack.event.VanillaTreeOverhaulListener;
import net.enderwish.Farming_Overhaul_Subpack.init.ModBlockEntities;
import net.enderwish.Farming_Overhaul_Subpack.world.TreeRemovalRegistry;
import net.enderwish.Farming_Overhaul_Subpack.item.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(FarmingOverhaulSubpack.MODID)
public class FarmingOverhaulSubpack {
    public static final String MODID = "gh_farming_overhaul";

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> FARMING_TAB =
            CREATIVE_MODE_TABS.register("farming_overhaul_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("creativetab.farming_overhaul_tab")) // Name in en_us.json
                    .icon(() -> new ItemStack(ModItems.OAK_ADAPTIVE_SAPLING.get())) // The tab icon
                    .displayItems((parameters, output) -> {
                        // All your custom items go here!
                        output.accept(ModItems.OAK_ADAPTIVE_SAPLING.get());
                        output.accept(ModItems.APPLE.get());
                        output.accept(ModItems.ORANGE.get());
                        output.accept(ModItems.BANANA.get());
                    })
                    .build());

    public FarmingOverhaulSubpack(IEventBus modEventBus, ModContainer container) {

        // 1. Register our custom Branch Blocks
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);

        // 2. Register Global Event Listeners
        // This watches for sapling growth and climate changes
        NeoForge.EVENT_BUS.register(SeasonGrowthListener.class);
        NeoForge.EVENT_BUS.register(VanillaTreeOverhaulListener.class);

        TreeRemovalRegistry.BIOME_MODIFIER_SERIALIZERS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}