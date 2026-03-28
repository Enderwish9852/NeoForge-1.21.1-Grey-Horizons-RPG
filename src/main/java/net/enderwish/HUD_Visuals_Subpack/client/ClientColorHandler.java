package net.enderwish.HUD_Visuals_Subpack.client;

import net.enderwish.HUD_Visuals_Subpack.core.Season;
import net.minecraft.util.Mth;

/**
 * Handles the mathematical blending of biome colors based on seasons.
 * This class is called by BiomeMixin to apply seasonal shifts to grass and foliage.
 */
public class ClientColorHandler {

    /**
     * Modifies the base grass color of a biome based on the current season.
     * Grass usually stays somewhat green but shifts hue.
     */
    public static int modifyGrassColor(int originalColor) {
        Season season = ClientSeasonHandler.getClientSeason();

        return switch (season) {
            case SPRING -> originalColor; // Fresh, natural spring green
            case SUMMER -> multiply(originalColor, 1.0f, 1.05f, 0.9f); // Brighter, slightly more yellow-green
            case AUTUMN -> lerpColor(originalColor, 0x9B8136, 0.45f);  // Muted olive/brownish blend
            case WINTER -> lerpColor(originalColor, 0x82968E, 0.6f);   // Desaturated, cold "frosted" look
        };
    }

    /**
     * Modifies foliage color. Leaves change more drastically than grass in many biomes.
     */
    public static int modifyFoliageColor(int originalColor) {
        Season season = ClientSeasonHandler.getClientSeason();

        return switch (season) {
            case SPRING -> originalColor;
            case SUMMER -> originalColor;
            case AUTUMN -> lerpColor(originalColor, 0xBF8D2C, 0.85f); // Deep orange/gold (stronger effect)
            case WINTER -> lerpColor(originalColor, 0x729990, 0.7f);  // Icy teal/grey
        };
    }

    /**
     * Utility to blend two hex colors together (Linear Interpolation).
     * @param colorA Start color
     * @param colorB Target color
     * @param factor 0.0 to 1.0 (how much of colorB to mix in)
     */
    private static int lerpColor(int colorA, int colorB, float factor) {
        int r1 = (colorA >> 16) & 0xFF;
        int g1 = (colorA >> 8) & 0xFF;
        int b1 = colorA & 0xFF;

        int r2 = (colorB >> 16) & 0xFF;
        int g2 = (colorB >> 8) & 0xFF;
        int b2 = colorB & 0xFF;

        int r = (int) Mth.lerp(factor, r1, r2);
        int g = (int) Mth.lerp(factor, g1, g2);
        int b = (int) Mth.lerp(factor, b1, b2);

        return (r << 16) | (g << 8) | b;
    }

    /**
     * Utility to multiply RGB channels individually for subtle tinting.
     */
    private static int multiply(int color, float rM, float gM, float bM) {
        int r = (int) (((color >> 16) & 0xFF) * rM);
        int g = (int) (((color >> 8) & 0xFF) * gM);
        int b = (int) ((color & 0xFF) * bM);

        // Clamp values to ensure they don't exceed 255 (white)
        r = Math.min(255, Math.max(0, r));
        g = Math.min(255, Math.max(0, g));
        b = Math.min(255, Math.max(0, b));

        return (r << 16) | (g << 8) | b;
    }
}