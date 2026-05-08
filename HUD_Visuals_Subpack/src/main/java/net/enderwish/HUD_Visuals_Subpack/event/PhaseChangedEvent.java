package net.enderwish.HUD_Visuals_Subpack.event;

import net.enderwish.HUD_Visuals_Subpack.core.season.SeasonCalendar;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.Event;

/**
 * PhaseChangedEvent
 *
 * Fired on the NeoForge EVENT_BUS when the season phase changes.
 * e.g. Early Spring → Mid Spring, Mid Autumn → Late Autumn
 *
 * Note: SeasonChangedEvent is fired instead when the season itself changes.
 * This only fires for phase changes within the same season.
 *
 * Usage (from Farming subpack):
 *   @SubscribeEvent
 *   public static void onPhaseChanged(PhaseChangedEvent event) {
 *       if (event.getNewPhase() == SeasonCalendar.Phase.LATE
 *               && event.getSeason() == SeasonCalendar.Season.AUTUMN) {
 *           // late autumn — start preparing for winter
 *       }
 *   }
 */
public class PhaseChangedEvent extends Event {

    private final ServerLevel level;
    private final SeasonCalendar.Season season;
    private final SeasonCalendar.Phase oldPhase;
    private final SeasonCalendar.Phase newPhase;
    private final int yearDay;

    public PhaseChangedEvent(
            ServerLevel level,
            SeasonCalendar.Season season,
            SeasonCalendar.Phase oldPhase,
            SeasonCalendar.Phase newPhase,
            int yearDay
    ) {
        this.level    = level;
        this.season   = season;
        this.oldPhase = oldPhase;
        this.newPhase = newPhase;
        this.yearDay  = yearDay;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    /** The server level this phase change occurred in. */
    public ServerLevel getLevel() { return level; }

    /** The season this phase change occurred in (unchanged). */
    public SeasonCalendar.Season getSeason() { return season; }

    /** The previous phase. */
    public SeasonCalendar.Phase getOldPhase() { return oldPhase; }

    /** The new phase. */
    public SeasonCalendar.Phase getNewPhase() { return newPhase; }

    /** The current yearDay (0-79) when this event fired. */
    public int getYearDay() { return yearDay; }

    // ── Convenience ───────────────────────────────────────────────────────────

    /** True if transitioning into the MID phase. */
    public boolean isEnteringMid() { return newPhase == SeasonCalendar.Phase.MID; }

    /** True if transitioning into the LATE phase. */
    public boolean isEnteringLate() { return newPhase == SeasonCalendar.Phase.LATE; }

    /** Human-readable label e.g. "Early Spring → Mid Spring" */
    public String getDisplayLabel() {
        return oldPhase.displayName() + " " + season.displayName()
                + " → " + newPhase.displayName() + " " + season.displayName();
    }
}
