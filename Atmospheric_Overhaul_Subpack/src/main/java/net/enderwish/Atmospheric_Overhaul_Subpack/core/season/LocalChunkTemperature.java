package net.enderwish.Atmospheric_Overhaul_Subpack.core.season;

import net.enderwish.Atmospheric_Overhaul_Subpack.core.temperature.HeatBlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * LocalChunkTemperature
 *
 * Calculates the "feels like" temperature at a specific position
 * accounting for:
 *   1. Seasonal base temperature (from SeasonTemperature)
 *   2. Nearby hot/cold blocks within the chunk surface + 5 blocks
 *      underground — values now JSON-driven via HeatBlockRegistry
 *      (data/gh_atmospheric/heat_blocks/*.json) rather than hardcoded
 *      constants.
 *
 * Scan area:
 *   - Horizontal: full 16×16 chunk surface around target
 *   - Vertical: target Y down to target Y - 5 (shallow underground like lava lakes)
 *
 * Distance mitigation:
 *   contribution = blockBaseTemp / (distance² + 1)
 */
public class LocalChunkTemperature {

    private static final float MAX_BLOCK_MODIFIER  =  2.0f;
    private static final float MIN_BLOCK_MODIFIER  = -1.5f;

    public static float calculate(ServerLevel level, BlockPos pos) {
        SeasonData data = SeasonData.get(level);
        return calculate(level, pos,
                data.getSeason(),
                data.getPhase(),
                data.getActiveWeatherId(),
                data.getActiveIntensity());
    }

    public static float calculate(ServerLevel level, BlockPos pos,
                                  SeasonCalendar.Season season,
                                  SeasonCalendar.Phase phase,
                                  String activeWeatherId,
                                  float weatherIntensity) {

        float seasonalTemp = SeasonTemperature.calculate(
                level, pos, season, phase, activeWeatherId, weatherIntensity);

        float blockModifier = calculateBlockModifier(level, pos);

        return seasonalTemp + blockModifier;
    }

    private static float calculateBlockModifier(ServerLevel level, BlockPos pos) {
        float total = 0.0f;

        int chunkX = pos.getX() >> 4 << 4;
        int chunkZ = pos.getZ() >> 4 << 4;
        int scanTop    = pos.getY();
        int scanBottom = pos.getY() - 5;

        for (int x = chunkX; x < chunkX + 16; x++) {
            for (int z = chunkZ; z < chunkZ + 16; z++) {
                for (int y = scanTop; y >= scanBottom; y--) {
                    BlockPos scanPos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(scanPos);

                    float blockTemp = HeatBlockRegistry.INSTANCE.getTemperature(state);
                    if (blockTemp == 0.0f) continue;

                    double dx = pos.getX() - x;
                    double dy = pos.getY() - y;
                    double dz = pos.getZ() - z;
                    double distSquared = dx * dx + dy * dy + dz * dz;
                    float contribution = (float) (blockTemp / (distSquared + 1.0));

                    total += contribution;
                }
            }
        }

        return Math.max(MIN_BLOCK_MODIFIER, Math.min(MAX_BLOCK_MODIFIER, total));
    }

    public static float getBlockModifierOnly(ServerLevel level, BlockPos pos) {
        return calculateBlockModifier(level, pos);
    }

    public static int toCelsius(float localTemp) {
        return SeasonTemperature.toCelsius(localTemp);
    }

    public static String getLabel(float localTemp) {
        return SeasonTemperature.getLabel(localTemp);
    }
}
