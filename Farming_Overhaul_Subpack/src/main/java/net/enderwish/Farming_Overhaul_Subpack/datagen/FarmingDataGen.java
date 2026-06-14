package net.enderwish.Farming_Overhaul_Subpack.datagen;

import net.enderwish.Farming_Overhaul_Subpack.FarmingOverhaulSubpack;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = FarmingOverhaulSubpack.MODID, bus = EventBusSubscriber.Bus.MOD)
public class FarmingDataGen {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator gen = event.getGenerator();
        PackOutput output = gen.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        // Client side
        gen.addProvider(event.includeClient(),
                new CropBlockStateProvider(output, event.getExistingFileHelper()));
        gen.addProvider(event.includeClient(),
                new CropItemModelProvider(output, event.getExistingFileHelper()));
        gen.addProvider(event.includeClient(),
                new CropLanguageProvider(output));

        // Server side
        gen.addProvider(event.includeServer(),
                new CropLootTableProvider(output, lookupProvider));
        gen.addProvider(event.includeServer(),
                new ClayPotRecipeProvider(output));
        gen.addProvider(event.includeServer(),
                new CuttingBoardRecipeProvider(output));

        gen.addProvider(event.includeServer(),
                new net.enderwish.Farming_Overhaul_Subpack.datagen.tags.OrganicCompostablesTagProvider(
                        output,
                        event.getLookupProvider(),
                        event.getExistingFileHelper()));
        gen.addProvider(event.includeServer(),
                new ClayPotRecipeProvider(output));
        gen.addProvider(event.includeServer(),
                new CuttingBoardRecipeProvider(output));
    }
}
