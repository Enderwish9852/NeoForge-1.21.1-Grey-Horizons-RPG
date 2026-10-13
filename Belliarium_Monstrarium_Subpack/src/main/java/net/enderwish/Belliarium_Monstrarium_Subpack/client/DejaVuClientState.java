package net.enderwish.Belliarium_Monstrarium_Subpack.client;

import java.util.List;

/**
 * DejaVuClientState
 *
 * Holds the two Deja Vu visual cues:
 *  - Ghost slots: per-slot item/count data for whatever remembered container
 *    GUI is currently open, drawn directly over the real (now empty) slots
 *    by DejaVuGhostSlotHandler for as long as that screen stays open. Set by
 *    DejaVuFadePacket, cleared by DejaVuClearPacket once the server sees the
 *    player close that screen (and deletes the block).
 *  - Teleport glitch: a timed red/violet vignette flash, read by
 *    DejaVuOverlayHUD, unchanged.
 */
public final class DejaVuClientState {
    private DejaVuClientState() {}

    public static final long TELEPORT_GLITCH_MS = 1200;

    private static List<Integer> ghostSlotIndices = List.of();
    private static List<String> ghostItemIds = List.of();
    private static List<Integer> ghostCounts = List.of();
    private static boolean ghostActive = false;

    private static long teleportStartedAt = -1;

    public static void triggerGhostSlots(List<Integer> slotIndices, List<String> itemIds, List<Integer> counts) {
        ghostSlotIndices = slotIndices;
        ghostItemIds = itemIds;
        ghostCounts = counts;
        ghostActive = true;
    }

    public static void clearGhostSlots() {
        ghostActive = false;
        ghostSlotIndices = List.of();
        ghostItemIds = List.of();
        ghostCounts = List.of();
    }

    public static boolean isGhostActive() { return ghostActive; }
    public static List<Integer> getGhostSlotIndices() { return ghostSlotIndices; }
    public static List<String> getGhostItemIds() { return ghostItemIds; }
    public static List<Integer> getGhostCounts() { return ghostCounts; }

    public static void triggerTeleportGlitch() {
        teleportStartedAt = System.currentTimeMillis();
    }

    public static boolean isTeleportGlitchActive() {
        return teleportStartedAt >= 0 && System.currentTimeMillis() - teleportStartedAt < TELEPORT_GLITCH_MS;
    }

    public static long teleportGlitchAgeMs() { return System.currentTimeMillis() - teleportStartedAt; }
}
