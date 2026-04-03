package net.enderwish.HUD_Visuals_Subpack.client;

import net.enderwish.HUD_Visuals_Subpack.api.ClimateData;
import net.enderwish.HUD_Visuals_Subpack.core.Season;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;

/**
 * Handles seasonal and weather-based color shifts for grass, foliage, and water.
 * Aligned with Alpha Test Doc for seasonal transitions.
 */
public class ClientColorHandler {

    /**
     * Forces the game to refresh all chunk tints.
     * Called by ClimateSyncPacket to apply new colors immediately.
     */
    public static void refreshVisuals() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            mc.level.clearTintCaches();
            if (mc.levelRenderer != null) {
                mc.levelRenderer.allChanged();
            }
        }
    }

    /**
     * Modifies grass color based on Season and Rare Weather.
     */
    public static int modifyGrassColor(Holder<Biome> biome, int originalColor) {
        if (isExempt(biome)) return originalColor;

        ClimateData data = ClientClimateCache.get();
        Season season = data.season();

        // Special Logic: Pollen Haze gives a yellow tint to the grass [cite: 14]
        if (data.weather().contains("pollen_haze")) {
            return lerpColor(originalColor, 0xDAD35B, 0.3f);
        }

        return switch (season) {
            case SPRING -> originalColor; // Fresh vibrant green
            case SUMMER -> multiply(originalColor, 0.9f, 1.1f, 0.8f); // Slightly dried/yellowed
            case AUTUMN -> lerpColor(originalColor, 0x9B8136, 0.5f); // Gold/Brown
            case WINTER -> lerpColor(originalColor, 0x82968E, 0.6f); // Muted/Frosty
        };
    }

    /**
     * Modifies foliage (leaves) colors using the Hex values from Season.java.
     */
    public static int modifyFoliageColor(Holder<Biome> biome, int originalColor) {
        if (isExempt(biome)) return originalColor;

        Season season = ClientClimateCache.get().season();

        // Uses the Foliage Color defined in your Season enum
        return lerpColor(originalColor, season.getFoliageColor(), season == Season.AUTUMN ? 0.85f : 0.4f);
    }

    /**
     * Darkens water color during Winter and specialized weather.
     */
    public static int modifyWaterColor(Holder<Biome> biome, int originalColor) {
        ClimateData data = ClientClimateCache.get();

        // Thaw makes water look "muddier" or more active [cite: 16]
        if (data.weather().contains("thaw")) {
            return lerpColor(originalColor, 0x5E6B54, 0.4f);
        }

        if (data.season() == Season.WINTER && !isExempt(biome)) {
            return lerpColor(originalColor, 0x394D62, 0.5f); // Deep chilly blue
        }
        return originalColor;
    }

    private static boolean isExempt(Holder<Biome> biome) {
        return biome.is(BiomeTags.HAS_VILLAGE_DESERT) ||
                biome.is(BiomeTags.IS_NETHER) ||
                biome.is(BiomeTags.IS_END);
    }

    private static int lerpColor(int colorA, int colorB, float factor) {
        int r = (int) Mth.lerp(factor, (colorA >> 16) & 0xFF, (colorB >> 16) & 0xFF);
        int g = (int) Mth.lerp(factor, (colorA >> 8) & 0xFF, (colorB >> 8) & 0xFF);
        int b = (int) Mth.lerp(factor, colorA & 0xFF, colorB & 0xFF);
        return (r << 16) | (g << 8) | b;
    }

    private static int multiply(int color, float rM, float gM, float bM) {
        int r = Math.min(255, (int) (((color >> 16) & 0xFF) * rM));
        int g = Math.min(255, (int) (((color >> 8) & 0xFF) * gM));
        int b = Math.min(255, (int) ((color & 0xFF) * bM));
        return (r << 16) | (g << 8) | b;
    }
}