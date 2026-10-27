package net.enderwish.Belliarium_Monstrarium_Subpack.client.map;

import net.enderwish.Belliarium_Monstrarium_Subpack.core.map.MapDraw;

/**
 * MapDrawTool
 *
 * The active drawing tool on the map toolbar. One of ARROW, CIRCLE, TEXT,
 * or NONE when no tool is selected.
 */
public enum MapDrawTool {
    NONE,
    ARROW(MapDraw.Type.ARROW, 0xFFFF4444, "Arrow"),
    CIRCLE(MapDraw.Type.CIRCLE, 0xFFFF8800, "Circle"),
    TEXT(MapDraw.Type.TEXT, 0xFF3A2A14, "Text");

    public final MapDraw.Type drawType;
    public final int color;
    public final String label;

    MapDrawTool(MapDraw.Type drawType, int color, String label) {
        this.drawType = drawType;
        this.color = color;
        this.label = label;
    }

    MapDrawTool() {
        this.drawType = null;
        this.color = 0;
        this.label = null;
    }
}
