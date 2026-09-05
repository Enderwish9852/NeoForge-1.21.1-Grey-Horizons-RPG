package net.enderwish.Atmospheric_Overhaul_Subpack.core.weather;

import net.enderwish.Atmospheric_Overhaul_Subpack.core.season.SeasonData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * WindManager
 *
 * Server-side singleton. Ticks wind state every game tick, PERSISTED
 * per-level through SeasonData (SavedData, saved to that level's own
 * save folder) rather than an in-memory-only field — wind no longer
 * resets to defaults on world reload, and correctly resumes any
 * in-progress lerp toward a target speed.
 *
 * Each tick: loads the current WindState from SeasonData (speed AND
 * targetSpeed both restored, so a mid-interpolation wind resumes exactly
 * where it left off), advances it, writes the result back to SeasonData,
 * and broadcasts to clients every 20 ticks.
 *
 * Wind profile on weather change comes directly from the active
 * WeatherDefinition's JSON fields (windSpeedMin, windSpeedMax,
 * windTurbulence) — fully data-driven, no hardcoded Java profile table.
 */
public class WindManager {

    public static final WindManager INSTANCE = new WindManager();
    private WindManager() {}

    private int tickCounter = 0;
    private static final int SYNC_INTERVAL = 20; // ticks between client syncs

    // ── Tick ──────────────────────────────────────────────────────────────────

    public void tick(ServerLevel level) {
        SeasonData data = SeasonData.get(level);
        RandomSource random = level.getRandom();

        WindState state = loadState(data);
        state.tick(random);
        saveState(data, state);

        tickCounter++;
        if (tickCounter >= SYNC_INTERVAL) {
            tickCounter = 0;
            broadcastToClients(level, state);
        }
    }

    // ── Weather change (natural pipeline) ─────────────────────────────────────

    /**
     * Called by SeasonEventHandler when the active weather changes.
     * Applies the new WeatherDefinition's wind profile immediately.
     * Speed still lerps smoothly toward target via WindState.tick() —
     * this is the natural, non-instant path.
     */
    public void onWeatherChanged(WeatherDefinition newWeather,
                                 ServerLevel level) {
        SeasonData data = SeasonData.get(level);
        RandomSource random = level.getRandom();

        WindState state = loadState(data);

        float rolledSpeed = newWeather.rollWindSpeed(random);
        state.setTargetSpeed(rolledSpeed);
        state.setTurbulence(newWeather.windTurbulence());

        // On gale-force weather, also shift direction randomly
        if (newWeather.isGale()) {
            state.setDirection(WindDirection.random(random));
        }

        saveState(data, state);
        broadcastToClients(level, state);
    }

    // ── Manual override (command-driven) ──────────────────────────────────────

    /**
     * Called by WeatherCommand when weather is force-set via /ghweather.
     * Applies speed/turbulence/direction INSTANTLY (snaps, no lerp) so
     * command testing isn't fighting WindState's ~10-second natural
     * interpolation. direction may be null to leave it unchanged.
     */
    public void applyManual(float speed, float turbulence,
                            WindDirection direction, ServerLevel level) {
        SeasonData data = SeasonData.get(level);
        WindState state = loadState(data);

        state.setTargetSpeed(Mth.clamp(speed, 0f, 1f));
        state.snapToTarget(); // instant — no lerp for command testing
        state.setTurbulence(Mth.clamp(turbulence, 0f, 1f));
        if (direction != null) {
            state.setDirection(direction);
        }

        saveState(data, state);
        broadcastToClients(level, state);
    }

    // ── SeasonData bridge ────────────────────────────────────────────────────

    /**
     * Reconstructs a WindState from SeasonData's persisted fields, using
     * the full constructor (speed AND targetSpeed) so mid-interpolation
     * wind correctly resumes rather than snapping to "arrived" on load.
     */
    private WindState loadState(SeasonData data) {
        return new WindState(
                data.getWindDirection(),
                data.getWindSpeed(),
                data.getWindTargetSpeed(),
                data.getWindGustFactor(),
                data.getWindTurbulence()
        );
    }

    private void saveState(SeasonData data, WindState state) {
        data.setWindState(
                state.getDirection(),
                state.getSpeed(),
                state.getTargetSpeed(),
                state.getTurbulence(),
                state.getGustFactor()
        );
    }

    // ── Sync ──────────────────────────────────────────────────────────────────

    private void broadcastToClients(ServerLevel level, WindState state) {
        net.enderwish.Atmospheric_Overhaul_Subpack.network.WindSyncPacket
                .sendToAll(level, state);
    }

    // ── Getters (server-side, per-level) ────────────────────────────────────

    public WindState getState(ServerLevel level) { return loadState(SeasonData.get(level)); }
    public float getSpeed(ServerLevel level)      { return SeasonData.get(level).getWindSpeed(); }
    public WindDirection getDirection(ServerLevel level) {
        return SeasonData.get(level).getWindDirection();
    }
}