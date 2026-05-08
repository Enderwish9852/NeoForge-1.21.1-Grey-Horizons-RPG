package net.enderwish.HUD_Visuals_Subpack.event;

import net.enderwish.HUD_Visuals_Subpack.core.season.SeasonCalendar;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.Event;

/**
 * SeasonChangedEvent
 *
 * Fired on the NeoForge EVENT_BUS when the season changes.
 * e.g. Spring → Summer, Autumn → Winter
 *
 * Usage (from Farming subpack):
 *   @SubscribeEvent
 *   public static void onSeasonChanged(SeasonChangedEvent event) {
 *       if (event.getNewSeason() == SeasonCalendar.Season.SPRING) {
 *           // start growing crops etc.
 *       }
 *   }
 */
public class SeasonChangedEvent extends Event {

    private final ServerLevel level;
    private final SeasonCalendar.Season oldSeason;
    private final SeasonCalendar.Season newSeason;
    private final SeasonCalendar.Phase oldPhase;
    private final SeasonCalendar.Phase newPhase;
    private final int yearDay;

    public SeasonChangedEvent(
            ServerLevel level,
            SeasonCalendar.Season oldSeason,
            SeasonCalendar.Season newSeason,
            SeasonCalendar.Phase oldPhase,
            SeasonCalendar.Phase newPhase,
            int yearDay
    ) {
        this.level     = level;
        this.oldSeason = oldSeason;
        this.newSeason = newSeason;
        this.oldPhase  = oldPhase;
        this.newPhase  = newPhase;
        this.yearDay   = yearDay;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    /** The server level this season change occurred in. */
    public ServerLevel getLevel() { return level; }

    /** The previous season. */
    public SeasonCalendar.Season getOldSeason() { return oldSeason; }

    /** The new season. */
    public SeasonCalendar.Season getNewSeason() { return newSeason; }

    /** The phase of the previous season when it ended. */
    public SeasonCalendar.Phase getOldPhase() { return oldPhase; }

    /** The phase of the new season (always EARLY). */
    public SeasonCalendar.Phase getNewPhase() { return newPhase; }

    /** The current yearDay (0-79) when this event fired. */
    public int getYearDay() { return yearDay; }

    // ── Convenience ───────────────────────────────────────────────────────────

    /** True if transitioning into winter. */
    public boolean isEnteringWinter() { return newSeason == SeasonCalendar.Season.WINTER; }

    /** True if transitioning into spring. */
    public boolean isEnteringSpring() { return newSeason == SeasonCalendar.Season.SPRING; }

    /** Human-readable label e.g. "Autumn → Winter" */
    public String getDisplayLabel() {
        return oldSeason.displayName() + " → " + newSeason.displayName();
    }
}