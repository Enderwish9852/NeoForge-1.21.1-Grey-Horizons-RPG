package net.enderwish.Atmospheric_Overhaul_Subpack.client.sky;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.util.Mth;

/**
 * CloudMesher
 *
 * NEW -- builds one 128x128-block tile of the voxel cloud field on the
 * worker thread (pure CPU, no GL calls): a 32x24x32 lattice of 4-block
 * cells sampled from CloudDensityField in ABSOLUTE cloud coordinates,
 * so neighbouring tiles agree cell-for-cell along their shared border
 * (the old tile-local sampling and per-cell jitter drew the seam grid).
 *
 * Faces are emitted only next to a below-threshold cell -- no coplanar
 * duplicate quads, so no z-fighting and no doubled-alpha seams, and
 * interior faces are skipped entirely. Softness is baked PER VERTEX:
 * each face corner's alpha is scaled by the mean density of the four
 * cells sharing that corner, so cloud rims thin out and read as
 * rounded puffs rather than voxel blocks. Per-face grey shading (top
 * bright, bottom dark, sides between) is baked here; the time-of-day
 * tint is applied per frame through ColorModulator, so dusk does not
 * remesh anything.
 *
 * Vertex positions are TILE-LOCAL (x,z in [0,128), y in [0,96) above
 * CloudDensityField.LAYER_BOTTOM); the per-tile ModelOffset uniform
 * places them in the world at draw time.
 */
final class CloudMesher {

    private CloudMesher() {}

    static final int TILE_BLOCKS = 128;
    static final int CELLS_XZ = 32;                    // 4-block cells across
    static final int CELLS_Y = 24;                     // 4-block cells up (96 blocks of layer)
    static final double CELL = TILE_BLOCKS / (double) CELLS_XZ;

    /** density above which a cell counts as solid cloud */
    static final double THRESHOLD = 0.42;

    private static final double CORE_DENSITY = 0.80;  // density that maps to full face alpha
    private static final double SOFT_WINDOW = 0.24;   // corner-mean span for rim softening

    /** Builds the tile mesh, or null when the tile is clear sky. */
    static MeshData buildTile(long seed, int tileX, int tileZ, double rain) {
        double[][][] grid = new double[CELLS_XZ][CELLS_Y][CELLS_XZ];
        double ox = tileX * (double) TILE_BLOCKS;
        double oz = tileZ * (double) TILE_BLOCKS;

        boolean anyCloud = false;
        for (int cx = 0; cx < CELLS_XZ; cx++) {
            for (int cy = 0; cy < CELLS_Y; cy++) {
                for (int cz = 0; cz < CELLS_XZ; cz++) {
                    double d = CloudDensityField.sample(seed,
                            ox + (cx + 0.5) * CELL,
                            CloudDensityField.LAYER_BOTTOM + (cy + 0.5) * CELL,
                            oz + (cz + 0.5) * CELL,
                            rain);
                    grid[cx][cy][cz] = d;
                    if (d > THRESHOLD) anyCloud = true;
                }
            }
        }
        if (!anyCloud) return null;

        // worker thread: private memory (the Tesselator singleton is
        // main-thread-only); 1.21.1 takes the mode+format at construction
        ByteBufferBuilder memory = new ByteBufferBuilder(1 << 18);
        BufferBuilder buf = new BufferBuilder(memory, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (int cx = 0; cx < CELLS_XZ; cx++) {
            for (int cy = 0; cy < CELLS_Y; cy++) {
                for (int cz = 0; cz < CELLS_XZ; cz++) {
                    double d = grid[cx][cy][cz];
                    if (d <= THRESHOLD) continue;
                    emitCell(buf, grid, seed, ox, oz, cx, cy, cz, d, rain);
                }
            }
        }

        return buf.build();   // anyCloud guarantees non-null
    }

    // ── Cell emission ──────────────────────────────────────────────────

    private static void emitCell(BufferBuilder buf, double[][][] grid, long seed, double ox, double oz,
                                 int cx, int cy, int cz, double d, double rain) {
        float x0 = (float) (cx * CELL), x1 = (float) ((cx + 1) * CELL);
        float y0 = (float) (cy * CELL), y1 = (float) ((cy + 1) * CELL);
        float z0 = (float) (cz * CELL), z1 = (float) ((cz + 1) * CELL);
        float core = faceAlpha(d);

        // +Y (top): brightest, sky-lit. Corners read the 4 cells around
        // them in THIS layer (the layer above is empty -- that is why the
        // face exists).
        if (!solid(grid, seed, ox, oz, cx, cy + 1, cz, rain)) {
            quad(buf, 1.00f, core,
                    fadeXZ(grid, seed, ox, oz, cx - 1, cx, cy, cz - 1, cz, rain),
                    fadeXZ(grid, seed, ox, oz, cx, cx + 1, cy, cz - 1, cz, rain),
                    fadeXZ(grid, seed, ox, oz, cx, cx + 1, cy, cz, cz + 1, rain),
                    fadeXZ(grid, seed, ox, oz, cx - 1, cx, cy, cz, cz + 1, rain),
                    x0, y1, z0, x1, y1, z1, Plane.Y);
        }
        // -Y (base): shaded underside (same in-plane corner cells)
        if (!solid(grid, seed, ox, oz, cx, cy - 1, cz, rain)) {
            quad(buf, 0.56f, core,
                    fadeXZ(grid, seed, ox, oz, cx - 1, cx, cy, cz - 1, cz, rain),
                    fadeXZ(grid, seed, ox, oz, cx, cx + 1, cy, cz - 1, cz, rain),
                    fadeXZ(grid, seed, ox, oz, cx, cx + 1, cy, cz, cz + 1, rain),
                    fadeXZ(grid, seed, ox, oz, cx - 1, cx, cy, cz, cz + 1, rain),
                    x0, y0, z0, x1, y0, z1, Plane.Y);
        }
        // -Z (north): corners span (x, y) in this z-layer
        if (!solid(grid, seed, ox, oz, cx, cy, cz - 1, rain)) {
            quad(buf, 0.82f, core,
                    fadeXY(grid, seed, ox, oz, cx - 1, cx, cy - 1, cy, cz, rain),
                    fadeXY(grid, seed, ox, oz, cx, cx + 1, cy - 1, cy, cz, rain),
                    fadeXY(grid, seed, ox, oz, cx, cx + 1, cy, cy + 1, cz, rain),
                    fadeXY(grid, seed, ox, oz, cx - 1, cx, cy, cy + 1, cz, rain),
                    x0, y0, z0, x1, y1, z0, Plane.NS);
        }
        // +Z (south)
        if (!solid(grid, seed, ox, oz, cx, cy, cz + 1, rain)) {
            quad(buf, 0.82f, core,
                    fadeXY(grid, seed, ox, oz, cx - 1, cx, cy - 1, cy, cz, rain),
                    fadeXY(grid, seed, ox, oz, cx, cx + 1, cy - 1, cy, cz, rain),
                    fadeXY(grid, seed, ox, oz, cx, cx + 1, cy, cy + 1, cz, rain),
                    fadeXY(grid, seed, ox, oz, cx - 1, cx, cy, cy + 1, cz, rain),
                    x0, y0, z1, x1, y1, z1, Plane.NS);
        }
        // -X (west): corners span (y, z) in this x-column
        if (!solid(grid, seed, ox, oz, cx - 1, cy, cz, rain)) {
            quad(buf, 0.72f, core,
                    fadeYZ(grid, seed, ox, oz, cy - 1, cy, cz - 1, cz, cx, rain),
                    fadeYZ(grid, seed, ox, oz, cy - 1, cy, cz, cz + 1, cx, rain),
                    fadeYZ(grid, seed, ox, oz, cy, cy + 1, cz, cz + 1, cx, rain),
                    fadeYZ(grid, seed, ox, oz, cy, cy + 1, cz - 1, cz, cx, rain),
                    x0, y0, z0, x0, y1, z1, Plane.WE);
        }
        // +X (east)
        if (!solid(grid, seed, ox, oz, cx + 1, cy, cz, rain)) {
            quad(buf, 0.72f, core,
                    fadeYZ(grid, seed, ox, oz, cy - 1, cy, cz - 1, cz, cx, rain),
                    fadeYZ(grid, seed, ox, oz, cy - 1, cy, cz, cz + 1, cx, rain),
                    fadeYZ(grid, seed, ox, oz, cy, cy + 1, cz, cz + 1, cx, rain),
                    fadeYZ(grid, seed, ox, oz, cy, cy + 1, cz - 1, cz, cx, rain),
                    x1, y0, z0, x1, y1, z1, Plane.WE);
        }
    }

    /**
     * Emits one quad: (x0,y0,z0)..(x1,y1,z1) are opposite corners (one
     * axis constant per Plane). Vertex order runs along the face's two
     * VARYING axes so the quad never collapses to a line; culling is
     * disabled at draw time, so winding is free. Alpha is core * fade
     * per corner -- the rim softening that rounds the puffs.
     */
    private static void quad(BufferBuilder buf, float shade, float core,
                             float f00, float f10, float f11, float f01,
                             float x0, float y0, float z0,
                             float x1, float y1, float z1, Plane plane) {
        switch (plane) {
            case Y -> {   // plane const Y: span X then Z
                buf.addVertex(x0, y0, z0).setColor(shade, shade, shade, core * f00);
                buf.addVertex(x1, y0, z0).setColor(shade, shade, shade, core * f10);
                buf.addVertex(x1, y0, z1).setColor(shade, shade, shade, core * f11);
                buf.addVertex(x0, y0, z1).setColor(shade, shade, shade, core * f01);
            }
            case NS -> {  // plane const Z: span X then Y
                buf.addVertex(x0, y0, z0).setColor(shade, shade, shade, core * f00);
                buf.addVertex(x1, y0, z0).setColor(shade, shade, shade, core * f10);
                buf.addVertex(x1, y1, z0).setColor(shade, shade, shade, core * f11);
                buf.addVertex(x0, y1, z0).setColor(shade, shade, shade, core * f01);
            }
            case WE -> {  // plane const X: span Z then Y
                buf.addVertex(x0, y0, z0).setColor(shade, shade, shade, core * f00);
                buf.addVertex(x0, y0, z1).setColor(shade, shade, shade, core * f10);
                buf.addVertex(x0, y1, z1).setColor(shade, shade, shade, core * f11);
                buf.addVertex(x0, y1, z0).setColor(shade, shade, shade, core * f01);
            }
        }
    }

    private enum Plane { Y, NS, WE }

    // ── Density access + alpha curves ───────────────────────────────────

    /** Grid lookup for in-tile cells; direct field sample across the border. */
    private static double dens(double[][][] grid, long seed, double ox, double oz,
                               int cx, int cy, int cz, double rain) {
        if (cx >= 0 && cx < CELLS_XZ && cy >= 0 && cy < CELLS_Y && cz >= 0 && cz < CELLS_XZ) {
            return grid[cx][cy][cz];
        }
        return CloudDensityField.sample(seed,
                ox + (cx + 0.5) * CELL,
                CloudDensityField.LAYER_BOTTOM + (cy + 0.5) * CELL,
                oz + (cz + 0.5) * CELL,
                rain);
    }

    private static boolean solid(double[][][] grid, long seed, double ox, double oz,
                                 int cx, int cy, int cz, double rain) {
        return dens(grid, seed, ox, oz, cx, cy, cz, rain) > THRESHOLD;
    }

    /** Corner fade for +Y/-Y faces: cells (xa..xb, y, za..zb) around the vertex. */
    private static float fadeXZ(double[][][] grid, long seed, double ox, double oz,
                                int xa, int xb, int y, int za, int zb, double rain) {
        double avg = (dens(grid, seed, ox, oz, xa, y, za, rain) + dens(grid, seed, ox, oz, xb, y, za, rain)
                + dens(grid, seed, ox, oz, xa, y, zb, rain) + dens(grid, seed, ox, oz, xb, y, zb, rain)) * 0.25;
        return softCurve(avg);
    }

    /** Corner fade for east/west faces: cells (x, ya..yb, za..zb) around the vertex. */
    private static float fadeYZ(double[][][] grid, long seed, double ox, double oz,
                                int ya, int yb, int za, int zb, int x, double rain) {
        double avg = (dens(grid, seed, ox, oz, x, ya, za, rain) + dens(grid, seed, ox, oz, x, yb, za, rain)
                + dens(grid, seed, ox, oz, x, ya, zb, rain) + dens(grid, seed, ox, oz, x, yb, zb, rain)) * 0.25;
        return softCurve(avg);
    }

    /** Corner fade for north/south faces: cells (xa..xb, ya..yb, z) around the vertex. */
    private static float fadeXY(double[][][] grid, long seed, double ox, double oz,
                                int xa, int xb, int ya, int yb, int z, double rain) {
        double avg = (dens(grid, seed, ox, oz, xa, ya, z, rain) + dens(grid, seed, ox, oz, xb, ya, z, rain)
                + dens(grid, seed, ox, oz, xa, yb, z, rain) + dens(grid, seed, ox, oz, xb, yb, z, rain)) * 0.25;
        return softCurve(avg);
    }

    private static float softCurve(double cornerMean) {
        double t = Mth.clamp((cornerMean - THRESHOLD) / SOFT_WINDOW, 0.0, 1.0);
        return (float) (0.22 + 0.78 * t);
    }

    /** Face core alpha: rim cells barely above threshold stay translucent. */
    private static float faceAlpha(double d) {
        double t = Mth.clamp((d - THRESHOLD) / (CORE_DENSITY - THRESHOLD), 0.0, 1.0);
        return (float) (0.32 + 0.58 * t);
    }
}
