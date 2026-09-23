package net.enderwish.TerraForma_Subpack.core.climate;

/**
 * ClimateSettings
 *
 * Central place for all climate system constants.
 *
 * RETUNED this round: TEMPERATURE_SCALE and MOISTURE_SCALE both
 * narrowed to hit the confirmed biome-size target (standard biomes
 * ~1,800-3,500 blocks across; a full arctic-to-tropical crossing now
 * ~40-70 minutes on foot instead of the better part of two hours).
 * WASTELAND_SCALE scaled down proportionally to match. Thresholds
 * below are untouched -- they're proportions of the -1..1 / 0..1
 * range, not distances, so they don't need to move.
 */
public final class ClimateSettings {

    private ClimateSettings() {}

    // ── Temperature axis ──────────────────────────────────────────────────────
    public static final float TEMPERATURE_SCALE = 9_000f;
    public static final float ALTITUDE_TEMP_DROP = 0.003f;
    public static final int SEA_LEVEL = 63;
    public static final int ALPINE_HEIGHT = 160;

    // ── Moisture axis ─────────────────────────────────────────────────────────
    public static final float MOISTURE_SCALE = 2_600f;

    // ── Wasteland placement ───────────────────────────────────────────────────
    public static final float WASTELAND_SCALE = 4_000f;
    public static final float WASTELAND_THRESHOLD = 0.65f;
    public static final float WASTELAND_COVERAGE = 0.15f;

    // ── Climate zone thresholds ───────────────────────────────────────────────
    public static final float ARCTIC_THRESHOLD     = -0.4f;
    public static final float TEMPERATE_THRESHOLD  =  0.1f;
    public static final float MEDITERRANEAN_THRESHOLD = 0.5f;

    // ── Moisture thresholds ───────────────────────────────────────────────────
    public static final float DRY_THRESHOLD  = 0.35f;
    public static final float WET_THRESHOLD  = 0.65f;
}
