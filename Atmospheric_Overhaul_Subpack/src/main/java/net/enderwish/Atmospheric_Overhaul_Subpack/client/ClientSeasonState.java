package net.enderwish.Atmospheric_Overhaul_Subpack.client;

import net.enderwish.Atmospheric_Overhaul_Subpack.core.season.SeasonCalendar;

/**
 * ClientSeasonState
 *
 * Stores the season data synced from the server via SeasonSyncPacket.
 * All fields are static — there is only ever one season state on the client.
 *
 * Only ever written to by SeasonSyncPacket.handle().
 * Read by everything client-side:
 *   - HUD rendering
 *   - BiomeMixin (grass/foliage tint)
 *   - FogRendererMixin (fog colour + density)
 *   - SeasonsAPI client methods
 *
 * Never call this from server-side code.
 */
public final class ClientSeasonState {

    private ClientSeasonState() {} // static only

    // ── State ─────────────────────────────────────────────────────────────────

    private static int totalDays       = 0;
    private static int yearDay         = 0;
    private static int year            = 0;

    private static SeasonCalendar.Season season = SeasonCalendar.Season.SPRING;
    private static SeasonCalendar.Phase  phase  = SeasonCalendar.Phase.EARLY;

    private static String activeWeatherId  = "clear";
    private static float  activeIntensity  = 0.0f;

    // ── Update — called by SeasonSyncPacket ───────────────────────────────────

    /**
     * Updates all client season state from a received sync packet.
     * Must be called on the client thread — SeasonSyncPacket uses
     * context.enqueueWork() to ensure this.
     */
    public static void update(
            int totalDays,
            int yearDay,
            SeasonCalendar.Season season,
            SeasonCalendar.Phase phase,
            String activeWeatherId,
            float activeIntensity,
            int year
    ) {
        ClientSeasonState.totalDays       = totalDays;
        ClientSeasonState.yearDay         = yearDay;
        ClientSeasonState.season          = season;
        ClientSeasonState.phase           = phase;
        ClientSeasonState.activeWeatherId = activeWeatherId;
        ClientSeasonState.activeIntensity = activeIntensity;
        ClientSeasonState.year            = year;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    /** Current season on the client. e.g. SPRING, SUMMER, AUTUMN, WINTER */
    public static SeasonCalendar.Season getSeason() { return season; }

    /** Current phase on the client. e.g. EARLY, MID, LATE */
    public static SeasonCalendar.Phase getPhase() { return phase; }

    /** Current year day (0-79). */
    public static int getYearDay() { return yearDay; }

    /** Total ever-incrementing day count. */
    public static int getTotalDays() { return totalDays; }

    /** How many full years have passed. */
    public static int getYear() { return year; }

    /** ID of the currently active weather. e.g. "light_rain", "blizzard" */
    public static String getWeatherId() { return activeWeatherId; }

    /** Current weather intensity (0.0 - 1.0). */
    public static float getIntensity() { return activeIntensity; }

    /** True if it is currently raining or snowing. */
    public static boolean isPrecipitating() {
        return !activeWeatherId.equals("clear")
                && !activeWeatherId.equals("fog")
                && !activeWeatherId.equals("heatwave");
    }

    /** True if the active weather is a special weather (blizzard, heatwave). */
    public static boolean isSpecialWeather() {
        return activeWeatherId.equals("blizzard")
                || activeWeatherId.equals("heatwave");
    }

    /**
     * Returns a human-readable display label.
     * e.g. "Early Spring, Year 2"
     * Ready to use directly in HUD rendering.
     */
    public static String getDisplayLabel() {
        return phase.displayName()
                + " " + season.displayName()
                + ", Year " + (year + 1);
    }

    /**
     * Returns a short season label for compact HUD display.
     * e.g. "Early Spring"
     */
    public static String getShortLabel() {
        return phase.displayName() + " " + season.displayName();
    }
}
