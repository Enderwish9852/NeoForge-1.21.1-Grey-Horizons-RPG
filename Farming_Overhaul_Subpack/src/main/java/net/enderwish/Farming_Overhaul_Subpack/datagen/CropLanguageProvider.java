package net.enderwish.Farming_Overhaul_Subpack.datagen;

import net.enderwish.Farming_Overhaul_Subpack.FarmingOverhaulSubpack;
import net.enderwish.Farming_Overhaul_Subpack.block.ModBlocks;
import net.enderwish.Farming_Overhaul_Subpack.item.ModItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class CropLanguageProvider extends LanguageProvider {

    public CropLanguageProvider(PackOutput output) {
        super(output, FarmingOverhaulSubpack.MODID, "en_us");
    }

    @Override
    protected void addTranslations() {
        // Blocks


        // Veg items
        add(ModItems.AUBERGINE.get(),    "Aubergine");
        add(ModItems.BELL_PEPPER.get(),  "Bell Pepper");
        add(ModItems.BROCCOLI.get(),     "Broccoli");
        add(ModItems.CABBAGE.get(),      "Cabbage");
        add(ModItems.CORN.get(),         "Corn");
        add(ModItems.COURGETTE.get(),    "Courgette");
        add(ModItems.CUCUMBER.get(),     "Cucumber");
        add(ModItems.GARLIC.get(),       "Garlic");
        add(ModItems.LETTUCE.get(),      "Lettuce");
        add(ModItems.ONION.get(),        "Onion");
        add(ModItems.SWEET_POTATO.get(), "Sweet Potato");
        add(ModItems.TOMATO.get(),       "Tomato");

        // Fruit items
        add(ModItems.BANANA.get(),      "Banana");
        add(ModItems.FIG.get(),         "Fig");
        add(ModItems.GUAVA.get(),       "Guava");
        add(ModItems.KIWI.get(),        "Kiwi");
        add(ModItems.MANGO.get(),       "Mango");
        add(ModItems.ORANGE.get(),      "Orange");
        add(ModItems.PAPAYA.get(),      "Papaya");
        add(ModItems.PEACH.get(),       "Peach");
        add(ModItems.PEAR.get(),        "Pear");
        add(ModItems.PERSIMMON.get(),   "Persimmon");
        add(ModItems.PLUM.get(),        "Plum");
        add(ModItems.POMEGRANATE.get(), "Pomegranate");

        // Misc
        add(ModItems.ROTTEN_SCRAP.get(), "Rotten Scrap");

        // Seeds
        add(ModItems.AUBERGINE_SEEDS.get(),    "Aubergine Seeds");
        add(ModItems.BELL_PEPPER_SEEDS.get(),  "Bell Pepper Seeds");
        add(ModItems.BROCCOLI_SEEDS.get(),     "Broccoli Seeds");
        add(ModItems.CABBAGE_SEEDS.get(),      "Cabbage Seeds");
        add(ModItems.CORN_SEEDS.get(),         "Corn Seeds");
        add(ModItems.COURGETTE_SEEDS.get(),    "Courgette Seeds");
        add(ModItems.CUCUMBER_SEEDS.get(),     "Cucumber Seeds");
        add(ModItems.LETTUCE_SEEDS.get(),      "Lettuce Seeds");
        add(ModItems.TOMATO_SEEDS.get(),       "Tomato Seeds");
        add(ModItems.FIG_SEEDS.get(),          "Fig Seeds");
        add(ModItems.GUAVA_SEEDS.get(),        "Guava Seeds");
        add(ModItems.KIWI_SEEDS.get(),         "Kiwi Seeds");
        add(ModItems.MANGO_SEEDS.get(),        "Mango Seeds");
        add(ModItems.ORANGE_SEEDS.get(),       "Orange Seeds");
        add(ModItems.PAPAYA_SEEDS.get(),       "Papaya Seeds");
        add(ModItems.PEACH_SEEDS.get(),        "Peach Seeds");
        add(ModItems.PEAR_SEEDS.get(),         "Pear Seeds");
        add(ModItems.PERSIMMON_SEEDS.get(),    "Persimmon Seeds");
        add(ModItems.PLUM_SEEDS.get(),         "Plum Seeds");
        add(ModItems.POMEGRANATE_SEEDS.get(),  "Pomegranate Seeds");

        // Container

    }
}
