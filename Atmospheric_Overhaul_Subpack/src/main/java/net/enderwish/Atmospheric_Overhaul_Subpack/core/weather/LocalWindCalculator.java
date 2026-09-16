package net.enderwish.Atmospheric_Overhaul_Subpack.core.weather;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;

/**
 * LocalWindCalculator
 *
 * Computes a player's "felt wind" — the GLOBAL wind (from WindManager),
 * reduced when nearby solid geometry shelters the player from it.
 *
 * v1 scope (sheltered-only, no funneling): checks for a solid block
 * within a short distance in the direction the wind is blowing FROM.
 * The closer/more solid that obstruction, the more the felt speed is
 * reduced. Direction itself is NOT altered in this version — a true
 * funneling/redirect effect (wind speeding up through gaps) is a
 * meaningfully harder heuristic, deferred as a possible v2 addition.
 *
 * Detection range is currently a fixed constant (one chunk width, 16
 * blocks) rather than config-driven — Frontier's hardware-tiering config
 * system (scoped to eventually control this) doesn't exist yet. Once it
 * does, DETECTION_RANGE should read from that config instead.
 */
public class LocalWindCalculator {

    /** Matches "minimum is always current chunk" from the design discussion. */
    public static final int DETECTION_RANGE = 16;

    private static final int SHELTER_CHECK_DISTANCE = 4;

    private LocalWindCalculator() {}

    /**
     * Returns the felt wind SPEED at a position, given the level's current
     * global wind. Direction is unchanged from global (v1 scope).
     */
    public static float calculateFeltSpeed(ServerLevel level, BlockPos pos,
                                           WindDirection globalDirection,
                                           float globalSpeed) {
        // No sky access at all — treat as fully sheltered indoors.
        if (!level.canSeeSky(pos)) {
            return globalSpeed * 0.05f;
        }

        // Step a short distance in the direction the wind is blowing FROM
        // (opposite of travel direction) checking for a solid block.
        int windwardX = -Math.round(globalDirection.dx);
        int windwardZ = -Math.round(globalDirection.dz);

        for (int dist = 1; dist <= SHELTER_CHECK_DISTANCE; dist++) {
            BlockPos checkPos = pos.offset(windwardX * dist, 0, windwardZ * dist);
            if (level.getBlockState(checkPos).isSolid()) {
                // Closer obstruction = more sheltered = lower felt speed.
                float shelterFactor = (float) dist / SHELTER_CHECK_DISTANCE;
                return globalSpeed * Mth.clamp(shelterFactor, 0.1f, 1.0f);
            }
        }

        // No obstruction found within range — full exposure to global wind.
        return globalSpeed;
    }
}
