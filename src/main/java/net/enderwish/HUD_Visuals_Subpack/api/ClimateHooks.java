package net.enderwish.HUD_Visuals_Subpack.api;

import net.enderwish.HUD_Visuals_Subpack.core.ModAttachments;
import net.enderwish.HUD_Visuals_Subpack.core.Season;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class ClimateHooks {
    public static final TagKey<WeatherType> IS_FREEZING = TagKey.create(WeatherRegistry.WEATHER_REGISTRY_KEY, ResourceLocation.fromNamespaceAndPath("gh_hud_visuals", "is_freezing"));
    public static final TagKey<WeatherType> HAS_WIND = TagKey.create(WeatherRegistry.WEATHER_REGISTRY_KEY, ResourceLocation.fromNamespaceAndPath("gh_hud_visuals", "has_wind"));

    /**
     * Clean check for freezing conditions using tags.
     */
    public static boolean isColdToFreeze(Level level) {
        ClimateData data = level.getData(ModAttachments.CLIMATE);
        WeatherType type = WeatherRegistry.getById(data.weather());
        return data.season() == Season.WINTER || WeatherRegistry.is(type, IS_FREEZING);
    }

    /**
     * Clean check for wind using tags.
     */
    public static Vec3 getWindVector(Level level) {
        ClimateData data = level.getData(ModAttachments.CLIMATE);
        WeatherType type = WeatherRegistry.getById(data.weather());
        if (WeatherRegistry.is(type, HAS_WIND)) {
            return new Vec3(0.6 * data.intensity(), -0.05, 0.3 * data.intensity());
        }
        return new Vec3(0.04, 0, 0.02);
    }

    /**
     * Master Temp Hook using Alpha Doc degree values. [cite: 1, 13]
     */
    public static float getTemperatureInDegrees(Level level, BlockPos pos) {
        ClimateData data = level.getData(ModAttachments.CLIMATE);
        float baseTemp = switch (data.season()) {
            case SPRING -> 15.0f; // Range: 10-20 [cite: 6]
            case SUMMER -> 31.5f; // Range: 25-38 [cite: 8]
            case AUTUMN -> 12.5f; // Range: 5-20 [cite: 10]
            case WINTER -> -5.0f; // Range: -15-5 [cite: 12]
        };

        float timeFactor = (float) Math.cos(level.getTimeOfDay(1.0F) * 2 * Math.PI - Math.PI);
        // data.tempOffset() now contains the exact degree changes like -20 or +10. [cite: 15, 16]
        return baseTemp + data.tempOffset() + (timeFactor * 5.0f);
    }
}