package net.enderwish.Atmospheric_Overhaul_Subpack.client;

import net.enderwish.Atmospheric_Overhaul_Subpack.core.season.SeasonCalendar;
import net.enderwish.Atmospheric_Overhaul_Subpack.core.weather.WindState;
import net.enderwish.Atmospheric_Overhaul_Subpack.core.weather.WindDirection;

/**
 * ClientSeasonState
 *
 * Stores the season/weather data synced from the server via
 * SeasonSyncPacket, the GLOBAL wind data synced via WindSyncPacket, and
 * the PER-PLAYER local/felt environment data synced via
 * LocalPlayerEnvironmentSyncPacket. All fields are static — there is
 * only ever one state on the client.
 */
public final class ClientSeasonState {

    private ClientSeasonState() {}

    // ── Season state ──────────────────────────────────────────────────────────

    private static int totalDays       = 0;
    private static int yearDay         = 0;
    private static int year            = 0;

    private static SeasonCalendar.Season season = SeasonCalendar.Season.SPRING;
    private static SeasonCalendar.Phase  phase  = SeasonCalendar.Phase.EARLY;

    private static String activeWeatherId  = "clear";
    private static float  activeIntensity  = 0.0f;

    // ── Global wind state ─────────────────────────────────────────────────────

    private static WindState windState = new WindState();

    // ── Local (felt) environment state — per-player, synced separately ───────

    private static WindDirection feltWindDirection = WindDirection.SOUTHWEST;
    private static float feltWindSpeed = 0.0f;
    private static float feelsLikeTemp = 0.0f;

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

    // ── Local environment update — called by LocalPlayerEnvironmentSyncPacket ─

    public static void setLocalEnvironment(WindDirection direction, float speed, float feelsLike) {
        feltWindDirection = direction;
        feltWindSpeed = speed;
        feelsLikeTemp = feelsLike;
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

    // ── Getters — global wind ─────────────────────────────────────────────────

    public static float getWindSpeed() { return windState.getSpeed(); }
    public static WindDirection getWindDirection() { return windState.getDirection(); }
    public static float getWindDx() { return windState.getEffectiveDx(); }
    public static float getWindDz() { return windState.getEffectiveDz(); }
    public static boolean isWindy() { return windState.getSpeed() > 0.4f; }
    public static boolean isGale()  { return windState.isGale(); }

    // ── Getters — local (felt) environment ────────────────────────────────────

    public static WindDirection getFeltWindDirection() { return feltWindDirection; }
    public static float getFeltWindSpeed() { return feltWindSpeed; }
    public static float getFeltWindDx() { return feltWindDirection.dx * feltWindSpeed; }
    public static float getFeltWindDz() { return feltWindDirection.dz * feltWindSpeed; }
    public static float getFeelsLikeTemp() { return feelsLikeTemp; }
}