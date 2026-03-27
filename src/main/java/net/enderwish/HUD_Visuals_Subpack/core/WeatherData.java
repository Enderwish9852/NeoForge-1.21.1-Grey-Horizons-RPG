package net.enderwish.HUD_Visuals_Subpack.core;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * Representing weather state.
 * Using 'duration' as the field name to match your logic,
 * but ensuring it's in the 2nd position for constructor consistency.
 */
public record WeatherData(WeatherType type, int duration, float intensity) {

    public static final StreamCodec<FriendlyByteBuf, WeatherData> STREAM_CODEC = StreamCodec.of(
            (buf, val) -> {
                WeatherType.STREAM_CODEC.encode(buf, val.type());
                buf.writeInt(val.duration());
                buf.writeFloat(val.intensity());
            },
            buf -> new WeatherData(
                    WeatherType.STREAM_CODEC.decode(buf),
                    buf.readInt(),
                    buf.readFloat()
            )
    );

    public static WeatherData defaultClear() {
        return new WeatherData(WeatherType.CLEAR, 0, 0.0f);
    }

    // Alias for ticksRemaining if you prefer that name in other classes
    public int ticksRemaining() {
        return duration;
    }
}