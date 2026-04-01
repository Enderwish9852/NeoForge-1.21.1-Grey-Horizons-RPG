package net.enderwish.HUD_Visuals_Subpack.core;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Standardized DTO for syncing weather data from Server to Client.
 */
public record WeatherData(WeatherType type, int ticksRemaining, float intensity, float tempOffset) {

    /**
     * Optimized StreamCodec for NeoForge 1.21.1.
     * Handles the transmission of the Enum ID, VarInt for ticks, and Floats for intensities.
     */
    public static final StreamCodec<ByteBuf, WeatherData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.idMapper(id -> {
                WeatherType[] values = WeatherType.values();
                return (id >= 0 && id < values.length) ? values[id] : WeatherType.CLEAR;
            }, WeatherType::ordinal), WeatherData::type,
            ByteBufCodecs.VAR_INT, WeatherData::ticksRemaining,
            ByteBufCodecs.FLOAT, WeatherData::intensity,
            ByteBufCodecs.FLOAT, WeatherData::tempOffset,
            WeatherData::new
    );
}