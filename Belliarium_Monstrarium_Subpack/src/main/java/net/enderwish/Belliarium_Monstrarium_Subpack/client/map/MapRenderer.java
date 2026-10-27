package net.enderwish.Belliarium_Monstrarium_Subpack.client.map;

import net.enderwish.Belliarium_Monstrarium_Subpack.core.map.MapWorldData;
import net.enderwish.Frontier_Subpack.FrontierConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

/**
 * MapRenderer
 *
 * Xaero-style top-down terrain renderer. Each screen pixel samples the
 * top-most solid block at its world position and draws the biome color,
 * so the map reads as a smooth terrain view rather than chunky flat
 * colours.
 *
 * Zoom levels: 0 (full range) to 4 (max detail). Middle mouse wheel
 * changes the zoom; middle-mouse drag pans the map.
 *
 * Only explored chunks are drawn — unexplored areas stay dark.
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

    /** Pan the map center by the given world-space delta. */
    public void pan(int dx, int dz) {
        centerX += dx;
        centerZ += dz;
    }

    public int getZoomLevel() {
        return zoomLevel;
    }

    /** Current screen-pixels-per-block scale (used for panning input conversion). */
    public float currentScale(int screenW, int screenH) {
        return Math.min(screenW, screenH) / (float) visibleRange();
    }

    public void zoomIn() {
        if (zoomLevel < ZOOM_LEVELS - 1) zoomLevel++;
    }

    public void zoomOut() {
        if (zoomLevel > 0) zoomLevel--;
    }

    /** Zoom in while keeping the given world position at screen center. */
    public void zoomInAt(int worldX, int worldZ) {
        zoomIn();
        setCenter(worldX, worldZ);
    }

    /** Zoom out while keeping the given world position at screen center. */
    public void zoomOutAt(int worldX, int worldZ) {
        zoomOut();
        setCenter(worldX, worldZ);
    }

    /** Render the map into the given screen area. */
    public void render(GuiGraphics graphics, int screenW, int screenH, Level level) {
        int range = visibleRange();
        int halfRange = range / 2;
        int left = centerX - halfRange;
        int top = centerZ - halfRange;

        // Screen pixels per world block at this zoom.
        float scale = Math.min(screenW, screenH) / (float) range;

        int startX = (int) (left * scale);
        int startZ = (int) (top * scale);
        int right = startX + (int) (range * scale);
        int bottom = startZ + (int) (range * scale);

        // At low zoom every chunk is sub-pixel; fill with flat unexplored
        // colour instead of sampling millions of individual blocks.
        if (scale < 0.02f) { // less than 1 block per 50 px
            graphics.fill(startX, startZ, right, bottom, 0xFF2A2A2A);
            drawBorder(graphics, startX, startZ, right, bottom);
            return;
        }

        // Sample each screen pixel: convert to world XZ, find top solid
        // block, draw its biome colour. Only for explored chunks.
        for (int sx = startX; sx < right; sx++) {
            int worldX = left + (int) (sx / scale);
            for (int sz = startZ; sz < bottom; sz++) {
                int worldZ = top + (int) (sz / scale);

                int cx = worldX >> 4;
                int cz = worldZ >> 4;
                if (!MapWorldData.get(level).isExplored(cx, cz)) {
                    continue; // leave parchment showing through
                }

                int color = samplePixel(level, worldX, worldZ);
                graphics.fill(sx, sz, sx + 1, sz + 1, color);
            }
        }

        drawBorder(graphics, startX, startZ, right, bottom);
    }

    /** Sample one pixel: top-most solid block's biome colour, or fluid if on top. */
    private int samplePixel(Level level, int worldX, int worldZ) {
        int topY = BiomeColors.topSolidHeight(level, worldX, worldZ);

        // If there's a fluid at the surface, show its colour instead.
        int fluid = BiomeColors.fluidColorAt(level, worldX, topY, worldZ);
        if (fluid != 0) return fluid;

        // Otherwise draw the biome colour of the top block.
        Holder<Biome> biome = level.getBiome(new BlockPos(worldX, topY, worldZ));
        return BiomeColors.colorFor(biome);
    }

    private void drawBorder(GuiGraphics graphics, int startX, int startZ, int right, int bottom) {
        graphics.fill(startX, startZ, right, startZ + 1, 0xFF4A3F35);
        graphics.fill(startX, bottom - 1, right, bottom, 0xFF4A3F35);
        graphics.fill(startX, startZ, startX + 1, bottom, 0xFF4A3F35);
        graphics.fill(right - 1, startZ, right, bottom, 0xFF4A3F35);
    }

    /** Convert a world block position to screen x. */
    public int worldToScreenX(int worldX) {
        int range = visibleRange();
        int left = centerX - range / 2;
        int screenW = Minecraft.getInstance().getWindow().getWidth();
        float scale = Math.min(screenW, Minecraft.getInstance().getWindow().getHeight()) / (float) range;
        return (int) ((worldX - left) * scale);
    }

    /** Convert a world block position to screen z. */
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
