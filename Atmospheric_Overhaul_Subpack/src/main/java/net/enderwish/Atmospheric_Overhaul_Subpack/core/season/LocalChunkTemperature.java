package net.enderwish.Atmospheric_Overhaul_Subpack.core.season;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * LocalChunkTemperature
 *
 * Calculates the "feels like" temperature at a specific position
 * accounting for:
 *   1. Seasonal base temperature (from SeasonTemperature)
 *   2. Nearby hot/cold blocks within the chunk surface + 5 blocks underground
 *
 * Scan area:
 *   - Horizontal: full 16×16 chunk surface around target
 *   - Vertical: target Y down to target Y - 5 (shallow underground like lava lakes)
 *
 * Distance mitigation:
 *   contribution = blockBaseTemp / (distance² + 1)
 *   Standing ON the block = full contribution
 *   1 block away = half contribution
 *   2 blocks away = one fifth contribution (drops off fast, realistic)
 *
 * Usage:
 *   float localTemp = LocalChunkTemperature.calculate(level, pos);
 *   float localTemp = LocalChunkTemperature.calculate(level, pos, season, phase, weatherId, intensity);
 */
public class LocalChunkTemperature {

    // ── Block temperature values ──────────────────────────────────────────────

    // Hot blocks — contribution at distance 0
    private static final float LAVA_TEMP           =  1.2f;
    private static final float FIRE_TEMP           =  0.8f;
    private static final float MAGMA_TEMP          =  0.5f;
    private static final float CAMPFIRE_TEMP       =  0.4f;
    private static final float SOUL_CAMPFIRE_TEMP  =  0.3f;
    private static final float BLAST_FURNACE_TEMP  =  0.4f;
    private static final float FURNACE_TEMP        =  0.3f;
    private static final float SMOKER_TEMP         =  0.3f;

    // Cold blocks — contribution at distance 0 (negative)
    private static final float BLUE_ICE_TEMP       = -0.6f;
    private static final float PACKED_ICE_TEMP     = -0.4f;
    private static final float ICE_TEMP            = -0.3f;
    private static final float SNOW_BLOCK_TEMP     = -0.2f;
    private static final float POWDER_SNOW_TEMP    = -0.15f;

    // ── Max contribution cap ──────────────────────────────────────────────────
    // Prevents a room full of lava from giving unrealistic temperatures
    private static final float MAX_BLOCK_MODIFIER  =  2.0f;
    private static final float MIN_BLOCK_MODIFIER  = -1.5f;

    // ── Main calculation ──────────────────────────────────────────────────────

    /**
     * Full calculation — reads season/weather from SeasonData.
     * Use this for composter speed, player body temp etc.
     */
    public static float calculate(ServerLevel level, BlockPos pos) {
        SeasonData data = SeasonData.get(level);
        return calculate(level, pos,
                data.getSeason(),
                data.getPhase(),
                data.getActiveWeatherId(),
                data.getActiveIntensity());
    }

    /**
     * Full calculation with explicit season/weather parameters.
     * Useful for simulations or when you already have season data.
     */
    public static float calculate(ServerLevel level, BlockPos pos,
                                  SeasonCalendar.Season season,
                                  SeasonCalendar.Phase phase,
                                  String activeWeatherId,
                                  float weatherIntensity) {

        // 1. Seasonal base temperature (existing system)
        float seasonalTemp = SeasonTemperature.calculate(
                level, pos, season, phase, activeWeatherId, weatherIntensity);

        // 2. Block temperature modifier from nearby blocks
        float blockModifier = calculateBlockModifier(level, pos);

        return seasonalTemp + blockModifier;
    }

    // ── Block scanning ────────────────────────────────────────────────────────

    /**
     * Scans the chunk surface and 5 blocks underground at the target position.
     * Returns total block temperature modifier with distance mitigation.
     */
    private static float calculateBlockModifier(ServerLevel level, BlockPos pos) {
        float total = 0.0f;

        int chunkX = pos.getX() >> 4 << 4; // chunk origin X
        int chunkZ = pos.getZ() >> 4 << 4; // chunk origin Z
        int scanTop    = pos.getY();
        int scanBottom = pos.getY() - 5;

        // Scan the 16×16 chunk surface area from target Y down 5 blocks
        for (int x = chunkX; x < chunkX + 16; x++) {
            for (int z = chunkZ; z < chunkZ + 16; z++) {
                for (int y = scanTop; y >= scanBottom; y--) {
                    BlockPos scanPos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(scanPos);

                    float blockTemp = getBlockTemp(state);
                    if (blockTemp == 0.0f) continue;

                    // Distance mitigation: contribution = blockTemp / (dist² + 1)
                    double dx = pos.getX() - x;
                    double dy = pos.getY() - y;
                    double dz = pos.getZ() - z;
                    double distSquared = dx * dx + dy * dy + dz * dz;
                    float contribution = (float) (blockTemp / (distSquared + 1.0));

                    total += contribution;
                }
            }
        }

        // Clamp to prevent extreme values
        return Math.max(MIN_BLOCK_MODIFIER, Math.min(MAX_BLOCK_MODIFIER, total));
    }

    /**
     * Returns the base temperature value for a block.
     * Returns 0.0 if the block has no thermal effect.
     */
    private static float getBlockTemp(BlockState state) {
        // ── Lava ─────────────────────────────────────────────────────────────
        if (state.is(Blocks.LAVA)) return LAVA_TEMP;

        // ── Fire ─────────────────────────────────────────────────────────────
        if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)) return FIRE_TEMP;

        // ── Magma ─────────────────────────────────────────────────────────────
        if (state.is(Blocks.MAGMA_BLOCK)) return MAGMA_TEMP;

        // ── Campfires — only if lit ───────────────────────────────────────────
        if (state.is(Blocks.CAMPFIRE)) {
            return state.getValue(CampfireBlock.LIT) ? CAMPFIRE_TEMP : 0.0f;
        }
        if (state.is(Blocks.SOUL_CAMPFIRE)) {
            return state.getValue(CampfireBlock.LIT) ? SOUL_CAMPFIRE_TEMP : 0.0f;
        }

        // ── Furnaces — only if lit ────────────────────────────────────────────
        if (state.is(Blocks.FURNACE)) {
            return isLit(state) ? FURNACE_TEMP : 0.0f;
        }
        if (state.is(Blocks.BLAST_FURNACE)) {
            return isLit(state) ? BLAST_FURNACE_TEMP : 0.0f;
        }
        if (state.is(Blocks.SMOKER)) {
            return isLit(state) ? SMOKER_TEMP : 0.0f;
        }

        // ── Ice and snow blocks ───────────────────────────────────────────────
        if (state.is(Blocks.BLUE_ICE))    return BLUE_ICE_TEMP;
        if (state.is(Blocks.PACKED_ICE))  return PACKED_ICE_TEMP;
        if (state.is(Blocks.ICE) || state.is(Blocks.FROSTED_ICE)) return ICE_TEMP;
        if (state.is(Blocks.SNOW_BLOCK))  return SNOW_BLOCK_TEMP;
        if (state.is(Blocks.POWDER_SNOW)) return POWDER_SNOW_TEMP;

        return 0.0f;
    }

    /**
     * Returns true if a block with LIT state property is currently lit.
     */
    private static boolean isLit(BlockState state) {
        try {
            return state.getValue(BlockStateProperties.LIT);
        } catch (Exception e) {
            return false;
        }
    }

    // ── Display helpers ───────────────────────────────────────────────────────

    /**
     * Returns the block modifier only (without seasonal base).
     * Useful for debugging and testing.
     */
    public static float getBlockModifierOnly(ServerLevel level, BlockPos pos) {
        return calculateBlockModifier(level, pos);
    }

    /**
     * Returns the local chunk temp in Celsius for display.
     */
    public static int toCelsius(float localTemp) {
        return SeasonTemperature.toCelsius(localTemp);
    }

    /**
     * Returns a human-readable label for the local temp.
     */
    public static String getLabel(float localTemp) {
        return SeasonTemperature.getLabel(localTemp);
    }
}
