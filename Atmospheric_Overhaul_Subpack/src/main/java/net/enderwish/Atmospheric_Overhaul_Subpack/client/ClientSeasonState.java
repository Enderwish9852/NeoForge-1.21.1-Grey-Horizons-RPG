package net.enderwish.Atmospheric_Overhaul_Subpack.client;

import net.enderwish.Atmospheric_Overhaul_Subpack.core.season.SeasonCalendar;
import net.enderwish.Atmospheric_Overhaul_Subpack.core.weather.WindState;

/**
 * ClientSeasonState
 *
 * Stores the season data synced from the server via SeasonSyncPacket,
 * and the wind data synced via WindSyncPacket.
 * All fields are static — there is only ever one state on the client.
 *
 * Never call this from server-side code.
 */
public final class ClientSeasonState {

    private ClientSeasonState() {} // static only

    // ── Season state ──────────────────────────────────────────────────────────

    private static int totalDays       = 0;
    private static int yearDay         = 0;
    private static int year            = 0;

    private static SeasonCalendar.Season season = SeasonCalendar.Season.SPRING;
    private static SeasonCalendar.Phase  phase  = SeasonCalendar.Phase.EARLY;

    private static String activeWeatherId  = "clear";
    private static float  activeIntensity  = 0.0f;

    // ── Wind state ────────────────────────────────────────────────────────────

    private static WindState windState = new WindState();

    // ── Update — called by SeasonSyncPacket ───────────────────────────────────

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

    // ── Wind update — called by WindSyncPacket ────────────────────────────────

    public static void setWindState(WindState state) {
        ClientSeasonState.windState = state;
    }

    public static WindState getWindState() {
        return windState;
    }

    // ── Getters — season ──────────────────────────────────────────────────────

    public static SeasonCalendar.Season getSeason() { return season; }
    public static SeasonCalendar.Phase getPhase() { return phase; }
    public static int getYearDay() { return yearDay; }
    public static int getTotalDays() { return totalDays; }
    public static int getYear() { return year; }
    public static String getWeatherId() { return activeWeatherId; }
    public static float getIntensity() { return activeIntensity; }

    public static boolean isPrecipitating() {
        return !activeWeatherId.equals("clear")
                && !activeWeatherId.equals("fog")
                && !activeWeatherId.equals("heatwave")
                && !activeWeatherId.equals("overcast");
    }

    public static boolean isSpecialWeather() {
        return activeWeatherId.equals("blizzard")
                || activeWeatherId.equals("heatwave")
                || activeWeatherId.equals("hail");
    }

    public static String getDisplayLabel() {
        return phase.displayName()
                + " " + season.displayName()
                + ", Year " + (year + 1);
    }

    public static String getShortLabel() {
        return phase.displayName() + " " + season.displayName();
    }

    // ── Getters — wind ────────────────────────────────────────────────────────

    /** Current wind speed (0.0 calm - 1.0 gale). */
    public static float getWindSpeed() { return windState.getSpeed(); }

    /** Current wind direction. */
    public static net.enderwish.Atmospheric_Overhaul_Subpack.core.weather.WindDirection
    getWindDirection() { return windState.getDirection(); }

    /** Effective X wind velocity — for particle deflection, camera bob etc. */
    public static float getWindDx() { return windState.getEffectiveDx(); }

    /** Effective Z wind velocity. */
    public static float getWindDz() { return windState.getEffectiveDz(); }

    public static boolean isWindy() { return windState.getSpeed() > 0.4f; }
    public static boolean isGale()  { return windState.isGale(); }
}