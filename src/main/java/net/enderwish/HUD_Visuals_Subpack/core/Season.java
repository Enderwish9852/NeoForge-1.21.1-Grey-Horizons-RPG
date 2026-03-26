package net.enderwish.HUD_Visuals_Subpack.core;

import com.mojang.serialization.Codec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/**
 * Defines the four seasons for the HUD & Visuals Subpack.
 * Includes color data for world rendering and temperature metadata for the Sports Watch.
 * Updated for 1.21.1 with StreamCodec for networking.
 */
public enum Season implements StringRepresentable {
    SPRING("spring", 0x7DB232, 15.0f, 25.0f),
    SUMMER("summer", 0x4B9E1E, 25.0f, 38.0f),
    AUTUMN("autumn", 0xBF8D2C, 5.0f, 15.0f),
    WINTER("winter", 0x729990, -15.0f, 5.0f);

    public static final Codec<Season> CODEC = StringRepresentable.fromEnum(Season::values);

    // This resolves the "Cannot resolve symbol 'STREAM_CODEC'" error in SeasonSyncPacket
    public static final StreamCodec<FriendlyByteBuf, Season> STREAM_CODEC = StreamCodec.of(
            (buf, season) -> buf.writeEnum(season),
            buf -> buf.readEnum(Season.class)
    );

    private final String name;
    private final int foliageColor;
    private final float minTemp;
    private final float maxTemp;

    Season(String name, int foliageColor, float minTemp, float maxTemp) {
        this.name = name;
        this.foliageColor = foliageColor;
        this.minTemp = minTemp;
        this.maxTemp = maxTemp;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    /**
     * @return The hex color used for foliage and grass during this season.
     */
    public int getFoliageColor() {
        return foliageColor;
    }

    /**
     * @return Base minimum temperature for HUD display.
     */
    public float getMinTemp() {
        return minTemp;
    }

    /**
     * @return Base maximum temperature for HUD display.
     */
    public float getMaxTemp() {
        return maxTemp;
    }

    /**
     * Helper to cycle seasons.
     */
    public Season next() {
        return values()[(this.ordinal() + 1) % values().length];
    }
}