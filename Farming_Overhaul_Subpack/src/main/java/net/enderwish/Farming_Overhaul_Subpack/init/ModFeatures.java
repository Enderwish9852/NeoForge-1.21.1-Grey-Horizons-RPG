package net.enderwish.Farming_Overhaul_Subpack.init;

import net.enderwish.Farming_Overhaul_Subpack.FarmingOverhaulSubpack;
import net.enderwish.Farming_Overhaul_Subpack.core.tree.CustomTreeFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * ModFeatures — code-registered worldgen features. The single entry,
 * custom_tree, is configured per species in
 * data/gh_farming_overhaul/worldgen/configured_feature/&lt;species&gt;_tree.json
 * and placed per density in .../placed_feature/&lt;species&gt;_&lt;density&gt;.json,
 * then wired into TerraForma's biomes with neoforge:add_features biome
 * modifiers (see data/gh_farming_overhaul/neoforge/biome_modifier/).
 */
public class ModFeatures {

    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, FarmingOverhaulSubpack.MODID);

    public static final DeferredHolder<Feature<?>, CustomTreeFeature> CUSTOM_TREE =
            FEATURES.register("custom_tree", CustomTreeFeature::new);

    public static void register(IEventBus eventBus) {
        FEATURES.register(eventBus);
    }
}
