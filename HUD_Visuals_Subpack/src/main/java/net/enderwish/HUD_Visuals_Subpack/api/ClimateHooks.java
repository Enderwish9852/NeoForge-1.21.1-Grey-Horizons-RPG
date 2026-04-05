package net.enderwish.HUD_Visuals_Subpack.api;

import net.enderwish.HUD_Visuals_Subpack.core.ModAttachments;
import net.enderwish.HUD_Visuals_Subpack.core.Season;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Handles specialized environmental logic for the "Thaw" cycle and Weather physics.
 * Updates are now tag-aware to match the new WeatherRegistry.
 */
public class ClimateHooks {

    /**
     * Determines if we are in the "Initial Thaw" (Spring Days 1-7).
     * This logic is used to preserve winter leftovers on first join.
     */
    public static boolean isThawingPeriod(Level level) {
        ClimateData data = getClimateData(level);
        if (data == null) return false;

        // Thaw logic only applies during the first week of Spring.
        // Once data.tempOffset() reaches 0.0 in WeatherManager, this naturally expires.
        return data.season() == Season.SPRING && data.day() <= 7 && data.tempOffset() < 0;
    }

    /**
     * SNOW PATCHES: Returns true for 15% of the world based on coordinates.
     */
    public static boolean isPosInSnowPatch(BlockPos pos) {
        long hash = (long)pos.getX() * 3121489L ^ (long)pos.getZ() * 1161297L;
        hash = hash * hash * 42317861L + hash * 11L;
        return (hash >> 16 & 100) < 15;
    }

    /**
     * NIGHT ICE: Returns true if it's currently night time during the Thaw period.
     */
    public static boolean isNightThaw(Level level) {
        if (!isThawingPeriod(level)) return false;
        long time = level.getDayTime() % 24000;
        return (time > 13000 && time < 23000);
    }

    /**
     * Standard check for winter or freezing weather.
     * Uses the new WeatherRegistry tags.
     */
    public static boolean isColdToFreeze(Level level) {
        ClimateData data = getClimateData(level);
        if (data == null) return false;

        WeatherType type = WeatherRegistry.getById(data.weather());

        // 1. ALWAYS freezing if the season is WINTER
        if (data.season() == Season.WINTER) return true;

        // 2. ALWAYS freezing if the weather type is specifically a freezing type (Blizzard, etc.)
        if (WeatherRegistry.is(type, WeatherRegistry.IS_FREEZING)) return true;

        // 3. SPECIAL CASE: Early Spring Thaw
        // This allows the leftover snow patches to exist even if the weather is "Clear"
        if (isThawingPeriod(level)) return true;

        // Otherwise, it's not freezing (so Rain will stay as Rain!)
        return false;
    }

    /**
     * Calculates Wind Vectors for particles and HUD elements.
     * Higher intensity = Stronger push.
     */
    public static Vec3 getWindVector(Level level) {
        ClimateData data = getClimateData(level);
        if (data == null) return new Vec3(0.04, 0, 0.02);

        WeatherType type = WeatherRegistry.getById(data.weather());
        float intensity = data.intensity();

        // If weather has wind or is a storm, apply dynamic intensity push
        if (WeatherRegistry.is(type, WeatherRegistry.HAS_WIND) || WeatherRegistry.is(type, WeatherRegistry.IS_STORM)) {
            // Horizontal push scaled by 0.3 - 1.0 intensity
            double windX = 0.65 * intensity;
            double windZ = 0.35 * intensity;
            // Slight downward "pressure" during heavy storms
            double windY = -0.02 * intensity;

            return new Vec3(windX, windY, windZ);
        }

        // Default calm breeze
        return new Vec3(0.04, 0, 0.02);
    }

    /**
     * Calculates temperature in Degrees Celsius for the Sports Watch.
     */
    public static float getTemperatureInDegrees(Level level, BlockPos pos) {
        ClimateData data = getClimateData(level);
        if (data == null) return 15.0f;

        float baseTemp = switch (data.season()) {
            case SPRING -> 15.0f;
            case SUMMER -> 31.5f;
            case AUTUMN -> 12.5f;
            case WINTER -> -5.0f;
        };

        // Diurnal (Day/Night) cycle factor (+/- 5 degrees)
        float timeFactor = (float) Math.cos(level.getTimeOfDay(1.0F) * 2 * Math.PI - Math.PI);

        // Final Temp = Base Season + Weather Offset + Time of Day
        return baseTemp + data.tempOffset() + (timeFactor * 5.0f);
    }

    /**
     * Internal helper to handle Client/Server data retrieval safely.
     */
    private static ClimateData getClimateData(Level level) {
        if (level == null) return null;
        return level.isClientSide ?
                net.enderwish.HUD_Visuals_Subpack.client.ClientClimateCache.get() :
                level.getData(ModAttachments.CLIMATE);
    }
}