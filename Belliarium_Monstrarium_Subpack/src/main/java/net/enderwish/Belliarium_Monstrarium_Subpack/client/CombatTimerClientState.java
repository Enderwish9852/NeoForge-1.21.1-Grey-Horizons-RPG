package net.enderwish.Belliarium_Monstrarium_Subpack.client;

/**
 * CombatTimerClientState
 *
 * Client-side mirror filled in by CombatTimerSyncPacket. endMessage fades on
 * its own local timer once received -- see CombatTimerHUD for fade timing.
 */
public final class CombatTimerClientState {

    private CombatTimerClientState() {}

    private static boolean inCombat = false;
    private static int elapsedSeconds = 0;
    private static String endMessage = null;
    private static long endMessageReceivedAtMillis = 0;

    public static void update(boolean newInCombat, int newElapsedSeconds, String newEndMessage) {
        inCombat = newInCombat;
        elapsedSeconds = newElapsedSeconds;
        if (newEndMessage != null) {
            endMessage = newEndMessage;
            endMessageReceivedAtMillis = System.currentTimeMillis();
        }
    }

    public static boolean isInCombat() { return inCombat; }
    public static int getElapsedSeconds() { return elapsedSeconds; }
    public static String getEndMessage() { return endMessage; }
    public static long getEndMessageReceivedAtMillis() { return endMessageReceivedAtMillis; }
}
