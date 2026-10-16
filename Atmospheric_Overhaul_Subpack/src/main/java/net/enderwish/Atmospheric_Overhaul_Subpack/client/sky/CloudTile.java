package net.enderwish.Atmospheric_Overhaul_Subpack.client.sky;

import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;

/**
 * CloudTile
 *
 * NEW -- one tile of the cloud lattice: its cloud-space grid
 * coordinates, the handoff mesh built by the worker thread, and the
 * GPU state owned by the render thread.
 *
 * Thread contract:
 * - buildSeed/epoch are written by the render thread at ENQUEUE time;
 *   the work queue's happens-before edge publishes them to the worker.
 * - pendingMesh is volatile: the worker writes the built mesh, the
 *   render thread consumes it in processUploads (upload() closes it).
 * - queued/buffer/empty/lastNeededFrame are render-thread only.
 * - epoch guards stale handoffs across level swaps: results from an
 *   older generation are dropped at upload time.
 */
final class CloudTile {

    final int tileX, tileZ;
    final long key;

    // enqueue-time snapshot (render thread writes, worker reads)
    long buildSeed;
    int epoch;

    /** worker -> render handoff; consumed by the upload */
    volatile MeshData pendingMesh;

    // render-thread-only state
    boolean queued;
    VertexBuffer buffer;
    boolean empty;
    long lastNeededFrame;

    CloudTile(int tileX, int tileZ) {
        this.tileX = tileX;
        this.tileZ = tileZ;
        this.key = packKey(tileX, tileZ);
    }

    /** Meshed already -- either uploaded or known-empty sky. */
    boolean hasMesh() {
        return buffer != null || empty;
    }

    static long packKey(int tileX, int tileZ) {
        return ((long) tileX << 32) | (tileZ & 0xFFFFFFFFL);
    }
}
