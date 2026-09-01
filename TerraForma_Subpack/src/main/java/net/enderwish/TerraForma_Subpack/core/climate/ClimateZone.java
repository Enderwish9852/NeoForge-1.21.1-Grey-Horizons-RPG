package net.enderwish.TerraForma_Subpack.core.climate;

/**
 * ClimateZone
 *
 * The five climate zones that drive biome placement,
 * weather patterns, and agricultural viability.
 *
 * Assigned per world position by ClimateMap using
 * temperature axis (north/south + altitude) and
 * moisture axis (separate noise).
 */
public enum ClimateZone {

    ARCTIC(
            "Arctic",
            -0.8f,   // base temperature modifier
            0.2f,    // moisture (low — frozen precipitation)
            false,   // farming possible
            true     // snow/ice forms
    ),

    TEMPERATE(
            "Temperate",
            0.0f,
            0.6f,
            true,
            false
    ),

    MEDITERRANEAN(
            "Mediterranean",
            0.3f,
            0.3f,   // dry summers
            true,
            false
    ),

    TROPICAL(
            "Tropical",
            0.8f,
            0.9f,   // high humidity
            true,
            false
    ),

    WASTELAND(
            "Wasteland",
            0.2f,
            0.1f,   // parched
            false,
            false
    );

    // ── Fields ────────────────────────────────────────────────────────────────

    public final String displayName;
    public final float baseTempModifier;  // added to Atmospheric seasonal temp
    public final float moisture;          // 0.0 (arid) to 1.0 (saturated)
    public final boolean farmingPossible; // crops can grow here
    public final boolean formsSnow;       // snow/ice forms here naturally

    ClimateZone(String displayName, float baseTempModifier,
                float moisture, boolean farmingPossible,
                boolean formsSnow) {
        this.displayName    = displayName;
        this.baseTempModifier = baseTempModifier;
        this.moisture       = moisture;
        this.farmingPossible = farmingPossible;
        this.formsSnow      = formsSnow;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public boolean isCold()    { return this == ARCTIC; }
    public boolean isHot()     { return this == TROPICAL || this == MEDITERRANEAN; }
    public boolean isArid()    { return this == WASTELAND || this == MEDITERRANEAN; }
    public boolean isHumid()   { return this == TROPICAL || this == TEMPERATE; }
}