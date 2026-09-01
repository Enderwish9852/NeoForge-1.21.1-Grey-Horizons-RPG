package net.enderwish.TerraForma_Subpack.datagen;

import net.enderwish.TerraForma_Subpack.TerraFormaSubpack;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(
        modid = TerraFormaSubpack.MODID,
        bus = EventBusSubscriber.Bus.MOD)
public class TerraFormaDataGen {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator gen    = event.getGenerator();
        PackOutput output    = gen.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider =
                event.getLookupProvider();

        // ── Server side ───────────────────────────────────────────────────────
        gen.addProvider(event.includeServer(),
                new GHBiomeProvider(output, lookupProvider));
    }
}
