package net.enderwish.Atmospheric_Overhaul_Subpack.client.sky;

import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * CelestialLight
 *
 * The light that is actually up.
 *
 * The sun and the moon are EXACTLY opposite each other: vanilla draws
 * the moon with the same rotation as the sun and puts it on the other
 * side (LevelRenderer.renderSky draws the sun at y=+100 and the moon
 * at y=-100 in the same rotated frame). So at any moment exactly one
 * of them is above the horizon, and whichever it is, is the light
 * source for everything the mod draws from the sky.
 *
 * That gives the god rays a moon at night for free: the shafts march
 * towards the light, the shadow map is built from the light's point of
 * view, and the trees block the moonlight exactly as they block the
 * sun.
 *
 * The moon is much dimmer than the sun and much colder, and the night
 * itself is darkened, so the two together give the contrast the user
 * asked for: a dark sky with cold shafts in it.
 */
public final class CelestialLight {

    private CelestialLight() {}

    /** How much darker the night is than vanilla. */
    public static final float NIGHT_DARKEN = 0.42f;
    /** How strong the moonlight is, against the sun's 1.0. */
    public static final float MOON_STRENGTH = 0.20f;

    /** The direction TOWARD the sun, in world space. */
    public static Vector3f sunDirection(float dayT) {
        return SunRenderer.worldDirection(dayT);
    }

    /**
     * The direction towards the light that is up: the sun by day, the
     * moon by night. The moon is the sun's direction reversed.
     */
    public static Vector3f direction(float dayT) {
        Vector3f sun = SunRenderer.worldDirection(dayT);
        return sun.y > 0.0f ? sun : new Vector3f(-sun.x, -sun.y, -sun.z);
    }

    /** True when the moon is the light source. */
    public static boolean isMoon(float dayT) {
        return SunRenderer.worldDirection(dayT).y <= 0.0f;
    }

    /**
     * How deep into the night it is: 0 in daylight, 1 at full night.
     * Used to darken the world, so the shafts have something to stand
     * out against.
     */
    public static float nightAmount(float dayT) {
        float sunY = SunRenderer.worldDirection(dayT).y;
        return Mth.clamp(-sunY / 0.14f, 0.0f, 1.0f);
    }

    /** The light's colour: warm by day, cold silver by night. */
    public static float[] tint(float dayT, float warmR, float warmG, float warmB) {
        if (isMoon(dayT)) {
            return new float[]{0.60f, 0.74f, 1.00f};
        }
        return new float[]{warmR, warmG, warmB};
    }

    /** How strong the light is. The moon is a fraction of the sun. */
    public static float strength(float dayT) {
        return isMoon(dayT) ? MOON_STRENGTH : 1.0f;
    }
}
