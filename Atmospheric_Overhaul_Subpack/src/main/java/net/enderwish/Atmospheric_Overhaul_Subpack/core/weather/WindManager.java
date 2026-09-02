package net.enderwish.Atmospheric_Overhaul_Subpack.core.weather;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * WindManager
 *
 * Server-side singleton. Ticks WindState every game tick.
 * Adjusts target speed and turbulence when weather changes.
 * Broadcasts state to clients every 20 ticks.
 *
 * Wind profile comes directly from the active WeatherDefinition's
 * JSON fields (windSpeedMin, windSpeedMax, windTurbulence) — fully
 * data-driven, no hardcoded Java profile table.
 *
 * Uses level.getRandom() (RandomSource) throughout, matching
 * WindState.tick() and WindDirection.random().
 */
public class WindManager {

    public static final WindManager INSTANCE = new WindManager();
    private WindManager() {}

    private final WindState state = new WindState();

    private int tickCounter = 0;
    private static final int SYNC_INTERVAL = 20; // ticks between client syncs

    // ── Tick ──────────────────────────────────────────────────────────────────

    public void tick(ServerLevel level) {
        RandomSource random = level.getRandom();
        state.tick(random);

        tickCounter++;
        if (tickCounter >= SYNC_INTERVAL) {
            tickCounter = 0;
            broadcastToClients(level);
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
        RandomSource random = level.getRandom();

        float rolledSpeed = newWeather.rollWindSpeed(random);
        state.setTargetSpeed(rolledSpeed);
        state.setTurbulence(newWeather.windTurbulence());

        // On gale-force weather, also shift direction randomly
        if (newWeather.isGale()) {
            state.setDirection(WindDirection.random(random));
        }

        broadcastToClients(level);
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
        state.setTargetSpeed(Mth.clamp(speed, 0f, 1f));
        state.snapToTarget(); // instant — no lerp for command testing
        state.setTurbulence(Mth.clamp(turbulence, 0f, 1f));
        if (direction != null) {
            state.setDirection(direction);
        }
        broadcastToClients(level);
    }

    // ── Sync ──────────────────────────────────────────────────────────────────

    private void broadcastToClients(ServerLevel level) {
        net.enderwish.Atmospheric_Overhaul_Subpack.network.WindSyncPacket
                .sendToAll(level, state);
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public WindState getState()           { return state; }
    public float getSpeed()               { return state.getSpeed(); }
    public float getEffectiveDx()         { return state.getEffectiveDx(); }
    public float getEffectiveDz()         { return state.getEffectiveDz(); }
    public WindDirection getDirection()   { return state.getDirection(); }
}