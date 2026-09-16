package net.enderwish.Atmospheric_Overhaul_Subpack.core.season;

import net.enderwish.Atmospheric_Overhaul_Subpack.core.temperature.HeatBlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * LocalChunkTemperature
 *
 * "Feels like" temperature = seasonal base temp + nearby heat-block modifier.
 *
 * SEASONAL RANGE (new this round):
 *   effectiveDist = actualDist / seasonRangeMultiplier
 *   contribution  = blockTemp / (effectiveDist² + 1)
 *
 * Standing ON a block (actualDist=0) ALWAYS gives full, unmodified block
 * temperature — season never touches this. Season only compresses how far
 * the heat reaches: Summer = full natural range (1.0, the only season at
 * 100%). Spring/Autumn = 0.85 (mildly reduced). Winter = 0.60 (heat sources
 * never reach their normal distance). This matches: "temp blocks will never
 * transfer full range due to the 4 seasons, but standing on them gives max
 * temp."
 *
 * Note: this now computes a real sqrt(distance) per scanned block (needed
 * to apply the seasonal divide before squaring) — previously the code
 * worked directly in squared-distance space to skip that sqrt. Cost is
 * negligible (up to ~1536 sqrt calls per query, this isn't called every
 * tick) but flagging the change honestly since it wasn't there before.
 */
public class LocalChunkTemperature {

    private static final float MAX_BLOCK_MODIFIER  =  2.0f;
    private static final float MIN_BLOCK_MODIFIER  = -1.5f;

    private static final float RANGE_SUMMER = 1.00f;
    private static final float RANGE_SPRING = 0.85f;
    private static final float RANGE_AUTUMN = 0.85f;
    private static final float RANGE_WINTER = 0.60f;

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

        float blockModifier = calculateBlockModifier(level, pos, season);

        return seasonalTemp + blockModifier;
    }

    private static float calculateBlockModifier(ServerLevel level, BlockPos pos,
                                                SeasonCalendar.Season season) {
        float total = 0.0f;
        float rangeMultiplier = getRangeMultiplier(season);

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
                    double actualDist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    double effectiveDist = actualDist / rangeMultiplier;

                    float contribution = (float) (blockTemp / (effectiveDist * effectiveDist + 1.0));
                    total += contribution;
                }
            }
        }

        return Math.max(MIN_BLOCK_MODIFIER, Math.min(MAX_BLOCK_MODIFIER, total));
    }

    private static float getRangeMultiplier(SeasonCalendar.Season season) {
        return switch (season) {
            case SUMMER -> RANGE_SUMMER;
            case SPRING -> RANGE_SPRING;
            case AUTUMN -> RANGE_AUTUMN;
            case WINTER -> RANGE_WINTER;
        };
    }

    public static float getBlockModifierOnly(ServerLevel level, BlockPos pos) {
        SeasonCalendar.Season season = SeasonData.get(level).getSeason();
        return calculateBlockModifier(level, pos, season);
    }

    public static int toCelsius(float localTemp) {
        return SeasonTemperature.toCelsius(localTemp);
    }

    public static String getLabel(float localTemp) {
        return SeasonTemperature.getLabel(localTemp);
    }
}
