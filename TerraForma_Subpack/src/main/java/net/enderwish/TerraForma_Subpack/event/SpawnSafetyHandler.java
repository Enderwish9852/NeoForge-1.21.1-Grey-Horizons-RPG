package net.enderwish.TerraForma_Subpack.event;

import net.enderwish.TerraForma_Subpack.TerraFormaSubpack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * SpawnSafetyHandler
 *
 * Ensures players never spawn in water. When a player spawns (new world,
 * respawn, or /spawn), checks if they're in a water block and, if so,
 * searches outward in a spiral for the nearest land position and moves
 * them there.
 */
@EventBusSubscriber(modid = TerraFormaSubpack.MODID, bus = EventBusSubscriber.Bus.GAME)
public class SpawnSafetyHandler {

    private static final int MAX_SEARCH_RADIUS = 200;

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        var player = event.getEntity();
        var level = player.level();
        if (level.isClientSide) return;

        BlockPos pos = player.blockPosition();
        BlockState state = level.getBlockState(pos);

        // If not in water, nothing to do
        if (!isWater(state)) return;

        // Search outward in a spiral for land
        BlockPos landPos = findNearestLand(level, pos, MAX_SEARCH_RADIUS);
        if (landPos != null) {
            // Place player on top of the land block, slightly above to avoid suffocation
            player.moveTo(
                landPos.getX() + 0.5,
                landPos.getY() + 1.0,
                landPos.getZ() + 0.5,
                player.getYRot(),
                player.getXRot()
            );
            player.setDeltaMovement(0, 0, 0);
        }
    }

    private static boolean isWater(BlockState state) {
        return state.is(Blocks.WATER) || state.is(Blocks.FROSTED_ICE);
    }

    private static BlockPos findNearestLand(Level level, BlockPos start, int maxRadius) {
        for (int radius = 1; radius <= maxRadius; radius++) {
            BlockPos result = spiralSearch(level, start, radius);
            if (result != null) return result;
        }
        return null;
    }

    private static BlockPos spiralSearch(Level level, BlockPos center, int radius) {
        int x = center.getX();
        int z = center.getZ();
        int y = center.getY();

        // Search a square ring around the center at this radius
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                // Only check the outer ring (max(|dx|, |dz|) == radius)
                if (Math.abs(dx) != radius && Math.abs(dz) != radius) continue;

                BlockPos checkPos = new BlockPos(x + dx, y, z + dz);
                BlockState state = level.getBlockState(checkPos);

                // Land = not water, not air, and not a liquid
                if (!isWater(state) && !state.isAir() && !state.is(Blocks.LAVA)) {
                    // Make sure there's air above to stand in
                    BlockPos above = checkPos.above();
                    if (level.getBlockState(above).isAir()) {
                        return checkPos;
                    }
                }
            }
        }
        return null;
    }
}
