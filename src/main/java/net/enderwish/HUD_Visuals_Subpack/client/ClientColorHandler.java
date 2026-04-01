package net.enderwish.HUD_Visuals_Subpack.client;

import net.enderwish.HUD_Visuals_Subpack.core.Season;
import net.enderwish.HUD_Visuals_Subpack.event.SeasonManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;

/**
 * Handles the mathematical blending of biome colors based on seasons.
 */
public class ClientColorHandler {

    /**
     * Forces the game to re-render all chunks and clear color caches.
     * Call this from ClientSeasonHandler when a season or day changes.
     */
    public static void refreshVisuals() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LevelRenderer renderer = mc.levelRenderer;

        if (level != null) {
            level.clearTintCaches();
        }

        if (renderer != null) {
            renderer.allChanged();
        }
    }

    /**
     * Modifies the base grass color. Now checks if the biome is a "Hot Biome"
     * to prevent snow-colors in the desert.
     */
    public static int modifyGrassColor(Holder<Biome> biome, int originalColor) {
        // EXCLUSION: If it's a desert/savanna, return original colors or very subtle shifts
        if (SeasonManager.isHotBiome(biome)) {
            return originalColor;
        }

        Season season = ClientSeasonHandler.getSeason();
        return switch (season) {
            case SPRING -> originalColor;
            case SUMMER -> multiply(originalColor, 1.0f, 1.05f, 0.9f);
            case AUTUMN -> lerpColor(originalColor, 0x9B8136, 0.45f);
            case WINTER -> lerpColor(originalColor, 0x82968E, 0.6f);
        };
    }

    /**
     * Modifies foliage color with biome exclusion.
     */
    public static int modifyFoliageColor(Holder<Biome> biome, int originalColor) {
        if (SeasonManager.isHotBiome(biome)) {
            return originalColor;
        }

        Season season = ClientSeasonHandler.getSeason();
        return switch (season) {
            case SPRING -> originalColor;
            case SUMMER -> originalColor;
            case AUTUMN -> lerpColor(originalColor, 0xBF8D2C, 0.85f);
            case WINTER -> lerpColor(originalColor, 0x729990, 0.7f);
        };
    }

    /**
     * Optional: Add Water tinting for Winter to make it look "frozen" and dark.
     */
    public static int modifyWaterColor(Holder<Biome> biome, int originalColor) {
        if (ClientSeasonHandler.getSeason() == Season.WINTER && !SeasonManager.isHotBiome(biome)) {
            return lerpColor(originalColor, 0x394D62, 0.5f); // Dark, cold blue
        }
        return originalColor;
    }

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

    private static int multiply(int color, float rM, float gM, float bM) {
        int r = Math.min(255, (int) (((color >> 16) & 0xFF) * rM));
        int g = Math.min(255, (int) (((color >> 8) & 0xFF) * gM));
        int b = Math.min(255, (int) ((color & 0xFF) * bM));

        return (r << 16) | (g << 8) | b;
    }
}