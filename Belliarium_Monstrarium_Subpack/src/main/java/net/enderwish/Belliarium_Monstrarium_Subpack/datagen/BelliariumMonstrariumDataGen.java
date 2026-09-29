package net.enderwish.Belliarium_Monstrarium_Subpack.datagen;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = BelliariumMonstrariumSubpack.MODID, bus = EventBusSubscriber.Bus.MOD)
public class BelliariumMonstrariumDataGen {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator gen = event.getGenerator();
        PackOutput output = gen.getPackOutput();
        gen.addProvider(event.includeServer(), new ItemWeightProvider(output));
    }
}
