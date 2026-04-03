package net.enderwish.HUD_Visuals_Subpack.api;

import net.enderwish.HUD_Visuals_Subpack.core.Season;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * A clean data container for the current world state.
 * Updated to support degree-based temperature offsets and Tag-ready weather IDs.
 */
public record ClimateData(Season season, int day, String weather, float intensity, float tempOffset) {

    public static final Codec<ClimateData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Season.CODEC.fieldOf("season").forGetter(ClimateData::season),
                    Codec.INT.fieldOf("day").forGetter(ClimateData::day),
                    Codec.STRING.fieldOf("weather").forGetter(ClimateData::weather),
                    Codec.FLOAT.fieldOf("intensity").forGetter(ClimateData::intensity),
                    // Holds the degree offset (e.g., -20 for Diamond Dust) [cite: 15]
                    Codec.FLOAT.fieldOf("tempOffset").forGetter(ClimateData::tempOffset)
            ).apply(instance, ClimateData::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ClimateData> STREAM_CODEC = StreamCodec.composite(
            Season.STREAM_CODEC, ClimateData::season,
            ByteBufCodecs.VAR_INT, ClimateData::day,
            ByteBufCodecs.STRING_UTF8, ClimateData::weather,
            ByteBufCodecs.FLOAT, ClimateData::intensity,
            ByteBufCodecs.FLOAT, ClimateData::tempOffset,
            ClimateData::new
    );

    public static ClimateData getDefault() {
        // Matches your document: Starts in Spring, Day 1, Clear weather [cite: 4, 5]
        return new ClimateData(Season.SPRING, 1, "clear", 0.0f, 0.0f);
    }
}