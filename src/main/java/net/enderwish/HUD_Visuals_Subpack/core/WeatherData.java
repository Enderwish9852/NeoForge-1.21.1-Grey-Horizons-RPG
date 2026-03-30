package net.enderwish.HUD_Visuals_Subpack.core;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Standardized record for syncing weather.
 * Now uses 4 components: type, duration (ticks), intensity, and temperature offset.
 */
public record WeatherData(WeatherType type, int ticksRemaining, float intensity, float tempOffset) {

    /**
     * The StreamCodec used by NeoForge to sync this record over the network.
     * Updated to include the tempOffset component.
     */
    public static final StreamCodec<ByteBuf, WeatherData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.idMapper(id -> WeatherType.values()[id], WeatherType::ordinal), WeatherData::type,
            ByteBufCodecs.VAR_INT, WeatherData::ticksRemaining,
            ByteBufCodecs.FLOAT, WeatherData::intensity,
            ByteBufCodecs.FLOAT, WeatherData::tempOffset, // Added the 4th component here
            WeatherData::new
    );

    /**
     * Manual encoding for FriendlyByteBuf if needed.
     */
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.type);
        buffer.writeInt(this.ticksRemaining);
        buffer.writeFloat(this.intensity);
        buffer.writeFloat(this.tempOffset); // Added tempOffset to manual encoding
    }

    /**
     * Manual decoding from FriendlyByteBuf if needed.
     */
    public static WeatherData decode(FriendlyByteBuf buffer) {
        return new WeatherData(
                buffer.readEnum(WeatherType.class),
                buffer.readInt(),
                buffer.readFloat(),
                buffer.readFloat() // Added tempOffset to manual decoding
        );
    }
}