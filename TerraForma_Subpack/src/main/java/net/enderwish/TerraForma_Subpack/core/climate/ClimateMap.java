package net.enderwish.TerraForma_Subpack.core.climate;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;

import java.util.Random;

/**
 * ClimateMap
 *
 * Assigns a ClimateZone to any world position using:
 *   1. Temperature axis — Z coordinate + altitude modifier + local noise
 *   2. Moisture axis — independent Perlin noise
 *   3. Wasteland placement — separate noise for collapse epicentres
 */
public class ClimateMap {

    public static final ClimateMap INSTANCE = new ClimateMap();
    private ClimateMap() {}

    private ImprovedNoise temperatureNoise = null;
    private ImprovedNoise moistureNoise    = null;
    private ImprovedNoise wastelandNoise   = null;

    private long lastSeed = Long.MIN_VALUE;

    /**
     * Must be called when the world loads to seed noise with the world seed.
     * Called from GHBiomeSource on construction.
     */
    public void setSeed(long seed) {
        if (seed == lastSeed) return;
        lastSeed = seed;

        RandomSource rand  = RandomSource.create(seed);
        RandomSource rand2 = RandomSource.create(rand.nextLong());
        RandomSource rand3 = RandomSource.create(rand.nextLong());

        temperatureNoise = new ImprovedNoise(rand);
        moistureNoise    = new ImprovedNoise(rand2);
        wastelandNoise   = new ImprovedNoise(rand3);
    }

    // ── Main queries ──────────────────────────────────────────────────────────

    public ClimateZone getZone(BlockPos pos) {
        return getZone(pos.getX(), pos.getY(), pos.getZ());
    }

    public ClimateZone getZone(int x, int y, int z) {
        ensureInitialized();

        // Wasteland check first — overrides climate
        if (getWastelandNoise(x, z) > ClimateSettings.WASTELAND_THRESHOLD) {
            return ClimateZone.WASTELAND;
        }

        float temp = getTemperature(x, y, z);

        if (temp < ClimateSettings.ARCTIC_THRESHOLD)          return ClimateZone.ARCTIC;
        if (temp < ClimateSettings.TEMPERATE_THRESHOLD)       return ClimateZone.TEMPERATE;
        if (temp < ClimateSettings.MEDITERRANEAN_THRESHOLD)   return ClimateZone.MEDITERRANEAN;
        return ClimateZone.TROPICAL;
    }

    public float getTemperature(BlockPos pos) {
        return getTemperature(pos.getX(), pos.getY(), pos.getZ());
    }

    public float getTemperature(int x, int y, int z) {
        ensureInitialized();

        // 1. Latitude gradient — Z axis, north cold south hot
        float latitudeTemp = Mth.clamp(
                (float) z / ClimateSettings.TEMPERATURE_SCALE,
                -1.0f, 1.0f);

        // 2. Altitude modifier
        float altitudeDrop = Math.max(0, y - ClimateSettings.SEA_LEVEL)
                * ClimateSettings.ALTITUDE_TEMP_DROP;

        // 3. Local noise variation
        float noiseVariation = (float) temperatureNoise.noise(
                x / (ClimateSettings.TEMPERATURE_SCALE * 0.3),
                0,
                z / (ClimateSettings.TEMPERATURE_SCALE * 0.3)) * 0.2f;

        return Mth.clamp(latitudeTemp - altitudeDrop + noiseVariation,
                -1.0f, 1.0f);
    }

    public float getMoisture(BlockPos pos) {
        return getMoisture(pos.getX(), pos.getZ());
    }

    public float getMoisture(int x, int z) {
        ensureInitialized();
        float raw = (float) moistureNoise.noise(
                x / ClimateSettings.MOISTURE_SCALE,
                0,
                z / ClimateSettings.MOISTURE_SCALE);
        return Mth.clamp((raw + 1.0f) * 0.5f, 0.0f, 1.0f);
    }

    public float getWastelandNoise(int x, int z) {
        ensureInitialized();
        float raw = (float) wastelandNoise.noise(
                x / ClimateSettings.WASTELAND_SCALE,
                0,
                z / ClimateSettings.WASTELAND_SCALE);
        return Mth.clamp((raw + 1.0f) * 0.5f, 0.0f, 1.0f);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void ensureInitialized() {
        if (temperatureNoise == null) {
            setSeed(12345L);
        }
    }

    public boolean isTemperateDry(int x, int y, int z) {
        return getZone(x, y, z) == ClimateZone.TEMPERATE
                && getMoisture(x, z) < ClimateSettings.DRY_THRESHOLD;
    }

    public boolean isTemperateWet(int x, int y, int z) {
        return getZone(x, y, z) == ClimateZone.TEMPERATE
                && getMoisture(x, z) > ClimateSettings.WET_THRESHOLD;
    }
}
