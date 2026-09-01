package net.enderwish.Atmospheric_Overhaul_Subpack.core.weather;

/**
 * WindDirection
 *
 * Eight compass directions. Each stores a normalized
 * horizontal vector (dx, dz) pointing in that direction.
 * Used by WindState to drive particle velocity and
 * player animation lean direction.
 *
 * Coordinate system (Minecraft):
 *   +X = East,  -X = West
 *   +Z = South, -Z = North
 */
public enum WindDirection {

    NORTH   ( 0.00f, -1.00f),
    NORTHEAST( 0.71f, -0.71f),
    EAST    ( 1.00f,  0.00f),
    SOUTHEAST( 0.71f,  0.71f),
    SOUTH   ( 0.00f,  1.00f),
    SOUTHWEST(-0.71f,  0.71f),
    WEST    (-1.00f,  0.00f),
    NORTHWEST(-0.71f, -0.71f);

    /** Normalized X component (east positive) */
    public final float dx;
    /** Normalized Z component (south positive) */
    public final float dz;

    WindDirection(float dx, float dz) {
        this.dx = dx;
        this.dz = dz;
    }

    /**
     * Returns a random wind direction.
     */
    public static WindDirection random(net.minecraft.util.RandomSource r) {
        WindDirection[] values = values();
        return values[r.nextInt(values.length)];
    }

    /**
     * Returns the direction closest to the given angle in degrees.
     * 0° = North, 90° = East, 180° = South, 270° = West.
     */
    public static WindDirection fromDegrees(float degrees) {
        float normalized = ((degrees % 360) + 360) % 360;
        int index = (int) ((normalized + 22.5f) / 45f) % 8;
        return values()[index];
    }

    /**
     * Returns the direction shifted by the given number of steps clockwise.
     */
    public WindDirection rotate(int steps) {
        WindDirection[] values = values();
        return values[((ordinal() + steps) % 8 + 8) % 8];
    }

    /**
     * Returns the opposite direction.
     */
    public WindDirection opposite() {
        return rotate(4);
    }
}
