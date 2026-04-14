package net.enderwish.Farming_Overhaul_Subpack.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;

public record TreeRemovalModifier(HolderSet<Biome> biomes, HolderSet<PlacedFeature> features) implements BiomeModifier {

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase == Phase.REMOVE) {
            builder.getGenerationSettings().getFeatures(GenerationStep.Decoration.VEGETAL_DECORATION).removeIf(placedFeatureHolder ->
                    placedFeatureHolder.unwrapKey().map(key -> {
                        String namespace = key.location().getNamespace();
                        String path = key.location().getPath().toLowerCase();

                        // 1. Cancel anything from the "minecraft" namespace that looks like a tree
                        // 2. Cancel anything with the keyword "tree" regardless of who made it
                        return namespace.equals("minecraft") || path.contains("tree") || path.contains("forest");
                    }).orElse(false)
            );
        }
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() {
        return TreeRemovalRegistry.TREE_REMOVAL.get();
    }
}