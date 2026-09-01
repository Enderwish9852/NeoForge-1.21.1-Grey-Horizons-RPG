package net.enderwish.Farming_Overhaul_Subpack.event;

import net.enderwish.Farming_Overhaul_Subpack.FarmingOverhaulSubpack;
import net.enderwish.Farming_Overhaul_Subpack.block.tree.FruitTreeLeavesBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * FruitTreeSeasonHandler
 *
 * Listens for season changes from Atmospheric subpack
 * and updates all loaded FruitTreeLeavesBlock states.
 *
 * Strategy:
 *   Every ~60 seconds of game time, scan loaded chunks
 *   for FruitTreeLeavesBlock and update their SEASON_STAGE.
 *   This is cheaper than updating every tick and fast enough
 *   that the transition feels gradual rather than instant.
 *
 * On season change:
 *   All loaded leaf blocks update within ~1 in-game minute.
 *
 * TODO: Hook into Atmospheric season change event directly
 *       once Atmospheric exposes a season change event.
 *       For now: polls every 1200 ticks (60 seconds).
 */
@EventBusSubscriber(
        modid = FarmingOverhaulSubpack.MODID,
        bus = EventBusSubscriber.Bus.GAME)
public class FruitTreeSeasonHandler {

    // Check every 1200 ticks = 60 seconds
    private static final int CHECK_INTERVAL = 1200;

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (level.getGameTime() % CHECK_INTERVAL != 0) return;

        // Safe approach — iterate players and scan chunks around them
        level.players().forEach(player -> {
            BlockPos playerPos = player.blockPosition();
            int chunkX = playerPos.getX() >> 4;
            int chunkZ = playerPos.getZ() >> 4;

            // Scan chunks in 4 chunk radius around each player
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    net.minecraft.world.level.chunk.LevelChunk chunk =
                            level.getChunkSource().getChunkNow(
                                    chunkX + dx, chunkZ + dz);
                    if (chunk != null) {
                        updateLeavesInChunk(level, chunk);
                    }
                }
            }
        });
    }

    /**
     * Scans a chunk for FruitTreeLeavesBlock and updates season stage.
     */
    private static void updateLeavesInChunk(ServerLevel level,
                                            LevelChunk chunk) {
        // Scan Y range where trees exist
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = level.getMinBuildHeight();
                     y < level.getMaxBuildHeight(); y++) {

                    BlockPos pos = chunk.getPos().getBlockAt(x, y, z);
                    BlockState state = level.getBlockState(pos);

                    if (state.getBlock() instanceof FruitTreeLeavesBlock) {
                        FruitTreeLeavesBlock.updateSeasonStage(
                                level, pos, state);
                    }
                }
            }
        }
    }
}