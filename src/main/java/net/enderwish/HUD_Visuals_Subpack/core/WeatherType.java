package net.enderwish.HUD_Visuals_Subpack.core;

import net.minecraft.util.StringRepresentable;
import com.mojang.serialization.Codec;

/**
 * Redefined Weather States.
 * Rain and Wind now use dynamic intensity instead of separate "Light/Heavy" variants.
 */
public enum WeatherType implements StringRepresentable {
    CLEAR("clear", 0.0f, 0.0f),
    CLOUDY("cloudy", 0.1f, -2.0f),
    FOG("fog", 0.3f, -3.0f),

    // Merged Variants (Intensity handled by WeatherManager)
    RAIN("rain", 0.0f, -8.0f),  // Base temp drop of -8 at max intensity
    WIND("wind", 0.0f, -6.0f),  // Base temp drop of -6 at max intensity
    THUNDER("thunder", 1.0f, -10.0f),

    // Spring Special (Max Intensity / 24k ticks)
    POLLEN_HAZE("pollen_haze", 1.0f, 1.0f),

    // Summer Specific
    HEATWAVE("heatwave", 1.0f, 12.0f),
    DROUGHT("drought", 0.8f, 5.0f),

    // Winter Specific
    SNOW("snow", 0.5f, -5.0f),
    BLIZZARD("blizzard", 1.0f, -12.0f),
    HAIL("hail", 0.8f, -10.0f),

    // Winter Specials (Max Intensity / 24k ticks)
    DIAMOND_DUST("diamond_dust", 1.0f, -20.0f),
    THAW("thaw", 1.0f, 10.0f);

    private final String name;
    private final float defaultIntensity;
    private final float maxTempModifier;

    WeatherType(String name, float defaultIntensity, float maxTempModifier) {
        this.name = name;
        this.defaultIntensity = defaultIntensity;
        this.maxTempModifier = maxTempModifier;
    }

    /**
     * Determines if this weather can naturally occur in a given season.
     */
    public boolean isAvailableIn(Season season) {
        return switch (season) {
            case SPRING -> matchAny(CLEAR, CLOUDY, FOG, RAIN, THUNDER, WIND, POLLEN_HAZE);
            case SUMMER -> matchAny(CLEAR, HEATWAVE, DROUGHT, RAIN, WIND);
            case AUTUMN -> matchAny(RAIN, FOG, CLOUDY, CLEAR, WIND, THUNDER);
            case WINTER -> matchAny(SNOW, BLIZZARD, HAIL, CLOUDY, RAIN, DIAMOND_DUST, THAW);
        };
    }

    /**
     * Rarity Weights.
     */
    public int getWeight(Season season) {
        if (!isAvailableIn(season)) return 0;

        return switch (season) {
            case SPRING -> (this == POLLEN_HAZE) ? 2 : 20;
            case SUMMER -> (matchAny(CLEAR, HEATWAVE, DROUGHT)) ? 25 : 10;
            case AUTUMN -> (this == THUNDER) ? 5 : 25;
            case WINTER -> (matchAny(DIAMOND_DUST, THAW)) ? 2 : 20;
        };
    }

    /**
     * Helper to identify "Special" weathers that force max duration and intensity.
     */
    public boolean isSpecial() {
        return matchAny(POLLEN_HAZE, DIAMOND_DUST, THAW, HEATWAVE);
    }

    private boolean matchAny(WeatherType... types) {
        for (WeatherType t : types) { if (t == this) return true; }
        return false;
    }

    @Override public String getSerializedName() { return this.name; }
    public float getDefaultIntensity() { return defaultIntensity; }
    public float getMaxTempModifier() { return maxTempModifier; }

    public static final Codec<WeatherType> CODEC = StringRepresentable.fromEnum(WeatherType::values);
}