package net.enderwish.Belliarium_Monstrarium_Subpack.client;

import net.enderwish.Belliarium_Monstrarium_Subpack.network.DeathReportPacket;

/**
 * DeathReportClientState
 *
 * Holds the latest received DeathReportPacket until DeathReportScreenHandler
 * consumes it. "Pending" clears on consume so a later, unrelated vanilla
 * DeathScreen open can't accidentally reopen a stale report.
 */
public final class DeathReportClientState {

    private DeathReportClientState() {}

    private static DeathReportPacket pending = null;

    public static void setPending(DeathReportPacket packet) {
        pending = packet;
    }

    public static DeathReportPacket consumePending() {
        DeathReportPacket result = pending;
        pending = null;
        return result;
    }
}
