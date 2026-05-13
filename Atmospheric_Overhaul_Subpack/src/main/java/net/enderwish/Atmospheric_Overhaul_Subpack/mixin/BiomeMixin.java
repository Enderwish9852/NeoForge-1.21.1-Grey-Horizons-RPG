package net.enderwish.Atmospheric_Overhaul_Subpack.mixin;

import net.enderwish.Atmospheric_Overhaul_Subpack.client.ClientSeasonState;
import net.enderwish.Atmospheric_Overhaul_Subpack.core.season.SeasonCalendar;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.core.BlockPos;

/**
 * BiomeMixin
 *
 * Intercepts getGrassColor() to blend the vanilla biome grass colour
 * toward a season tint.
 *
 * Only grass colour is overridden — foliage (leaves) and water are untouched.
 * Biome temperature is read-only — never modified.
 *
 * Hot biomes (temp >= 1.0) get a weaker blend so they never look frozen.
 * Cold biomes (temp <= 0.15) get a weaker blend since they're already muted.
 */
@Mixin(Biome.class)
public class BiomeMixin {

    @Inject(method = "getGrassColor", at = @At("RETURN"), cancellable = true)
    private void onGetGrassColor(double x, double z, CallbackInfoReturnable<Integer> cir) {

        int vanillaColor = cir.getReturnValue();
        SeasonCalendar.Season season = ClientSeasonState.getSeason();
        SeasonCalendar.Phase  phase  = ClientSeasonState.getPhase();

        // Get the biome temperature to scale the blend strength
        // Hot biomes blend less, cold biomes blend less
        Biome biome = (Biome) (Object) this;
        float temp = biome.getBaseTemperature();
        float blendStrength = getBlendStrength(temp, season);

        // Get the season tint colour
        int seasonTint = getSeasonGrassTint(season, phase);

        // Blend vanilla colour toward season tint
        int blended = blendColors(vanillaColor, seasonTint, blendStrength);
        cir.setReturnValue(blended);
    }

    @Inject(method = "getPrecipitationAt", at = @At("RETURN"), cancellable = true)
    private void onGetPrecipitationAt(BlockPos pos, CallbackInfoReturnable<Biome.Precipitation> cir) {
        // Only act if it's actually precipitating
        if (!ClientSeasonState.isPrecipitating()) return;

        Biome biome = (Biome) (Object) this;
        float temp = biome.getBaseTemperature();
        SeasonCalendar.Season season = ClientSeasonState.getSeason();

        // Hot biomes — never snow regardless of season
        if (temp >= 1.0f) {
            cir.setReturnValue(Biome.Precipitation.RAIN);
            return;
        }

        // Winter — force snow in all non-hot biomes
        if (season == SeasonCalendar.Season.WINTER) {
            cir.setReturnValue(Biome.Precipitation.SNOW);
            return;
        }

        // Summer — force rain even in cold biomes
        if (season == SeasonCalendar.Season.SUMMER) {
            cir.setReturnValue(Biome.Precipitation.RAIN);
            return;
        }

        // Spring/Autumn — let vanilla decide based on biome temp
        // Cold biomes still snow, temperate biomes rain
    }

    // ── Season tints ──────────────────────────────────────────────────────────

    /**
     * Returns the target grass tint colour for each season + phase.
     * Colours are RGB integers.
     *
     * Spring  — fresh bright green, slightly yellower in early
     * Summer  — rich deep green, slightly golden in late
     * Autumn  — warm amber/orange, deepening toward late
     * Winter  — desaturated grey-green, near white in mid winter
     */
    private static int getSeasonGrassTint(SeasonCalendar.Season season,
                                          SeasonCalendar.Phase phase) {
        return switch (season) {
            case SPRING -> switch (phase) {
                case EARLY -> 0x7DB83A; // yellow-green, just waking up
                case MID   -> 0x7DB232; // bright fresh green
                case LATE  -> 0x4E9E2A; // rich green, heading to summer
            };
            case SUMMER -> switch (phase) {
                case EARLY -> 0x4A9E28; // deep rich green
                case MID   -> 0x4B9E1E; // peak summer green
                case LATE  -> 0x5A9A1E; // slight golden tinge, drying out
            };
            case AUTUMN -> switch (phase) {
                case EARLY -> 0x8A8A20; // olive-yellow, cooling
                case MID   -> 0xBF8D2C; // warm amber
                case LATE  -> 0x906010; // deep burnt orange-brown
            };
            case WINTER -> switch (phase) {
                case EARLY -> 0x7A8A70; // grey-green, first frosts
                case MID   -> 0x729990; // muted grey-green
                case LATE  -> 0x7A8A72; // slight thaw, returning green
            };
        };
    }

    // ── Blend strength ────────────────────────────────────────────────────────

    /**
     * Returns how strongly to blend toward the season tint (0.0 - 1.0).
     *
     * Base strength is 0.45 — noticeable but not overwhelming.
     * Hot biomes (desert, savanna) blend less — they shouldn't look frozen.
     * Cold biomes (taiga, snowy plains) blend less — already muted.
     * Temperate biomes get the full blend.
     */
    private static float getBlendStrength(float biomeTemp,
                                          SeasonCalendar.Season season) {
        float base = 0.45f;

        // Extreme hot biomes — reduce winter/autumn tint significantly
        if (biomeTemp >= 1.5f) {
            return switch (season) {
                case WINTER -> 0.10f; // barely any winter tint in desert
                case AUTUMN -> 0.15f;
                case SUMMER -> 0.20f; // summer tint still applies a little
                case SPRING -> 0.20f;
            };
        }

        // Hot biomes — reduce cold season tints
        if (biomeTemp >= 1.0f) {
            return switch (season) {
                case WINTER -> 0.15f;
                case AUTUMN -> 0.25f;
                case SUMMER -> base;
                case SPRING -> base - 0.05f;
            };
        }

        // Cold biomes — reduce summer warmth tints
        if (biomeTemp <= 0.15f) {
            return switch (season) {
                case SUMMER -> 0.20f; // cold biome won't look tropical in summer
                case SPRING -> 0.25f;
                case AUTUMN -> base;
                case WINTER -> base + 0.10f; // extra winter feel in cold biomes
            };
        }

        // Temperate biomes — full blend
        return base;
    }

    // ── Colour blending ───────────────────────────────────────────────────────

    /**
     * Linearly blends two RGB colours.
     * t = 0.0 returns colorA (vanilla), t = 1.0 returns colorB (season tint).
     */
    private static int blendColors(int colorA, int colorB, float t) {
        int rA = (colorA >> 16) & 0xFF;
        int gA = (colorA >> 8)  & 0xFF;
        int bA =  colorA        & 0xFF;

        int rB = (colorB >> 16) & 0xFF;
        int gB = (colorB >> 8)  & 0xFF;
        int bB =  colorB        & 0xFF;

        int r = (int) (rA + (rB - rA) * t);
        int g = (int) (gA + (gB - gA) * t);
        int b = (int) (bA + (bB - bA) * t);

        return (r << 16) | (g << 8) | b;
    }
}