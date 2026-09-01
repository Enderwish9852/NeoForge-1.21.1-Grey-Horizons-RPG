package net.enderwish.TerraForma_Subpack.core.climate;

/**
 * ClimateSettings
 *
 * Central place for all climate system constants.
 * Tweak these to change how climate zones feel
 * without touching the noise logic.
 */
public final class ClimateSettings {

    private ClimateSettings() {}

    // ── Temperature axis ──────────────────────────────────────────────────────
    // Controls north/south climate gradient
    // World Z coordinate is mapped to -1.0 (north/arctic) to +1.0 (south/tropical)

    /** World Z scale — how many blocks per full climate cycle */
    public static final float TEMPERATURE_SCALE = 20_000f;

    /** Altitude modifier — temperature drops this much per block above sea level */
    public static final float ALTITUDE_TEMP_DROP = 0.003f;

    /** Sea level — blocks above this start getting colder */
    public static final int SEA_LEVEL = 63;

    /** Height where temperature reaches minimum regardless of latitude */
    public static final int ALPINE_HEIGHT = 160;

    // ── Moisture axis ─────────────────────────────────────────────────────────
    // Separate noise layer, independent of temperature

    /** Moisture noise scale — larger = broader wet/dry zones */
    public static final float MOISTURE_SCALE = 8_000f;

    // ── Wasteland placement ───────────────────────────────────────────────────
    // Wastelands are not climate-driven — they're placed by a separate noise
    // representing old war zones / collapse epicentres

    /** Wasteland noise scale */
    public static final float WASTELAND_SCALE = 12_000f;

    /** Wasteland threshold — noise values above this become wasteland */
    public static final float WASTELAND_THRESHOLD = 0.65f;

    /** Wasteland coverage — fraction of land that is wasteland */
    public static final float WASTELAND_COVERAGE = 0.15f;

    // ── Climate zone thresholds ───────────────────────────────────────────────
    // Based on normalized temperature value (-1.0 to +1.0)

    /** Below this = ARCTIC */
    public static final float ARCTIC_THRESHOLD     = -0.4f;

    /** Below this (above arctic) = TEMPERATE */
    public static final float TEMPERATE_THRESHOLD  =  0.1f;

    /** Below this (above temperate) = MEDITERRANEAN */
    public static final float MEDITERRANEAN_THRESHOLD = 0.5f;

    /** Above mediterranean = TROPICAL */
    // (implicitly anything above MEDITERRANEAN_THRESHOLD)

    // ── Moisture thresholds ───────────────────────────────────────────────────
    // Used within temperate zone to distinguish wet/dry biomes

    /** Below this = dry temperate (meadows, highlands) */
    public static final float DRY_THRESHOLD  = 0.35f;

    /** Above this = wet temperate (wetlands, dense forest) */
    public static final float WET_THRESHOLD  = 0.65f;
}
