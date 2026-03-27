package net.enderwish.HUD_Visuals_Subpack.core;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import java.util.Locale;

/**
 * Defines the different types of weather available in the HUD Visuals Subpack.
 */
public enum WeatherType {
    CLEAR("Clear", 0.0f),
    LIGHT_RAIN("Light Rain", 0.3f),
    HEAVY_RAIN("Heavy Rain", 0.8f),
    THUNDERSTORM("Thunderstorm", 1.0f),
    SNOW("Snow", 0.5f),
    BLIZZARD("Blizzard", 1.0f),
    FOG("Fog", 0.2f),
    HEATWAVE("Heatwave", 0.0f);

    private final String displayName;
    private final float intensity;

    // This fixes the 'Cannot resolve symbol STREAM_CODEC' error seen in your SeasonSyncPacket screenshot
    public static final StreamCodec<FriendlyByteBuf, WeatherType> STREAM_CODEC = StreamCodec.of(
            (buf, val) -> buf.writeEnum(val),
            buf -> buf.readEnum(WeatherType.class)
    );

    WeatherType(String displayName, float intensity) {
        this.displayName = displayName;
        this.intensity = intensity;
    }

    public String getDisplayName() {
        return displayName;
    }

    public float getIntensity() {
        return intensity;
    }

    public static WeatherType fromString(String name) {
        try {
            return WeatherType.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return CLEAR;
        }
    }
}