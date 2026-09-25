package net.enderwish.TerraForma_Subpack.core.climate;

/**
 * ClimateSettings
 *
 * RETUNED this round, correcting a real mistake from last round: I
 * scaled MOISTURE_SCALE (and to a lesser extent TEMPERATURE_SCALE)
 * targeting the FULL desired biome-patch size directly -- but moisture
 * is ONE periodic noise field that up to 5 different biome thresholds
 * carve out of, so each resulting patch was only getting roughly a
 * fifth of that number. That's the direct cause of "3 biomes crammed
 * together, characteristics can't show." Both widened to compensate:
 * MOISTURE_SCALE 2,600 -> 11,000 (11,000/5 ~= 2,200, back in the
 * target range). TEMPERATURE_SCALE 9,000 -> 14,000 -- this also
 * widens the noiseVariation wavelength used for the Wasteland-internal
 * Cracked Badlands/Ash Plains split (derived as TEMPERATURE_SCALE*0.3
 * inside ClimateMap), fixing that squeeze too, while keeping a full
 * arctic-to-tropical crossing around 45-50 minutes on foot -- still
 * inside the original target.
 */
public final class ClimateSettings {

    private ClimateSettings() {}

    // ── Temperature axis ──────────────────────────────────────────────────────
    public static final float TEMPERATURE_SCALE = 14_000f;
    public static final float ALTITUDE_TEMP_DROP = 0.003f;
    public static final int SEA_LEVEL = 63;
    public static final int ALPINE_HEIGHT = 160;

    // ── Moisture axis ─────────────────────────────────────────────────────────
    public static final float MOISTURE_SCALE = 11_000f;

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
