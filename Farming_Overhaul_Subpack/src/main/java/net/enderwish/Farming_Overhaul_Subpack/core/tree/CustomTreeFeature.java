package net.enderwish.Farming_Overhaul_Subpack.core.tree;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * CustomTreeFeature — the worldgen entry for the voxel tree system.
 *
 * Configured per species in
 * data/gh_farming_overhaul/worldgen/configured_feature/&lt;species&gt;_tree.json
 * and wired into TerraForma biomes through neoforge:add_features biome
 * modifiers, so wild trees generate naturally during terrain generation.
 *
 * Each tree rolls a private seed (its shape identity, so /treestage
 * replays it) and a growth stage: mostly MATURE with a configurable
 * slice of SEMI_MATURE, giving forests natural size variety. Those
 * ages are final — wild trees never grow on their own (the wild flag
 * in TreeGrowthManager; only their leaves recolor with the seasons).
 *
 * Ground rule: a sturdy top face under the trunk (dirt, grass, sand,
 * mud, terracotta, ice — anything TerraForma's palettes can produce),
 * reached through snow layers; nothing grows over liquids, powder
 * snow or air.
 */
public class CustomTreeFeature extends Feature<CustomTreeFeature.Config> {

    public record Config(String species, float semiMatureChance)
            implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                        Codec.STRING.fieldOf("species")
                                .forGetter(Config::species),
                        Codec.FLOAT.fieldOf("semi_mature_chance").orElse(0.15f)
                                .forGetter(Config::semiMatureChance)
                ).apply(instance, Config::new));
    }

    public CustomTreeFeature() {
        super(Config.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        Config config = context.config();

        Optional<TreeDefinition> def =
                TreeRegistry.INSTANCE.getBySpecies(config.species());
        if (def.isEmpty()) return false;

        // decorations may only write inside the feature write margin;
        // bail out cleanly instead of throwing on the margin edge
        if (!level.ensureCanWrite(origin)) return false;

        // ── ground: sturdy top face, reached through snow layers ──────────
        BlockPos groundPos = origin.below();
        BlockState ground = level.getBlockState(groundPos);
        int descend = 0;
        while ((ground.is(Blocks.SNOW) || ground.isAir()) && descend++ < 8) {
            groundPos = groundPos.below();
            ground = level.getBlockState(groundPos);
        }
        if (!ground.isFaceSturdy(level, groundPos, Direction.UP)) return false;

        BlockState atOrigin = level.getBlockState(origin);
        if (!atOrigin.isAir() && !atOrigin.canBeReplaced()) return false;

        // ── identity: one seed for life, mixed ages for variety ────────────
        long seed = context.random().nextLong();
        TreeStage stage = context.random().nextFloat() < config.semiMatureChance()
                ? TreeStage.SEMI_MATURE : TreeStage.MATURE;

        List<BlockPos> placed = new ArrayList<>();
        boolean ok = TreeGenerator.generate(level, origin, def.get(),
                RandomSource.create(seed), true, stage, placed);
        if (ok && !placed.isEmpty()) {
            // decoration runs on worker threads — SavedData is main-thread
            // only, so the tracking record is queued for the server tick
            WildTreeTracker.enqueue(level.getLevel().dimension(), origin,
                    config.species(), stage, seed, placed);
        }
        return ok;
    }
}
