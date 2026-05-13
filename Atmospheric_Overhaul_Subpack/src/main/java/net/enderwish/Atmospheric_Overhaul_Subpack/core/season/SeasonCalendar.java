package net.enderwish.Atmospheric_Overhaul_Subpack.core.season;

public class SeasonCalendar {

    public static final int DAYS_PER_YEAR = 80;
    public static final int DAYS_PER_SEASON = 20;

    // Enums
    public enum Season {
        SPRING,
        SUMMER,
        AUTUMN,
        WINTER;
        // Human-readable names
        public String displayName() {
            return switch (this) {
                case SPRING -> "Spring";
                case SUMMER -> "Summer";
                case AUTUMN -> "Autumn";
                case WINTER -> "Winter";
            };
        }
    }
    public enum Phase {
        EARLY (0, 6),
        MID (7, 13),
        LATE (14, 19);

        public final int startDay;
        public final int endDay;

        Phase(int startDay, int endDay) {
            this.startDay = startDay;
            this.endDay = endDay;
        }
        // Human-readable names
        public String displayName() {
            return switch (this) {
                case EARLY -> "Early";
                case MID -> "Mid";
                case LATE -> "Late";
            };
        }
    }
    // Queries
    // Returns the season for given yearDay (0-79)
    public static Season getSeason(int yearDay) {
        int index = clampYearDay(yearDay) / DAYS_PER_SEASON;
        return Season.values()[index];
    }
    // Return the phase wthin the current season for a given yearDay (0-79)
    public static Phase getPhase(int yearDay) {
        int dayInSeason = getDayInSeason(yearDay);
        for (Phase phase : Phase.values()) {
            if (dayInSeason >= phase.startDay && dayInSeason <= phase.endDay){
                return phase;
            }
        }
        return Phase.LATE; // Hopefully never happen
    }
    // Returns the day in the current season (0-79)
    public static int getDayInSeason(int yearDay) {
        return clampYearDay(yearDay) % DAYS_PER_SEASON;
    }
    // Returns how many complete years have passed
    public static int getYear(int totalDays) {
        return totalDays / DAYS_PER_YEAR;
    }
    // Converts a running totalDays count to a yearDay (0-79)
    public static int toYearDay(int totalDays) {
        return totalDays % DAYS_PER_YEAR;
    }
    // Return a full readable label e.g. "Early Spring, Year X"
    public static String getDisplayLabel(int totalDays) {
        int yearDay = toYearDay(totalDays);
        return getPhase(yearDay).displayName()
                + "" + getSeason(yearDay).displayName()
                + ", Year" + (getYear(totalDays) +1);
    }
    // Helpers
    private static int clampYearDay(int yearDay) {
        return Math.abs(yearDay) % DAYS_PER_YEAR;
    }
}
