package net.enderwish.Farming_Overhaul_Subpack.event;

import net.enderwish.Farming_Overhaul_Subpack.block.entity.GrowthNodeBlockEntity;
import net.enderwish.HUD_Visuals_Subpack.api.ClimateData;
import net.enderwish.HUD_Visuals_Subpack.core.ModAttachments;
import net.enderwish.HUD_Visuals_Subpack.event.SeasonChangeEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;

@EventBusSubscriber(modid = "gh_farming_overhaul")
public class SeasonGrowthListener {

    private static final TagKey<Block> IS_PERENNIAL = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath("gh_farming_overhaul", "is_perennial"));

    @SubscribeEvent
    public static void onSeasonChange(SeasonChangeEvent event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            // This is the most compatible way: Iterate players and check loaded chunks around them
            serverLevel.players().forEach(player -> {
                BlockPos playerPos = player.blockPosition();
                int chunkX = playerPos.getX() >> 4;
                int chunkZ = playerPos.getZ() >> 4;

                // Scan a 16x16 chunk area around every player
                for (int x = -8; x <= 8; x++) {
                    for (int z = -8; z <= 8; z++) {
                        LevelChunk chunk = serverLevel.getChunkSource().getChunk(chunkX + x, chunkZ + z, false);
                        if (chunk != null) {
                            for (BlockEntity be : chunk.getBlockEntities().values()) {
                                if (be instanceof GrowthNodeBlockEntity node) {
                                    node.advanceGrowth();
                                }
                            }
                        }
                    }
                }
            });
        }
    }

    @SubscribeEvent
    public static void onCropGrow(CropGrowEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        BlockState state = event.getState();

        if (state.is(IS_PERENNIAL)) {
            event.setResult(CropGrowEvent.Pre.Result.DO_NOT_GROW);
            return;
        }

        ClimateData data = level.getData(ModAttachments.CLIMATE);
        if (data != null && data.season().name().equalsIgnoreCase("WINTER")) {
            event.setResult(CropGrowEvent.Pre.Result.DO_NOT_GROW);
        }
    }
}