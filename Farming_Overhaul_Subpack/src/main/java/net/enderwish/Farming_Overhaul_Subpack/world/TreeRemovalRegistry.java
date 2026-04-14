package net.enderwish.Farming_Overhaul_Subpack.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class TreeRemovalRegistry {
    public static final DeferredRegister<MapCodec<? extends BiomeModifier>> BIOME_MODIFIER_SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, "gh_farming_overhaul");

    public static final DeferredHolder<MapCodec<? extends BiomeModifier>, MapCodec<TreeRemovalModifier>> TREE_REMOVAL =
            BIOME_MODIFIER_SERIALIZERS.register("tree_removal", () -> RecordCodecBuilder.mapCodec(builder -> builder.group(
                    Biome.LIST_CODEC.fieldOf("biomes").forGetter(TreeRemovalModifier::biomes),
                    PlacedFeature.LIST_CODEC.fieldOf("features").forGetter(TreeRemovalModifier::features)
            ).apply(builder, TreeRemovalModifier::new)));
}