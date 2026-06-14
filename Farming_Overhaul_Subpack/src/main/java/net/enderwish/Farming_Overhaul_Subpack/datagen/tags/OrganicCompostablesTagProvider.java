package net.enderwish.Farming_Overhaul_Subpack.datagen.tags;

import net.enderwish.Farming_Overhaul_Subpack.item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

public class OrganicCompostablesTagProvider extends ItemTagsProvider {

    private static final TagKey<Item> ORGANIC_COMPOSTABLES =
            TagKey.create(
                    Registries.ITEM,
                    ResourceLocation.fromNamespaceAndPath(
                            "gh_farming_overhaul", "organic_compostables"));

    public OrganicCompostablesTagProvider(PackOutput output,
                                          CompletableFuture<HolderLookup.Provider> lookupProvider,
                                          ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider,
                java.util.concurrent.CompletableFuture.completedFuture(
                        net.minecraft.data.tags.TagsProvider.TagLookup.empty()),
                "gh_farming_overhaul",
                existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(ORGANIC_COMPOSTABLES)
                // ── Custom veg crops (have spoil data) ───────────────────────
                .add(ModItems.AUBERGINE.get())
                .add(ModItems.BELL_PEPPER.get())
                .add(ModItems.BROCCOLI.get())
                .add(ModItems.CABBAGE.get())
                .add(ModItems.CORN.get())
                .add(ModItems.COURGETTE.get())
                .add(ModItems.CUCUMBER.get())
                .add(ModItems.GARLIC.get())
                .add(ModItems.LETTUCE.get())
                .add(ModItems.ONION.get())
                .add(ModItems.SWEET_POTATO.get())
                .add(ModItems.TOMATO.get());
        // TODO: add fruit crops, vanilla organic items, and prep items
        //       once spoil data is added for them
    }
}
