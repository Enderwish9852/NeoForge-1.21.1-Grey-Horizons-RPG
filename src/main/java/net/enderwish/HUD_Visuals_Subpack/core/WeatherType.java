package net.enderwish.HUD_Visuals_Subpack.core;

import net.minecraft.util.StringRepresentable;
import com.mojang.serialization.Codec;

/**
 * Defines all possible weather states and their environmental impacts.
 * Data based on "Hud & visuals subpack (alpha test).docx" specifications.
 */
public enum WeatherType implements StringRepresentable {
    CLEAR("clear", 0.0f, 0.0f),
    CLOUDY("cloudy", 0.1f, -2.0f),
    FOG("fog", 0.3f, -3.0f),
    LIGHT_RAIN("light_rain", 0.4f, -4.0f),
    HEAVY_RAIN("heavy_rain", 0.8f, -8.0f),
    THUNDER("thunder", 1.0f, -8.0f),
    SMALL_WIND("small_wind", 0.2f, -3.0f),
    STRONG_WIND("strong_wind", 0.5f, -5.0f),

    // Spring Special
    POLLEN_HAZE("pollen_haze", 0.4f, 1.0f),

    // Summer Specific
    HEATWAVE("heatwave", 0.9f, 10.0f),
    DROUGHT("drought", 0.7f, 10.0f),

    // Winter Specific
    SNOW("snow", 0.5f, -4.0f),
    SNOW_STORM("snow_storm", 0.8f, -6.0f),
    BLIZZARD("blizzard", 1.0f, -10.0f),
    HAIL("hail", 0.7f, -8.0f),
    DIAMOND_DUST("diamond_dust", 0.3f, -20.0f),
    THAW("thaw", 0.2f, 10.0f);

    private final String name;
    private final float intensity;
    private final float tempModifier;

    WeatherType(String name, float intensity, float tempModifier) {
        this.name = name;
        this.intensity = intensity;
        this.tempModifier = tempModifier;
    }

    /**
     * Determines if this weather can naturally occur in a given season.
     */
    public boolean isAvailableIn(Season season) {
        return switch (season) {
            case SPRING -> matchAny(CLEAR, CLOUDY, FOG, LIGHT_RAIN, HEAVY_RAIN, THUNDER, SMALL_WIND, STRONG_WIND, POLLEN_HAZE);
            case SUMMER -> matchAny(CLEAR, HEATWAVE, DROUGHT, LIGHT_RAIN, HEAVY_RAIN, SMALL_WIND);
            case AUTUMN -> matchAny(LIGHT_RAIN, HEAVY_RAIN, FOG, CLOUDY, CLEAR, SMALL_WIND, STRONG_WIND, THUNDER);
            case WINTER -> matchAny(SNOW, SNOW_STORM, BLIZZARD, HAIL, CLOUDY, HEAVY_RAIN, DIAMOND_DUST, THAW);
        };
    }

    /**
     * Returns the rarity weight for this weather in a specific season.
     * Higher = More common. 0 = Never happens.
     */
    public int getWeight(Season season) {
        if (!isAvailableIn(season)) return 0;

        return switch (season) {
            case SPRING -> (this == POLLEN_HAZE) ? 5 : 20; // Special is rare (5), others common (20)
            case SUMMER -> (matchAny(CLEAR, HEATWAVE, DROUGHT)) ? 20 : 10; // Heat is common
            case AUTUMN -> (this == THUNDER) ? 5 : 20; // Thunder uncommon in autumn
            case WINTER -> (matchAny(DIAMOND_DUST, THAW)) ? 2 : (matchAny(CLOUDY, HEAVY_RAIN)) ? 10 : 20;
        };
    }

    private boolean matchAny(WeatherType... types) {
        for (WeatherType t : types) { if (t == this) return true; }
        return false;
    }

    @Override public String getSerializedName() { return this.name; }
    public float getIntensity() { return intensity; }
    public float getTempModifier() { return tempModifier; }

    public static final Codec<WeatherType> CODEC = StringRepresentable.fromEnum(WeatherType::values);
}