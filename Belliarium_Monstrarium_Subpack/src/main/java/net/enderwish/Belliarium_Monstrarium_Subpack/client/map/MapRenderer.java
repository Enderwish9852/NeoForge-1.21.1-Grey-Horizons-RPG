package net.enderwish.Belliarium_Monstrarium_Subpack.client.map;

import net.enderwish.Belliarium_Monstrarium_Subpack.core.map.MapWorldData;
import net.enderwish.Frontier_Subpack.FrontierConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.level.Level;

/**
 * MapRenderer
 *
 * Renders the legendary map with zoom and explored / unexplored areas.
 *
 * Zoom levels: 0 (full range) to 4 (max detail). Middle mouse wheel
 * changes the zoom. Each zoom level halves the visible area and doubles
 * the detail.
 *
 * Explored areas are drawn with terrain contours (height lines, water).
 * Unexplored areas are black-and-white pencil outlines.
 */
public class MapRenderer {

    /** Default map range: 50k * 50k blocks. */
    public static final int DEFAULT_RANGE = 50_000;
    /** Expanded map range with Frontier setting: 100k * 100k blocks. */
    public static final int EXPANDED_RANGE = 100_000;

    private static final int ZOOM_LEVELS = 5;
    private static final int CHUNK_SIZE = 16;

    private int zoomLevel = 0;
    private int centerX = 0;
    private int centerZ = 0;

    /** Current visible range in blocks, based on zoom. */
    public int visibleRange() {
        int range = mapRange();
        return range >> zoomLevel; // halve per zoom level
    }

    /** Map range: 100k if Frontier expand-storage is on, 50k otherwise. */
    public int mapRange() {
        return FrontierConfig.MAP_EXPANDED_STORAGE.get() ? EXPANDED_RANGE : DEFAULT_RANGE;
    }

    public void setCenter(int x, int z) {
        this.centerX = x;
        this.centerZ = z;
    }

    public int getZoomLevel() {
        return zoomLevel;
    }

    public void zoomIn() {
        if (zoomLevel < ZOOM_LEVELS - 1) zoomLevel++;
    }

    public void zoomOut() {
        if (zoomLevel > 0) zoomLevel--;
    }

    /** Render the map into the given screen area. */
    public void render(GuiGraphics graphics, int screenW, int screenH, Level level) {
        int range = visibleRange();
        int halfRange = range / 2;
        int left = centerX - halfRange;
        int top = centerZ - halfRange;

        // How many screen pixels per block.
        float scale = Math.min(screenW, screenH) / (float) range;
        float chunkScreenW = 16.0f * scale;
        float chunkScreenH = 16.0f * scale;

        int startX = (int) (left * scale);
        int startZ = (int) (top * scale);

        // At full zoom out every chunk is sub-pixel; draw just a thin border
        // so we never loop millions of times and never fill the whole screen.
        if (chunkScreenW < 1.0f && chunkScreenH < 1.0f) {
            int right = startX + (int) (range * scale);
            int bottom = startZ + (int) (range * scale);
            // Top and bottom edges.
            graphics.fill(startX, startZ, right, startZ + 1, 0xFF4A3F35);
            graphics.fill(startX, bottom - 1, right, bottom, 0xFF4A3F35);
            // Left and right edges.
            graphics.fill(startX, startZ, startX + 1, bottom, 0xFF4A3F35);
            graphics.fill(right - 1, startZ, right, bottom, 0xFF4A3F35);
            return;
        }

        // Only iterate over chunks that intersect the visible range.
        // Math.floorDiv handles negative coordinates correctly.
        int startCX = Math.floorDiv(left, CHUNK_SIZE);
        int endCX = Math.floorDiv(left + range - 1, CHUNK_SIZE);
        int startCZ = Math.floorDiv(top, CHUNK_SIZE);
        int endCZ = Math.floorDiv(top + range - 1, CHUNK_SIZE);

        for (int cz = startCZ; cz <= endCZ; cz++) {
            for (int cx = startCX; cx <= endCX; cx++) {
                renderChunk(graphics, level, cx, cz, scale, left, top);
            }
        }

        // Draw the range border as thin lines, not a filled rectangle.
        int right = startX + (int) (range * scale);
        int bottom = startZ + (int) (range * scale);
        graphics.fill(startX, startZ, right, startZ + 1, 0xFF4A3F35);
        graphics.fill(startX, bottom - 1, right, bottom, 0xFF4A3F35);
        graphics.fill(startX, startZ, startX + 1, bottom, 0xFF4A3F35);
        graphics.fill(right - 1, startZ, right, bottom, 0xFF4A3F35);
    }

    private void renderChunk(GuiGraphics graphics, Level level, int chunkX, int chunkZ,
                             float scale, int left, int top) {
        int screenX = (int) ((chunkX * 16L - left) * scale);
        int screenZ = (int) ((chunkZ * 16L - top) * scale);
        int chunkScreenW = Math.max(1, (int) (16 * scale));
        int chunkScreenH = Math.max(1, (int) (16 * scale));

        boolean explored = MapWorldData.get(level).isExplored(chunkX, chunkZ);

        if (explored) {
            // Sample the terrain height at the chunk center.
            int height = MapWorldData.get(level).getSurfaceHeight(chunkX, chunkZ);

            // Pencil-sketch style: earth tone for land.
            int color = terrainColor(height);
            graphics.fill(screenX, screenZ, screenX + chunkScreenW, screenZ + chunkScreenH, color);

            // Draw a contour line based on height.
            if (scale >= 0.5f) {
                graphics.fill(screenX, screenZ, screenX + 1, screenZ + chunkScreenH, 0xFF2E2218);
                graphics.fill(screenX, screenZ, screenX + chunkScreenW, screenZ + 1, 0xFF2E2218);
            }
        } else {
            // Unexplored: black-and-white pencil outline.
            graphics.fill(screenX, screenZ, screenX + chunkScreenW, screenZ + chunkScreenH, 0xFF2A2A2A);
            if (chunkScreenW > 2 && chunkScreenH > 2) {
                graphics.fill(screenX + 1, screenZ + 1, screenX + chunkScreenW - 1, screenZ + chunkScreenH - 1, 0xFF1A1A1A);
            }
        }
    }

    /** Earth-tone pencil color based on terrain height. */
    private int terrainColor(int height) {
        if (height < 63) return 0xFF5C6B73; // shallow water
        if (height < 70) return 0xFF7A8B6A; // low land
        if (height < 90) return 0xFF8B9A7B; // hills
        if (height < 120) return 0xFF6B7A6A; // high hills
        return 0xFF5A6A5A; // mountains
    }

    /** Convert a world block position to screen coordinates. */
    public int worldToScreenX(int worldX) {
        int range = visibleRange();
        int left = centerX - range / 2;
        int screenW = Minecraft.getInstance().getWindow().getWidth();
        float scale = Math.min(screenW, Minecraft.getInstance().getWindow().getHeight()) / (float) range;
        return (int) ((worldX - left) * scale);
    }

    public int worldToScreenZ(int worldZ) {
        int range = visibleRange();
        int top = centerZ - range / 2;
        int screenH = Minecraft.getInstance().getWindow().getHeight();
        float scale = Math.min(Minecraft.getInstance().getWindow().getWidth(), screenH) / (float) range;
        return (int) ((worldZ - top) * scale);
    }

    /** Convert a screen x coordinate back to world x. */
    public int screenToWorldX(int screenX) {
        int range = visibleRange();
        int left = centerX - range / 2;
        int screenW = Minecraft.getInstance().getWindow().getWidth();
        float scale = Math.min(screenW, Minecraft.getInstance().getWindow().getHeight()) / (float) range;
        return left + (int) (screenX / scale);
    }

    /** Convert a screen z coordinate back to world z. */
    public int screenToWorldZ(int screenZ) {
        int range = visibleRange();
        int top = centerZ - range / 2;
        int screenH = Minecraft.getInstance().getWindow().getHeight();
        float scale = Math.min(Minecraft.getInstance().getWindow().getWidth(), screenH) / (float) range;
        return top + (int) (screenZ / scale);
    }
}
