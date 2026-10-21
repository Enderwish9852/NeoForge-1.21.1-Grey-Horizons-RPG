package net.enderwish.Atmospheric_Overhaul_Subpack.client.sky;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.enderwish.Atmospheric_Overhaul_Subpack.client.ClientSeasonState;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * CloudTileManager
 *
 * NEW -- the voxel cloud engine's tile system (Billow-style pipeline,
 * our own math and naming). 128-block tiles are meshed by ONE daemon
 * worker thread (CloudMesher, pure CPU), handed over through a
 * lock-free queue, uploaded on the render thread within a per-frame
 * budget, and drawn per tile with the gh_clouds shader:
 *
 * - the mesh lattice is anchored to CLOUD space (world minus wind
 *   drift), so the whole field advects smoothly without remeshing --
 *   only the grid of tiles scrolls as the camera (or the field) moves
 *   under it, new tiles mesh ahead, stale ones are evicted behind;
 * - ModelOffset places each tile CAMERA-RELATIVE (the mixin's
 *   frustumMatrix is rotation only), and the vertex shader derives
 *   fog distance from that same camera-relative position;
 * - fog fades clouds into the sky near the tile-range edge instead of
 *   popping; ColorModulator (RenderSystem shader color) carries the
 *   per-frame time-of-day/storm tint, so nothing remeshes at dusk;
 * - level swaps (dimension change, world exit) bump the epoch: worker
 *   results still in flight are dropped, GPU buffers are closed on the
 *   render thread, and the field reseeds from the new level.
 */
public final class CloudTileManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("GHAtmospheric/CloudTiles");

    public static final CloudTileManager INSTANCE = new CloudTileManager();

    private CloudTileManager() {}

    // ── Tuning (v1 constants; config exposure comes later) ──────────────

    private static final int UPLOAD_BUDGET = 3;          // tile uploads per frame
    private static final long DRAW_GRACE = 120;          // keep drawing tiles that just left range
    private static final long UNUSED_FRAME_GRACE = 600;  // evict tiles unseen this long
    private static final double DRIFT_BLOCKS_PER_TICK = 0.55;

    // ── Tile bookkeeping ─────────────────────────────────────────────────

    private final Map<Long, CloudTile> tiles = new ConcurrentHashMap<>();
    private final LinkedBlockingQueue<CloudTile> workQueue = new LinkedBlockingQueue<>();
    private final ConcurrentLinkedQueue<CloudTile> uploadQueue = new ConcurrentLinkedQueue<>();

    private Thread worker;
    private Level lastLevel;
    private volatile long seed;
    private volatile int epoch;
    private long frame;
    private double driftX, driftZ;
    private double lastDriftMillis;
    private boolean loggedFirstUpload;
    private boolean loggedFirstDraw;

    // ── Entry (from the facade, every frame) ─────────────────────────────

    void render(Level level, Matrix4f viewMatrix, Matrix4f projectionMatrix,
                double camX, double camY, double camZ) {
        ShaderInstance shader = CloudShaderRegistry.cloudShader;
        if (shader == null) return;   // not registered yet / failed reload

        frame++;
        if (level != lastLevel) resetFor(level);
        advanceDrift();

        Minecraft mc = Minecraft.getInstance();
        int rangeBlocks = Math.min(128, mc.options.getEffectiveRenderDistance() * 4) * 16;

        // cloud-space camera: the tile grid scrolls with the drift so the
        // field advects without ever remeshing what it already meshed
        double cloudCamX = camX - driftX;
        double cloudCamZ = camZ - driftZ;

        ensureTiles(cloudCamX, cloudCamZ, rangeBlocks);
        processUploads();
        drawTiles(level, shader, viewMatrix, projectionMatrix, camX, camY, camZ, rangeBlocks);
        evictStale();
    }

    // ── Tile lifecycle ───────────────────────────────────────────────────

    private void ensureTiles(double cloudCamX, double cloudCamZ, int rangeBlocks) {
        int centerTx = (int) Math.floor(cloudCamX / CloudMesher.TILE_BLOCKS);
        int centerTz = (int) Math.floor(cloudCamZ / CloudMesher.TILE_BLOCKS);
        int radius = (int) Math.ceil(rangeBlocks / (double) CloudMesher.TILE_BLOCKS);

        ensureWorker();
        for (int tx = centerTx - radius; tx <= centerTx + radius; tx++) {
            for (int tz = centerTz - radius; tz <= centerTz + radius; tz++) {
                final int tileX = tx, tileZ = tz;
                CloudTile tile = tiles.computeIfAbsent(CloudTile.packKey(tileX, tileZ), k -> new CloudTile(tileX, tileZ));
                tile.lastNeededFrame = frame;
                if (tile.hasMesh() || tile.queued) continue;
                tile.queued = true;
                tile.buildSeed = seed;
                tile.epoch = epoch;
                workQueue.add(tile);
            }
        }
    }

    private void ensureWorker() {
        if (worker != null && worker.isAlive()) return;
        synchronized (this) {
            if (worker == null || !worker.isAlive()) {
                worker = new Thread(this::workLoop, "GH-Cloud-Mesher");
                worker.setDaemon(true);
                worker.start();
            }
        }
    }

    /** Worker loop: mesh tiles (CPU only), hand the result to the render thread. */
    private void workLoop() {
        while (true) {
            CloudTile tile;
            try {
                tile = workQueue.take();
            } catch (InterruptedException e) {
                return;
            }
            if (tile.epoch != epoch || tile.buildSeed != seed) continue;   // stale enqueue
            tile.pendingMesh = CloudMesher.buildTile(tile.buildSeed, tile.tileX, tile.tileZ);
            uploadQueue.add(tile);
        }
    }

    /** Render thread: uploads finished meshes within the per-frame budget. */
    private void processUploads() {
        int budget = UPLOAD_BUDGET;
        CloudTile tile;
        while (budget > 0 && (tile = uploadQueue.poll()) != null) {
            tile.queued = false;
            if (tile.epoch != epoch) {   // level swapped under us
                MeshData stale = tile.pendingMesh;
                tile.pendingMesh = null;
                if (stale != null) stale.close();   // upload() normally closes meshes
                continue;
            }
            MeshData mesh = tile.pendingMesh;
            tile.pendingMesh = null;
            if (mesh == null) {
                tile.empty = true;   // meshed: holds clear sky
                continue;
            }
            if (tile.buffer == null) tile.buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
            tile.buffer.bind();
            tile.buffer.upload(mesh);   // consumes (closes) the mesh
            VertexBuffer.unbind();
            budget--;
            if (!loggedFirstUpload) {
                loggedFirstUpload = true;
                LOGGER.info("first tile upload (tiles tracked={}, epoch={})", tiles.size(), epoch);
            }
        }
    }

    // ── Draw ─────────────────────────────────────────────────────────────

    /**
     * Draws the cloud tiles into the sun's shadow map.
     *
     * The same tiles that are drawn for the sky are drawn a second
     * time, from the sun's point of view, so the clouds become
     * occluders in the map. The map then says three things at once:
     * the terrain's shadow, the tree's shadow, and the CLOUD's shadow
     * -- and the cloud's gaps are what break the god rays into shafts.
     *
     * Only the depth matters here, so the blending is off and the
     * depth writes are on; the colour the shader produces is never
     * read.
     */
    public void drawForShadow(Level level, Matrix4f viewMatrix, Matrix4f projectionMatrix,
                              double camX, double camY, double camZ) {
        ShaderInstance shader = CloudShaderRegistry.cloudShader;
        if (shader == null) return;

        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();

        drawTiles(level, shader, viewMatrix, projectionMatrix, camX, camY, camZ, 0, true);

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableBlend();
    }

    private void drawTiles(Level level, ShaderInstance shader, Matrix4f viewMatrix, Matrix4f projectionMatrix,
                           double camX, double camY, double camZ, int rangeBlocks) {
        drawTiles(level, shader, viewMatrix, projectionMatrix, camX, camY, camZ, rangeBlocks, false);
    }

    private void drawTiles(Level level, ShaderInstance shader, Matrix4f viewMatrix, Matrix4f projectionMatrix,
                           double camX, double camY, double camZ, int rangeBlocks, boolean shadowPass) {
        if (shadowPass) {
            // the shadow map wants the cloud's DEPTH: no blending, and
            // the depth has to be written
            RenderSystem.disableBlend();
            RenderSystem.depthMask(true);
        } else {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.depthMask(false);
        }
        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();

        // fade into the sky at the range edge instead of popping; the
        // shader's FogStart/FogEnd/FogColor uniforms are auto-filled from
        // this RenderSystem state (verified in the 1.21.1 sources)
        float fogStart = RenderSystem.getShaderFogStart();
        float fogEnd = RenderSystem.getShaderFogEnd();
        FogShape fogShape = RenderSystem.getShaderFogShape();
        FogRenderer.levelFogColor();
        RenderSystem.setShaderFogStart(rangeBlocks * 0.55f);
        RenderSystem.setShaderFogEnd(rangeBlocks * 0.97f);

        float[] tint = cloudTint(level);
        RenderSystem.setShaderColor(tint[0], tint[1], tint[2], 1.0f);

        Uniform offsetUniform = shader.getUniform("ModelOffset");
        int drawn = 0;
        for (CloudTile tile : tiles.values()) {
            if (tile.buffer == null) continue;
            if (frame - tile.lastNeededFrame > DRAW_GRACE) continue;
            offsetUniform.set(
                    (float) (tile.tileX * CloudMesher.TILE_BLOCKS + driftX - camX),
                    (float) (CloudDensityField.LAYER_BOTTOM - camY),
                    (float) (tile.tileZ * CloudMesher.TILE_BLOCKS + driftZ - camZ));
            // REQUIRED: drawWithShader never binds the VAO itself -- without
            // this the draw runs against VAO 0 (the old invisible-clouds bug)
            tile.buffer.bind();
            tile.buffer.drawWithShader(viewMatrix, projectionMatrix, shader);
            VertexBuffer.unbind();
            drawn++;
        }

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.setShaderFogStart(fogStart);
        RenderSystem.setShaderFogEnd(fogEnd);
        if (fogShape != null) RenderSystem.setShaderFogShape(fogShape);
        if (!shadowPass) {
            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
        }

        if (!loggedFirstDraw) {
            loggedFirstDraw = true;
            LOGGER.info("first draw: {} tiles, glErr=0x{}", drawn, Integer.toHexString(GL11.glGetError()));
        }
    }

    // ── Drift + eviction ─────────────────────────────────────────────────

    /** Wind advection, integrated in real time (smooth at any fps). */
    private void advanceDrift() {
        double now = Util.getMillis();
        double dtSec = Math.min((now - lastDriftMillis) / 1000.0, 0.25);   // clamp tab-outs
        lastDriftMillis = now;
        double blocksPerSec = windFactor() * DRIFT_BLOCKS_PER_TICK * 20.0;
        driftX += windX() * blocksPerSec * dtSec;
        driftZ += windZ() * blocksPerSec * dtSec;
    }

    // ── State exposed to GodRayRenderer (same package) ───────────────────

    /** The cloud field's seed -- ray occlusion must sample the same field. */
    public long cloudSeed() { return seed; }

    /** Integrated wind drift; cloud space = world minus these. */
    public double cloudDriftX() { return driftX; }
    public double cloudDriftZ() { return driftZ; }

    private void evictStale() {
        tiles.values().removeIf(tile -> {
            if (frame - tile.lastNeededFrame <= UNUSED_FRAME_GRACE) return false;
            if (tile.buffer != null) tile.buffer.close();   // render thread: safe
            return true;
        });
    }

    private void resetFor(Level level) {
        epoch++;
        for (CloudTile tile : tiles.values()) {
            if (tile.buffer != null) tile.buffer.close();   // render thread: safe
        }
        tiles.clear();
        workQueue.clear();
        CloudTile stale;
        while ((stale = uploadQueue.poll()) != null) {
            if (stale.pendingMesh != null) {
                stale.pendingMesh.close();
                stale.pendingMesh = null;
            }
        }
        lastLevel = level;
        seed = worldSeed(level) ^ level.dimension().location().hashCode();
        driftX = driftZ = 0.0;
        lastDriftMillis = Util.getMillis();
    }

    // ── Wind + tint (synced weather state) ───────────────────────────────

    /**
     * Stable per-level seed. The world seed is only reachable on the
     * integrated (singleplayer) server; dedicated servers keep it hidden,
     * so remote worlds fall back to 0 -- cloud shapes then repeat between
     * worlds of the same dimension instead of per session. Never random:
     * the field must be identical on re-entry.
     */
    private static long worldSeed(Level level) {
        try {
            IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
            if (server != null) return server.getWorldData().worldGenOptions().seed();
        } catch (Exception ignored) {
        }
        return 0L;
    }

    private static float windX() { return (float) ClientSeasonState.getWindState().getEffectiveDx(); }
    private static float windZ() { return (float) ClientSeasonState.getWindState().getEffectiveDz(); }

    private static float windFactor() {
        try {
            float s = ClientSeasonState.getWindState().getSpeed();
            return 0.5f + Math.min(2.0f, s) * 0.5f;
        } catch (Exception e) { return 1.0f; }
    }

    public static boolean isStorm(Level level) {
        try {
            String w = ClientSeasonState.getWeatherId();
            return w != null && (w.contains("storm") || w.contains("thunder")
                    || w.contains("rain") || w.contains("blizzard"));
        } catch (Exception e) { return level.isRaining(); }
    }

    /** Cloud color by time of day + weather, in-place 3-float array. */
    private static float[] cloudTint(Level level) {
        float[] out = new float[3];
        float dayFraction = level.getTimeOfDay(1.0f);   // 0 sunrise, .25 noon, .5 sunset

        boolean storm = isStorm(level);

        // base by day brightness (use skyDarken for storms/night)
        float bright = 1.0f - level.getSkyDarken() / 15f * 0.75f;

        float r = bright, g = bright, b = bright;

        // sunrise/sunset warm tint: near dayFraction 0 and 0.5
        float sunsetness = sunsetAmount(dayFraction);
        if (sunsetness > 0) {
            r = Mth.lerp(sunsetness, r, 1.00f * bright);
            g = Mth.lerp(sunsetness, g, 0.72f * bright);
            b = Mth.lerp(sunsetness, b, 0.50f * bright);
        }

        // night: pull toward dark blue-gray
        float nightness = nightAmount(dayFraction);
        r = Mth.lerp(nightness, r, 0.24f);
        g = Mth.lerp(nightness, g, 0.27f);
        b = Mth.lerp(nightness, b, 0.38f);

        if (storm) { r *= 0.55f; g *= 0.57f; b *= 0.62f; }

        out[0] = Mth.clamp(r, 0, 1);
        out[1] = Mth.clamp(g, 0, 1);
        out[2] = Mth.clamp(b, 0, 1);
        return out;
    }

    private static float sunsetAmount(float dayFraction) {
        // warm around sunrise (0) and sunset (0.5), wrapping
        float d = Math.min(dayFraction, 1f - dayFraction);        // dist to 0
        float s = Math.abs(dayFraction - 0.5f);                    // dist to 0.5
        float m = Math.min(d, s);
        return m > 0.08f ? 0 : 1f - m / 0.08f;
    }

    private static float nightAmount(float dayFraction) {
        // 1 deep night (~0.75), 0 during day
        float v = (float) Math.abs(Math.sin(dayFraction * Math.PI)); // 0 at 0/.5, 1 at .75
        return Mth.clamp((v - 0.7f) / 0.3f, 0, 1);
    }
}
