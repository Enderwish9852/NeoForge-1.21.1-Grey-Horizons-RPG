package net.enderwish.Farming_Overhaul_Subpack.event;

import net.enderwish.HUD_Visuals_Subpack.api.ClimateData;
import net.enderwish.HUD_Visuals_Subpack.core.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;

public class SeasonGrowthListener {

    private static final TagKey<Block> IS_PERENNIAL = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath("gh_farming_overhaul", "is_perennial"));

    // We assume your Fruit Tree blocks will use an "AGE" or "STAGE" property 0-3
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 3);

    /**
     * EXISTING HOOK: Handles random-tick growth.
     * We use this to FREEZE growth in winter for normal crops.
     */
    @SubscribeEvent
    public static void onCropGrow(CropGrowEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        ClimateData data = level.getData(ModAttachments.CLIMATE);
        if (data == null) return;

        // In a "Seasonal Leap" system, we actually want to STOP random growth
        // for perennials entirely, because they only grow when the season flips.
        if (event.getState().is(IS_PERENNIAL)) {
            event.setResult(CropGrowEvent.Pre.Result.DO_NOT_GROW);
            return;
        }

        // Standard Winter Block for normal crops (Wheat, etc.)
        if (data.season().name().equalsIgnoreCase("WINTER")) {
            event.setResult(CropGrowEvent.Pre.Result.DO_NOT_GROW);
        }
    }

    /**
     * NEW UPDATE: Call this method from your Climate Controller
     * whenever the season integer changes in HUD_Visuals.
     */
    public static void performSeasonalGrowth(ServerLevel level) {
        // Scan around players (Optimization: only grow what is loaded)
        level.players().forEach(player -> {
            BlockPos center = player.blockPosition();
            int radius = 48; // Scavenging range

            BlockPos.betweenClosedStream(center.offset(-radius, -20, -radius), center.offset(radius, 20, radius))
                    .forEach(pos -> {
                        BlockState state = level.getBlockState(pos);

                        if (state.is(IS_PERENNIAL)) {
                            advanceTreeStage(level, pos, state);
                        }
                    });
        });
    }

    private static void advanceTreeStage(ServerLevel level, BlockPos pos, BlockState state) {
        // If the block has a STAGE property, increase it.
        if (state.hasProperty(STAGE)) {
            int currentStage = state.getValue(STAGE);

            if (currentStage < 3) {
                // Advance to next visual stage (Sapling -> Shrub -> Young Tree)
                level.setBlock(pos, state.setValue(STAGE, currentStage + 1), 3);
            } else {
                // If it's already Stage 3 and Year 1 hits, it becomes a Mature Tree
                // This is where you'd trigger your tree structure generation logic
            }
        }
    }
}