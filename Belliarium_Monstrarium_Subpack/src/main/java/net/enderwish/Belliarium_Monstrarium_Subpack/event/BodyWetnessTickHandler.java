package net.enderwish.Belliarium_Monstrarium_Subpack.event;

import net.enderwish.Atmospheric_Overhaul_Subpack.api.SeasonsAPI;
import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.ModAttachments;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.status.StatusManager;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.status.StatusType;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.survival.SurvivalCapability;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * BodyWetnessTickHandler
 *
 * BODY WETNESS (0-100). Everything runs on a 5-tick step:
 *
 *  - IN WATER: soaked almost instantly (~0.5s to 100).
 *  - RAIN/SNOW, full sky exposure: wetting scales with Atmospheric's weather
 *    intensity; 0 -> 100 in ~90s at intensity 1.0. Snow wets at the same
 *    rate as rain.
 *  - PARTIAL COVER + WIND: overhead blocked but the angled rain still
 *    reaches (sky visible a few blocks upwind, the origin of the drops)
 *    -> wetting at half rate, and only while wind speed > 0.3. Full
 *    shelter on both checks -> no wetting at all.
 *  - DRYING: when not being wetted. Base 100 -> 0 in ~8 min, faster in
 *    heat (feels-like scaled, 0.5x in deep cold to 2x in heat), and
 *    much faster near heat blocks via SeasonsAPI.getHeatBlockModifier
 *    (the same strength/distance table the feels-like temp uses): a
 *    campfire (~+0.4) dries in ~2 min, lava-adjacent fastest.
 *  - DRENCHED (conditional status): applied the moment wetness hits
 *    100, removed when the body is 50% dried — the hysteresis rule.
 *    The actual cooling effect lives in ThirstBodyTempTickHandler.
 *
 * Creative/spectator pins wetness to 0. Sync rides the existing
 * SurvivalSyncPacket (now carrying wetness) — SurvivalTickHandler
 * already sends it every 10 ticks.
 */
@EventBusSubscriber(modid = BelliariumMonstrariumSubpack.MODID)
public class BodyWetnessTickHandler {

    /** Tick step for all wetness math. */
    private static final int WET_STEP_TICKS = 5;

    /** Full-exposure rain at intensity 1.0: 0 -> 100 in ~90s. */
    private static final float WET_PER_SEC_FULL = 100.0f / 90.0f;
    /** Partial-cover (wind-driven angled rain) multiplier on the rate. */
    private static final float PARTIAL_COVER_SCALE = 0.5f;
    /** Wind speed above which angled rain can reach under partial cover. */
    private static final float WIND_ANGLE_MIN = 0.3f;
    /** How many blocks upwind the angled-drop origin is probed. */
    private static final int ANGLE_CHECK_STEPS = 3;

    /** In water: 0 -> 100 in ~0.5s (two 5-tick steps at +50). */
    private static final float IN_WATER_WET_PER_STEP = 50.0f;

    /** Sheltered passive drying base: 100 -> 0 in ~8 min at a mild 15C. */
    private static final float DRY_PER_SEC_BASE = 100.0f / 480.0f;
    /** Heat-block drying scale on the modifier value (campfire ~+0.4 ->
     *  ~4x rate, i.e. ~2 min; lava-adjacent +2.0 -> ~16x). */
    private static final float HEAT_DRY_SCALE = 7.5f;

    /** DRENCHED clears once the body is this dry (50% wetness). */
    /**
     * DRENCHED clears once the body is this dry (50% wetness). Public:
     * the client F3 overlay derives DRENCHED client-side the same way.
     */
    public static final float DRENCHED_CLEAR_WETNESS = 50.0f;

    /** Wet-skin evaporative chill at full wetness in calm air, in C. */
    private static final float WET_CHILL_STILL_AIR = 3.0f;
    /** Extra chill at full wetness per unit of wind speed (0-1), in C. */
    private static final float WET_CHILL_PER_WIND = 5.0f;
    /** DRENCHED adds this much on top (soaked in a gale = 10C total chill). */
    private static final float DRENCHED_EXTRA_CHILL = 2.0f;

    /** Feels-like cache cadence (the chunk scan is not cheap). */
    private static final int FEELS_SAMPLE_INTERVAL = 20;

    private static final Map<UUID, Float> FEELS_LIKE_CACHE = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (serverPlayer.level().getGameTime() % WET_STEP_TICKS != 0) return;

        SurvivalCapability cap = player.getData(ModAttachments.SURVIVAL);
        ServerLevel level = serverPlayer.serverLevel();

        if (player.isCreative() || player.isSpectator()) {
            // pinned like the other survival stats: bone dry, no status
            if (cap.getWetness() > 0.0f) cap.setWetness(0.0f);
            StatusManager.INSTANCE.remove(player.getUUID(), StatusType.DRENCHED);
            return;
        }

        BlockPos pos = player.blockPosition();

        // ── In water: soaked almost instantly ─────────────────────────────
        boolean inWater = player.isInWater()
                || level.getFluidState(pos).is(FluidTags.WATER);
        if (inWater) {
            cap.setWetness(cap.getWetness() + IN_WATER_WET_PER_STEP);
            applyOrClearDrenched(player, cap);
            return;
        }

        // ── Rain / snow exposure ──────────────────────────────────────────
        float stepScale = WET_STEP_TICKS / 20.0f;   // per-second -> per-step
        if (SeasonsAPI.isPrecipitating(level)) {
            float intensity = Mth.clamp(SeasonsAPI.getWeatherIntensity(level), 0.0f, 1.0f);
            float exposure = rainExposure(level, pos);
            if (exposure > 0.0f) {
                cap.setWetness(cap.getWetness()
                        + WET_PER_SEC_FULL * intensity * exposure * stepScale);
                applyOrClearDrenched(player, cap);
                return;   // being wetted — no drying this step
            }
        }

        // ── Drying: passive (feels-like scaled) + heat blocks ────────────
        if (cap.getWetness() <= 0.0f) {
            applyOrClearDrenched(player, cap);
            return;
        }
        if (level.getGameTime() % FEELS_SAMPLE_INTERVAL == 0) {
            FEELS_LIKE_CACHE.put(serverPlayer.getUUID(),
                    (float) SeasonsAPI.getLocalChunkTempCelsius(level, pos));
        }
        float feelsLikeC = FEELS_LIKE_CACHE.getOrDefault(serverPlayer.getUUID(), 20.0f);
        float tempScale = Mth.clamp(1.0f + (feelsLikeC - 15.0f) / 25.0f, 0.5f, 2.0f);
        float heatScale = 1.0f + Math.max(0.0f,
                SeasonsAPI.getHeatBlockModifier(level, pos)) * HEAT_DRY_SCALE;
        cap.setWetness(cap.getWetness()
                - DRY_PER_SEC_BASE * tempScale * heatScale * stepScale);
        applyOrClearDrenched(player, cap);
    }

    /**
     * Returns the wetting-rate multiplier at a position: 1.0 fully exposed
     * to the sky, 0.5 when overhead cover still leaks wind-driven angled
     * rain (sky visible upwind, where the drops come from), 0.0 fully
     * sheltered.
     */
    private static float rainExposure(ServerLevel level, BlockPos pos) {
        if (level.canSeeSky(pos)) return 1.0f;

        float windSpeed = SeasonsAPI.getWindSpeed(level);
        if (windSpeed <= WIND_ANGLE_MIN) return 0.0f;

        // The wind's travel vector; the unit direction is that normalized.
        float vx = SeasonsAPI.getWindVectorX(level);
        float vz = SeasonsAPI.getWindVectorZ(level);
        int ux = Math.round(vx / windSpeed);
        int uz = Math.round(vz / windSpeed);

        // A drop that lands on the player came from upwind: probe those
        // origins. Sky visible at any of them -> angled rain still reaches
        // under the cover, at half rate.
        for (int k = 1; k <= ANGLE_CHECK_STEPS; k++) {
            BlockPos probe = pos.offset(-ux * k, 0, -uz * k);
            if (level.canSeeSky(probe)) return PARTIAL_COVER_SCALE;
        }
        return 0.0f;
    }

    /** DRENCHED hysteresis: applied at 100% wetness, cleared at 50% dry. */
    private static void applyOrClearDrenched(Player player, SurvivalCapability cap) {
        UUID id = player.getUUID();
        if (cap.getWetness() >= 99.5f) {
            if (!StatusManager.INSTANCE.has(id, StatusType.DRENCHED)) {
                StatusManager.INSTANCE.apply(id, StatusType.DRENCHED,
                        player.level().getGameTime(), -1L);
            }
        } else if (cap.getWetness() <= DRENCHED_CLEAR_WETNESS) {
            StatusManager.INSTANCE.remove(id, StatusType.DRENCHED);
        }
    }

    /**
     * Wet-skin evaporative chill, in degrees C, subtracted from the
     * environmental feels-like to get what a wet player's body actually
     * feels: wind-driven (airflow is what makes wet clothes dangerous),
     * scaled by wetness, +2C extra while DRENCHED. Soaked in a calm 20C
     * rain feels ~17C; drenched in a gale feels ~10C — mild weather turns
     * into real exposure, exactly like real life. Applied on top of the
     * heat too (a soaked shirt in a heatwave cools you, like sweating).
     *
     * SINGLE SOURCE: ThirstBodyTempTickHandler calls this for its
     * effective feels-like, so body temp and the wetness system can never
     * drift apart.
     */
    public static float wetChillC(float wetness, float windSpeed, boolean drenched) {
        float chill = (Mth.clamp(wetness, 0.0f, 100.0f) / 100.0f)
                * (WET_CHILL_STILL_AIR + WET_CHILL_PER_WIND * Mth.clamp(windSpeed, 0.0f, 1.0f));
        if (drenched) chill += DRENCHED_EXTRA_CHILL;
        return chill;
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        FEELS_LIKE_CACHE.remove(event.getEntity().getUUID());
    }
}
