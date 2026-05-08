package net.enderwish.HUD_Visuals_Subpack.event;

import net.enderwish.HUD_Visuals_Subpack.core.season.SeasonCalendar;
import net.enderwish.HUD_Visuals_Subpack.core.weather.WeatherDefinition;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.Event;

/**
 * WeatherChangedEvent
 *
 * Fired on the NeoForge EVENT_BUS when the active weather changes.
 * Other subpacks listen to this to react to weather changes.
 *
 * Usage (from Farming subpack):
 *   @SubscribeEvent
 *   public static void onWeatherChanged(WeatherChangedEvent event) {
 *       if (event.getNewWeatherId().equals("blizzard")) {
 *           // freeze crops etc.
 *       }
 *   }
 */
public class WeatherChangedEvent extends Event {

    private final ServerLevel level;
    private final String oldWeatherId;
    private final String newWeatherId;
    private final WeatherDefinition newWeather;
    private final SeasonCalendar.Season season;
    private final SeasonCalendar.Phase phase;
    private final float intensity;
    private final int durationTicks;

    public WeatherChangedEvent(
            ServerLevel level,
            String oldWeatherId,
            String newWeatherId,
            WeatherDefinition newWeather,
            SeasonCalendar.Season season,
            SeasonCalendar.Phase phase,
            float intensity,
            int durationTicks
    ) {
        this.level         = level;
        this.oldWeatherId  = oldWeatherId;
        this.newWeatherId  = newWeatherId;
        this.newWeather    = newWeather;
        this.season        = season;
        this.phase         = phase;
        this.intensity     = intensity;
        this.durationTicks = durationTicks;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    /** The server level this weather change occurred in. */
    public ServerLevel getLevel() { return level; }

    /** The ID of the previous weather e.g. "clear" */
    public String getOldWeatherId() { return oldWeatherId; }

    /** The ID of the new weather e.g. "blizzard" */
    public String getNewWeatherId() { return newWeatherId; }

    /** The full WeatherDefinition for the new weather. */
    public WeatherDefinition getNewWeather() { return newWeather; }

    /** The season this weather change occurred in. */
    public SeasonCalendar.Season getSeason() { return season; }

    /** The phase this weather change occurred in. */
    public SeasonCalendar.Phase getPhase() { return phase; }

    /** The rolled intensity for this weather (0.0 - 1.0). */
    public float getIntensity() { return intensity; }

    /** How many ticks this weather will last. */
    public int getDurationTicks() { return durationTicks; }

    // ── Convenience ───────────────────────────────────────────────────────────

    /** True if the new weather has rain or snow. */
    public boolean isPrecipitating() { return newWeather.hasRain(); }

    /** True if the new weather has thunder. */
    public boolean isThundering() { return newWeather.hasThunder(); }

    /** True if the new weather is a special weather (blizzard, heatwave). */
    public boolean isSpecial() { return newWeather.isSpecial(); }
}
