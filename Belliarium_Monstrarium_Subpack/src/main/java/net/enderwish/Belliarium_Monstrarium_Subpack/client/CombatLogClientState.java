package net.enderwish.Belliarium_Monstrarium_Subpack.client;

import net.enderwish.Belliarium_Monstrarium_Subpack.core.CombatLogRowType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class CombatLogClientState {

    private CombatLogClientState() {}

    public record KillFeedRow(String killerName, String weaponId, String targetName, long spawnedAtMillis) {}

    private static final List<KillFeedRow> killFeedRows = new ArrayList<>();

    public static void addKillFeedRow(String killerName, String weaponId, String targetName) {
        killFeedRows.add(new KillFeedRow(killerName, weaponId, targetName, System.currentTimeMillis()));
    }

    public static List<KillFeedRow> getKillFeedRows() { return killFeedRows; }

    public static void pruneKillFeed(long lingerMs, long fadeMs) {
        killFeedRows.removeIf(row -> System.currentTimeMillis() - row.spawnedAtMillis() > lingerMs + fadeMs);
    }

    public static class ExpRowState {
        public String label = "";
        public float displayedValue = 0f;
        public float targetValue = 0f;
        public boolean visible = false;
        public boolean fadesOnItsOwn = false;
        public long shownAtMillis = 0;
        public boolean fadingOut = false;
        public long fadeStartMillis = 0;
    }

    private static final Map<CombatLogRowType, ExpRowState> expRows = new EnumMap<>(CombatLogRowType.class);
    static {
        for (CombatLogRowType type : CombatLogRowType.values()) expRows.put(type, new ExpRowState());
    }

    public static ExpRowState getRow(CombatLogRowType type) { return expRows.get(type); }

    public static void showRow(CombatLogRowType type, String label, float targetXp, boolean fadesOnItsOwn) {
        ExpRowState row = expRows.get(type);
        row.label = label;
        row.targetValue = targetXp;
        row.visible = true;
        row.fadesOnItsOwn = fadesOnItsOwn;
        row.shownAtMillis = System.currentTimeMillis();
        row.fadingOut = false;
    }

    public static void fadeOutRow(CombatLogRowType type) {
        ExpRowState row = expRows.get(type);
        row.fadingOut = true;
        row.fadeStartMillis = System.currentTimeMillis();
    }

    public static void restartRow(CombatLogRowType type, String label) {
        ExpRowState row = expRows.get(type);
        row.label = label;
        row.displayedValue = 0f;
        row.targetValue = 0f;
        row.visible = true;
        row.fadingOut = false;
    }

    /**
     * NEW -- battle-end entry point. Leaves label/displayedValue/targetValue
     * completely untouched, only starts the same fade timer fadeOutRow()
     * already uses per-row -- the row keeps showing its real last value
     * while it fades, instead of visibly resetting to 0 first.
     */
    public static void fadeOutAllRows() {
        long now = System.currentTimeMillis();
        for (ExpRowState row : expRows.values()) {
            if (row.visible && !row.fadingOut) {
                row.fadingOut = true;
                row.fadeStartMillis = now;
            }
        }
    }

    /** Kept for a possible future hard-reset case (e.g. logout) -- battle-end no longer uses this. */
    public static void clearAllExpRows() {
        for (ExpRowState row : expRows.values()) {
            row.visible = false;
            row.fadingOut = false;
            row.displayedValue = 0f;
            row.targetValue = 0f;
        }
    }
}
