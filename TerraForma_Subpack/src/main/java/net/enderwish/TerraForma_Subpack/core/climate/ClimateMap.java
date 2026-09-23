package net.enderwish.TerraForma_Subpack.core.climate;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;

/**
 * ClimateMap
 *
 * Assigns a ClimateZone to any world position using:
 *   1. Temperature axis — Z coordinate + altitude modifier + local noise
 *   2. Moisture axis — independent Perlin noise
 *   3. Wasteland placement — separate noise for collapse epicentres
 *
 * BUGFIX (crash): world generation runs across MULTIPLE background
 * threads concurrently (confirmed from ChunkGenerator's own
 * createBiomes(), which dispatches via CompletableFuture.supplyAsync
 * on a shared background executor). ensureInitialized()/setSeed() were
 * previously unsynchronized plain field writes — one thread could see
 * temperatureNoise already set (so it assumes init is done and skips
 * setSeed()) while ANOTHER thread's setSeed() call hadn't yet reached
 * the wastelandNoise assignment, leaving wastelandNoise null for the
 * first thread. That's the exact NullPointerException that was
 * crashing world generation. Marking both methods `synchronized` on
 * this singleton closes the race: only one thread can be inside either
 * at a time, and the Java memory model guarantees that once a thread
 * acquires the lock, every field write made under that same lock by
 * whichever thread held it before is fully visible — the standard
 * "safe publication via synchronization" pattern.
 *
 * ALSO FIXED: the real world seed was never wired in — setSeed() only
 * ever ran via ensureInitialized()'s hardcoded fallback (12345L).
 * GHChunkGenerator now overrides createState(...) and calls
 * ClimateMap.INSTANCE.setSeed(seed) there with the real world seed.
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
     * Called from GHChunkGenerator.createState() with the real world seed.
     */
    public synchronized void setSeed(long seed) {
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

        float latitudeTemp = Mth.clamp(
                (float) z / ClimateSettings.TEMPERATURE_SCALE,
                -1.0f, 1.0f);

        float altitudeDrop = Math.max(0, y - ClimateSettings.SEA_LEVEL)
                * ClimateSettings.ALTITUDE_TEMP_DROP;

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

    private synchronized void ensureInitialized() {
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
