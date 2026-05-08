package net.enderwish.HUD_Visuals_Subpack.event;

import net.enderwish.HUD_Visuals_Subpack.core.season.SeasonCalendar;
import net.enderwish.HUD_Visuals_Subpack.core.season.SeasonData;
import net.enderwish.HUD_Visuals_Subpack.core.weather.WeatherDefinition;
import net.enderwish.HUD_Visuals_Subpack.core.weather.WeatherRegistry;
import net.enderwish.HUD_Visuals_Subpack.core.weather.WeatherRoller;
import net.enderwish.HUD_Visuals_Subpack.network.ModMessages;
import net.enderwish.HUD_Visuals_Subpack.network.SeasonSyncPacket;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * SeasonEventHandler
 *
 * The main server-side tick listener.
 * Every server tick it:
 *   1. Advances SeasonData (day counter, ticks)
 *   2. Fires SeasonChangedEvent, PhaseChangedEvent when transitions happen
 *   3. Checks if weather needs re-rolling
 *   4. Calls WeatherRoller to pick new weather if needed
 *   5. Applies weather to the Minecraft level
 *   6. Fires WeatherChangedEvent
 *
 * Registered on NeoForge.EVENT_BUS in HUDVisualsSubpack constructor.
 */
public class SeasonEventHandler {

    // ── Tick ──────────────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {

        // Only run on the server overworld
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (level.dimension() != ServerLevel.OVERWORLD) return;

        SeasonData data = SeasonData.get(level);

        // ── Step 1: advance the day counter ───────────────────────────────────
        SeasonCalendar.Season seasonBefore = data.getSeason();
        SeasonCalendar.Phase  phaseBefore  = data.getPhase();

        SeasonData.TickResult result = data.tick();

        SeasonCalendar.Season seasonAfter = data.getSeason();
        SeasonCalendar.Phase  phaseAfter  = data.getPhase();

        // ── Step 2: fire transition events ────────────────────────────────────
        if (result == SeasonData.TickResult.SEASON_CHANGED) {
            // Fire SeasonChangedEvent
            NeoForge.EVENT_BUS.post(new SeasonChangedEvent(
                    level,
                    seasonBefore,
                    seasonAfter,
                    phaseBefore,
                    phaseAfter,
                    data.getYearDay()
            ));
            // Re-roll weather immediately on season change
            rollAndApplyWeather(level, data);

        } else if (result == SeasonData.TickResult.PHASE_CHANGED) {
            // Fire PhaseChangedEvent
            NeoForge.EVENT_BUS.post(new PhaseChangedEvent(
                    level,
                    seasonAfter,
                    phaseBefore,
                    phaseAfter,
                    data.getYearDay()
            ));
            // Re-roll weather on phase change too — pool may have shifted
            rollAndApplyWeather(level, data);
        }

        // ── Step 3: check if weather needs re-rolling ─────────────────────────
        if (data.needsWeatherRoll()) {
            rollAndApplyWeather(level, data);
        }
    }

    // ── Weather rolling ───────────────────────────────────────────────────────

    /**
     * Asks WeatherRoller to pick the next weather, stores it in SeasonData,
     * applies it to the Minecraft level, and fires WeatherChangedEvent.
     */
    private static void rollAndApplyWeather(ServerLevel level, SeasonData data) {

        // Guard — don't roll if registry isn't loaded yet
        if (!WeatherRegistry.INSTANCE.isLoaded()) return;

        String oldWeatherId = data.getActiveWeatherId();

        // Roll new weather
        WeatherRoller.RollResult roll = WeatherRoller.INSTANCE.roll(
                data.getSeason(),
                data.getPhase(),
                level
        );

        // Store in SeasonData
        data.setActiveWeather(roll.name(), roll.durationTicks(), roll.intensity());

        // Apply to Minecraft level
        applyToLevel(level, roll);

        // Fire WeatherChangedEvent only if weather actually changed
        if (!roll.name().equals(oldWeatherId)) {
            NeoForge.EVENT_BUS.post(new WeatherChangedEvent(
                    level,
                    oldWeatherId,
                    roll.name(),
                    roll.definition(),
                    data.getSeason(),
                    data.getPhase(),
                    roll.intensity(),
                    roll.durationTicks()
            ));
        }
        ModMessages.sendToAllPlayers(new SeasonSyncPacket(
                data.getTotalDays(),
                data.getYearDay(),
                data.getSeason(),
                data.getPhase(),
                data.getActiveWeatherId(),
                data.getActiveIntensity(),
                data.getYear()
        ));
    }

    /**
     * Applies the rolled weather to the Minecraft ServerLevel.
     *
     * setWeatherParameters(clearTime, rainTime, isRaining, isThundering)
     *
     * clearTime    = how long clear weather lasts (0 if raining)
     * rainTime     = how long rain lasts (0 if clear)
     * isRaining    = whether rain/snow is active
     * isThundering = whether thunder is active
     */
    private static void applyToLevel(ServerLevel level, WeatherRoller.RollResult roll) {
        WeatherDefinition def = roll.definition();
        int duration = roll.durationTicks();

        if (def.hasRain()) {
            level.setWeatherParameters(
                    0,                  // clearTime
                    duration,           // rainTime
                    true,               // isRaining
                    def.hasThunder()    // isThundering
            );
        } else {
            level.setWeatherParameters(
                    duration,           // clearTime
                    0,                  // rainTime
                    false,              // isRaining
                    false               // isThundering
            );
        }
    }
}