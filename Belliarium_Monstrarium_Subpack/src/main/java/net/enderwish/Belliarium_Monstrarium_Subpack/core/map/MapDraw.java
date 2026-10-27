package net.enderwish.Belliarium_Monstrarium_Subpack.core.map;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * MapDraw
 *
 * One player-made annotation on the legendary map: an arrow, a circle,
 * or a text box. Stored per-world so drawings survive game sessions.
 */
public record MapDraw(
        UUID id,
        Type type,
        int centerX,
        int centerZ,
        float radius,
        String text,
        int color
) {

    public enum Type {
        ARROW,
        CIRCLE,
        TEXT
    }

    /** Serialize to NBT for persistence. */
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Id", id.toString());
        tag.putString("Type", type.name());
        tag.putInt("CX", centerX);
        tag.putInt("CZ", centerZ);
        tag.putFloat("Radius", radius);
        tag.putString("Text", text == null ? "" : text);
        tag.putInt("Color", color);
        return tag;
    }

    /** Deserialize from NBT. */
    public static MapDraw fromTag(CompoundTag tag) {
        UUID id = UUID.fromString(tag.getString("Id"));
        Type type = Type.valueOf(tag.getString("Type"));
        int cx = tag.getInt("CX");
        int cz = tag.getInt("CZ");
        float radius = tag.getFloat("Radius");
        String text = tag.getString("Text");
        if (text.isEmpty()) text = null;
        int color = tag.getInt("Color");
        return new MapDraw(id, type, cx, cz, radius, text, color);
    }

    /** The world position of this drawing as a BlockPos (y = 0, maps live on the ground plane). */
    public BlockPos pos() {
        return new BlockPos(centerX, 0, centerZ);
    }

    /** Default arrow color: loot-marker red. */
    public static final int COLOR_ARROW = 0xFFFF4444;
    /** Default circle color: danger-zone orange. */
    public static final int COLOR_CIRCLE = 0xFFFF8800;
    /** Default text color: ink black. */
    public static final int COLOR_TEXT = 0xFF3A2A14;

    public static MapDraw arrow(int x, int z, float radius) {
        return new MapDraw(UUID.randomUUID(), Type.ARROW, x, z, radius, null, COLOR_ARROW);
    }

    public static MapDraw circle(int x, int z, float radius) {
        return new MapDraw(UUID.randomUUID(), Type.CIRCLE, x, z, radius, null, COLOR_CIRCLE);
    }

    public static MapDraw text(int x, int z, float radius, String text) {
        return new MapDraw(UUID.randomUUID(), Type.TEXT, x, z, radius, text, COLOR_TEXT);
    }
}
