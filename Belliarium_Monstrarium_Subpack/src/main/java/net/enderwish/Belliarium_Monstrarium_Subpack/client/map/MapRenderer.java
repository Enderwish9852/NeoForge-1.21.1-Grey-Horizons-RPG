package net.enderwish.Belliarium_Monstrarium_Subpack.client.map;

import net.enderwish.Belliarium_Monstrarium_Subpack.core.map.MapWorldData;
import net.enderwish.Frontier_Subpack.FrontierConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

/**
 * MapRenderer
 *
 * Xaero-style top-down terrain renderer with 10 zoom levels (10% to 100%).
 * At 10% zoom the viewport shows 1000 blocks; at 100% zoom it shows 100
 * blocks. Each scroll step changes zoom by 10%.
 *
 * Explored chunks are drawn with biome colours — large flat colours at low
 * zoom (green for forests, blue for water), more detail and shading at high
 * zoom. Unexplored chunks are drawn as black fill with terrain edges.
 *
 * The map pans within a 50k×50k world (100k×100k with the Frontier
 * expand-storage setting).
 */
public class MapRenderer {

    /** Default map range: 50k * 50k blocks. */
    public static final int DEFAULT_RANGE = 50_000;
    /** Expanded map range with Frontier setting: 100k * 100k blocks. */
    public static final int EXPANDED_RANGE = 100_000;

    /** 10 zoom levels: 10%, 20%, ... 100%. */
    private static final int ZOOM_LEVELS = 10;
    /** Blocks shown at 10% zoom (most zoomed out). */
    private static final int MAX_VIEWPORT_BLOCKS = 1000;
    /** Blocks shown at 100% zoom (most zoomed in). */
    private static final int MIN_VIEWPORT_BLOCKS = 100;
    /** Blocks removed per zoom level step. */
    private static final int BLOCKS_PER_STEP = (MAX_VIEWPORT_BLOCKS - MIN_VIEWPORT_BLOCKS) / (ZOOM_LEVELS - 1);

    private static final int CHUNK_SIZE = 16;

    private int zoomLevel = 1; // 1 = 10%, 10 = 100%
    private int centerX = 0;
    private int centerZ = 0;

    /** Current visible range in blocks, based on zoom level. */
    public int visibleRange() {
        return MAX_VIEWPORT_BLOCKS - (zoomLevel - 1) * BLOCKS_PER_STEP;
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

    /** Current screen-pixels-per-block scale (used for panning input conversion). */
    public float currentScale(int screenW, int screenH) {
        return Math.max(screenW, screenH) / (float) visibleRange();
    }

    public int getZoomLevel() {
        return zoomLevel;
    }

    /** Zoom percentage (10 to 100). */
    public int getZoomPercent() {
        return zoomLevel * 10;
    }

    public void zoomIn() {
        if (zoomLevel < ZOOM_LEVELS) zoomLevel++;
    }

    public void zoomOut() {
        if (zoomLevel > 1) zoomLevel--;
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

    /** Render the map into the given screen area. Fills the entire screen. */
    public void render(GuiGraphics graphics, int screenW, int screenH, Level level) {
        int range = visibleRange();
        int halfRange = range / 2;
        int left = centerX - halfRange;
        int top = centerZ - halfRange;

        // Screen pixels per world block — use Math.max so the map fills the
        // entire screen on widescreen displays, no parchment showing through.
        float scale = Math.max(screenW, screenH) / (float) range;

        int startX = (int) (left * scale);
        int startZ = (int) (top * scale);
        int right = startX + (int) (range * scale);
        int bottom = startZ + (int) (range * scale);

        // Sample each screen pixel.
        for (int sx = startX; sx < right; sx++) {
            int worldX = left + (int) (sx / scale);
            for (int sz = startZ; sz < bottom; sz++) {
                int worldZ = top + (int) (sz / scale);

                int cx = worldX >> 4;
                int cz = worldZ >> 4;
                if (!MapWorldData.get(level).isExplored(cx, cz)) {
                    // Unexplored: black fill with terrain edges.
                    int color = sampleUnexploredPixel(level, worldX, worldZ, sx, sz, startX, startZ, scale, screenW, screenH);
                    graphics.fill(sx, sz, sx + 1, sz + 1, color);
                    continue;
                }

                int color = samplePixel(level, worldX, worldZ);
                graphics.fill(sx, sz, sx + 1, sz + 1, color);
            }
        }

        drawBorder(graphics, startX, startZ, right, bottom);
    }

    /** Sample one explored pixel: top-most solid block's biome colour, or fluid if on top. */
    private int samplePixel(Level level, int worldX, int worldZ) {
        int topY = BiomeColors.topSolidHeight(level, worldX, worldZ);

        // If there's a fluid at the surface, show its colour instead.
        int fluid = BiomeColors.fluidColorAt(level, worldX, topY, worldZ);
        if (fluid != 0) return fluid;

        // Otherwise draw the biome colour of the top block.
        Holder<Biome> biome = level.getBiome(new BlockPos(worldX, topY, worldZ));
        int color = BiomeColors.colorFor(biome);

        // At higher zoom levels, add height-based shading for detail.
        if (zoomLevel >= 6) {
            color = applyHeightShade(color, topY);
        }

        return color;
    }

    /** Sample one unexplored pixel: black fill with terrain edges. */
    private int sampleUnexploredPixel(Level level, int worldX, int worldZ, int sx, int sz,
                                      int startX, int startZ, float scale, int screenW, int screenH) {
        // Default: black fill.
        int color = 0xFF1A1A1A;

        // Check edges: sample neighbours to detect terrain transitions.
        // Water/land edges and biome edges.
        boolean isEdge = false;
        for (int dx = -1; dx <= 1 && !isEdge; dx++) {
            for (int dz = -1; dz <= 1 && !isEdge; dz++) {
                if (dx == 0 && dz == 0) continue;
                int nx = worldX + dx;
                int nz = worldZ + dz;
                int ncx = nx >> 4;
                int ncz = nz >> 4;
                boolean neighbourExplored = MapWorldData.get(level).isExplored(ncx, ncz);
                // Edge if neighbour is explored (different state) or different terrain type.
                if (neighbourExplored) {
                    isEdge = true;
                    break;
                }
                // Also edge if height changes significantly (terrain shape).
                int hereY = BiomeColors.topSolidHeight(level, worldX, worldZ);
                int thereY = BiomeColors.topSolidHeight(level, nx, nz);
                if (Math.abs(hereY - thereY) > 2) {
                    isEdge = true;
                    break;
                }
            }
        }

        if (isEdge) {
            color = 0xFF4A3F35; // dark brown edge line
        }

        return color;
    }

    /** Apply height-based shading: lighter for higher, darker for lower. */
    private int applyHeightShade(int color, int height) {
        // Sea level is 63. Shift shade based on height relative to sea level.
        float shift = (height - 63) * 0.015f;
        shift = Mth.clamp(shift, -0.3f, 0.3f);

        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        r = Mth.clamp((int) (r * (1.0f + shift)), 0, 255);
        g = Mth.clamp((int) (g * (1.0f + shift)), 0, 255);
        b = Mth.clamp((int) (b * (1.0f + shift)), 0, 255);

        return (0xFF << 24) | (r << 16) | (g << 8) | b;
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
        float scale = Math.max(screenW, Minecraft.getInstance().getWindow().getHeight()) / (float) range;
        return (int) ((worldX - left) * scale);
    }

    /** Convert a world block position to screen z. */
    public int worldToScreenZ(int worldZ) {
        int range = visibleRange();
        int top = centerZ - range / 2;
        int screenH = Minecraft.getInstance().getWindow().getHeight();
        float scale = Math.max(Minecraft.getInstance().getWindow().getWidth(), screenH) / (float) range;
        return (int) ((worldZ - top) * scale);
    }

    /** Convert a screen x coordinate back to world x. */
    public int screenToWorldX(int screenX) {
        int range = visibleRange();
        int left = centerX - range / 2;
        int screenW = Minecraft.getInstance().getWindow().getWidth();
        float scale = Math.max(screenW, Minecraft.getInstance().getWindow().getHeight()) / (float) range;
        return left + (int) (screenX / scale);
    }

    /** Convert a screen z coordinate back to world z. */
    public int screenToWorldZ(int screenZ) {
        int range = visibleRange();
        int top = centerZ - range / 2;
        int screenH = Minecraft.getInstance().getWindow().getHeight();
        float scale = Math.max(Minecraft.getInstance().getWindow().getWidth(), screenH) / (float) range;
        return top + (int) (screenZ / scale);
    }
}
