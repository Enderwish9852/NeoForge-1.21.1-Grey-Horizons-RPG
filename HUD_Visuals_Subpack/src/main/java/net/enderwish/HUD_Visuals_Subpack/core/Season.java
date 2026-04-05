package net.enderwish.HUD_Visuals_Subpack.core;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import java.util.function.IntFunction;

/**
 * Optimized Season Enum for GH HUD Visuals.
 * Matches Alpha Test requirements for visual transitions.
 */
public enum Season implements StringRepresentable {
    SPRING("spring", 0x7DB232), // Vibrant Green
    SUMMER("summer", 0x4B9E1E), // Darker Forest Green
    AUTUMN("autumn", 0xBF8D2C), // Orange/Brown
    WINTER("winter", 0x729990); // Muted Teal/Grey

    private static final Season[] BY_ID = values();

    // Standard Codec for NBT/Data storage
    public static final Codec<Season> CODEC = StringRepresentable.fromEnum(Season::values);

    // StreamCodec for networking (Syncing Season to the Sports Watch)
    public static final StreamCodec<ByteBuf, Season> STREAM_CODEC = ByteBufCodecs.idMapper(
            (IntFunction<Season>) (id) -> BY_ID[Math.floorMod(id, BY_ID.length)],
            Season::ordinal
    );

    private final String name;
    private final int foliageColor;

    Season(String name, int foliageColor) {
        this.name = name;
        this.foliageColor = foliageColor;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public int getFoliageColor() {
        return foliageColor;
    }

    public Season next() {
        return BY_ID[(this.ordinal() + 1) % BY_ID.length];
    }

    public static Season getById(int id) {
        return BY_ID[Math.floorMod(id, BY_ID.length)];
    }
}