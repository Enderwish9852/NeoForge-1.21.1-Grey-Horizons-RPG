package net.enderwish.Belliarium_Monstrarium_Subpack.client;

/**
 * CombatTimerClientState
 *
 * BUGFIX (logs vanishing instead of smoothing out) -- battleId changing now
 * triggers fadeOutAllRows() instead of clearAllExpRows(). Same trigger
 * condition as before, just a graceful fade instead of an instant wipe.
 */
public final class CombatTimerClientState {

    private CombatTimerClientState() {}

    private static boolean inCombat = false;
    private static int elapsedSeconds = 0;
    private static String endMessage = null;
    private static long endMessageReceivedAtMillis = 0;
    private static long lastBattleId = -1;

    public static void update(boolean newInCombat, int newElapsedSeconds, String newEndMessage, long battleId) {
        inCombat = newInCombat;
        elapsedSeconds = newElapsedSeconds;
        if (newEndMessage != null) {
            endMessage = newEndMessage;
            endMessageReceivedAtMillis = System.currentTimeMillis();
        }

        if (battleId != lastBattleId) {
            lastBattleId = battleId;
            CombatLogClientState.fadeOutAllRows(); // THE FIX -- was clearAllExpRows()
        }
    }

    public static boolean isInCombat() { return inCombat; }
    public static int getElapsedSeconds() { return elapsedSeconds; }
    public static String getEndMessage() { return endMessage; }
    public static long getEndMessageReceivedAtMillis() { return endMessageReceivedAtMillis; }
}
