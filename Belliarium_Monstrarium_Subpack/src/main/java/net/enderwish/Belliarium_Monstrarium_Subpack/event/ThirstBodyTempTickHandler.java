package net.enderwish.Belliarium_Monstrarium_Subpack.event;

import net.enderwish.Atmospheric_Overhaul_Subpack.api.SeasonsAPI;
import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.ModAttachments;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.status.StatusType;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.survival.SurvivalCapability;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * ThirstBodyTempTickHandler
 *
 * THIRST -- grounded in the survival "rule of threes": a healthy human in
 * temperate conditions lasts about 3 days without water (lab maximum
 * ~100h), so the baseline drain empties 100 thirst over 3 in-game days
 * (72000 ticks) at normal body temperature. Hotter bodies sweat more:
 * the drain scales up 0.3x per degree C above 37 (clamped 0.7x-3.0x for
 * the whole range), and the hyperthermia status multiplies it a further
 * 3x -- at heat stroke (40C+) the 3-day clock collapses toward ~half a
 * day, which matches desert survival reality. Thirst 0 = death
 * (BodyDamageHandler.killByDehydration, through the diary's death report).
 *
 * BODY TEMP -- starts and centers at 37.0C. The "humid spring morning"
 * comfort band is a feels-like of 15-25C (Atmospheric's per-player
 * LocalChunkTemperature, which already includes the season, phase,
 * weather, biome and the nearby heat-block list): inside the band the
 * body holds 37; outside it the target slides 0.15C per degree C away
 * from the band (capped 32.5-41.5C -- standing on magma gives a feels-like
 * of ~70C and pushes straight to the cap), and the body drifts toward the
 * target at a base rate of 0.0004/tick that scales up with how extreme
 * the air feels -- +0.00004 per degree C of feels-like gap, capped at
 * 5x -- so a desert afternoon or a blizzard bites several times harder
 * than a mild evening. Clinical thresholds (real
 * values): below 35C = hypothermia, above 40C = hyperthermia. Both are
 * pure conditional statuses -- active exactly while the temp is past its
 * threshold, no duration to manage. V1 effects only: hyperthermia x3
 * thirst drain, hypothermia greatly increased hunger drain (flat
 * exhaustion per tick).
 *
 * Creative/spectator: pinned at 100 thirst / 37C, no drains -- same
 * treatment the sanity system got.
 *
 * DRINKING (TEST VALUES, will change when the drinks subpack lands):
 * water bottle +25 thirst, milk bucket +15.
 */
@EventBusSubscriber(modid = BelliariumMonstrariumSubpack.MODID)
public class ThirstBodyTempTickHandler {

    // ── thirst ───────────────────────────────────────────────────────────────

    /** 100 thirst over 3 in-game days (3 x 24000 ticks), at 37C body temp. */
    private static final float THIRST_DRAIN_PER_TICK = 100.0f / (3.0f * 24000.0f);
    /** Drain scale per degree C above 37; below 37 it cools toward 0.7x. */
    private static final float THIRST_TEMP_SCALE_PER_DEGREE = 0.3f;
    private static final float THIRST_SCALE_MIN = 0.7f;
    private static final float THIRST_SCALE_MAX = 3.0f;
    /** Hyperthermia status multiplier on top of the continuous scale. */
    private static final float HYPERTHERMIA_THIRST_MULTIPLIER = 3.0f;
    /** Water bottle / milk bucket -- TEST VALUES. */
    private static final float WATER_BOTTLE_THIRST = 25.0f;
    private static final float MILK_THIRST = 15.0f;

    // ── body temperature ──────────────────────────────────────────────────────

    private static final float BODY_TEMP_NORMAL = 37.0f;
    /** Real clinical thresholds: hypothermia < 35C, hyperthermia (heat stroke) > 40C.
     *  The values live on StatusType so the /status derivation uses the exact same ones. */
    private static final float HYPOTHERMIA_THRESHOLD = StatusType.HYPOTHERMIA_THRESHOLD_C;
    private static final float HYPERTHERMIA_THRESHOLD = StatusType.HYPERTHERMIA_THRESHOLD_C;
    /** "Humid spring morning" comfort band, in feels-like C. */
    private static final float NEUTRAL_FEELS_MIN_C = 15.0f;
    private static final float NEUTRAL_FEELS_MAX_C = 25.0f;
    /** Target slides this many C per degree C outside the comfort band. */
    private static final float TEMP_TARGET_SLOPE = 0.15f;
    private static final float TEMP_TARGET_MIN = 32.5f;
    private static final float TEMP_TARGET_MAX = 41.5f;
    /** Base drift: fraction of the target gap closed per tick (a ~2500-tick
     *  time constant in mild conditions, where the felt gap is small). */
    private static final float TEMP_DRIFT_BASE = 0.0004f;
    /** Drift accelerates with the felt extreme: this much extra rate per
     *  degree C of |feels-like - body temp|, so a desert afternoon or a
     *  blizzard bites several times harder than a mild evening. */
    private static final float TEMP_DRIFT_GAP_SCALE = 0.00004f;
    /** Hard cap on the drift rate: 5x the base, however extreme it gets. */
    private static final float TEMP_DRIFT_MAX = 0.002f;
    /** Hypothermia hunger drain: flat exhaustion per tick while active. */
    private static final float HYPOTHERMIA_EXHAUSTION_PER_TICK = 0.008f;

    /** Feels-like re-sample cadence; the chunk scan is not cheap, Atmospheric
     *  itself already runs it on a 10-tick cycle for its own sync. */
    private static final int FEELS_SAMPLE_INTERVAL = 20;

    private static final Map<UUID, Float> FEELS_LIKE_CACHE = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        SurvivalCapability cap = player.getData(ModAttachments.SURVIVAL);

        if (player.isCreative() || player.isSpectator()) {
            // pinned like the sanity system: max thirst, normal temp, no drains
            if (cap.getThirst() < 100.0f || cap.getBodyTemp() != BODY_TEMP_NORMAL) {
                cap.setThirst(100.0f);
                cap.setBodyTemp(BODY_TEMP_NORMAL);
            }
            return;
        }

        if (player.level().getGameTime() % FEELS_SAMPLE_INTERVAL == 0) {
            FEELS_LIKE_CACHE.put(serverPlayer.getUUID(),
                    (float) SeasonsAPI.getLocalChunkTempCelsius(
                            serverPlayer.serverLevel(), serverPlayer.blockPosition()));
        }
        float feelsLikeC = FEELS_LIKE_CACHE.getOrDefault(serverPlayer.getUUID(), 20.0f);

        float target = targetBodyTemp(feelsLikeC);
        float feltGap = Math.abs(feelsLikeC - cap.getBodyTemp());
        float driftRate = Math.min(TEMP_DRIFT_BASE + TEMP_DRIFT_GAP_SCALE * feltGap, TEMP_DRIFT_MAX);
        cap.setBodyTemp(cap.getBodyTemp() + (target - cap.getBodyTemp()) * driftRate);

        boolean hyperthermic = cap.getBodyTemp() > HYPERTHERMIA_THRESHOLD;
        boolean hypothermic = cap.getBodyTemp() < HYPOTHERMIA_THRESHOLD;

        float tempScale = Mth.clamp(
                1.0f + (cap.getBodyTemp() - BODY_TEMP_NORMAL) * THIRST_TEMP_SCALE_PER_DEGREE,
                THIRST_SCALE_MIN, THIRST_SCALE_MAX);
        cap.setThirst(cap.getThirst() - THIRST_DRAIN_PER_TICK * tempScale
                * (hyperthermic ? HYPERTHERMIA_THIRST_MULTIPLIER : 1.0f));

        if (hypothermic) {
            player.causeFoodExhaustion(HYPOTHERMIA_EXHAUSTION_PER_TICK);
        }

        if (cap.getThirst() <= 0.0f) {
            BodyDamageHandler.killByDehydration(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onUseItemFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.isCreative() || player.isSpectator()) return;

        ItemStack stack = event.getItem();
        if (stack.is(Items.MILK_BUCKET)) {
            SurvivalCapability cap = player.getData(ModAttachments.SURVIVAL);
            cap.setThirst(cap.getThirst() + MILK_THIRST);
        } else if (stack.is(Items.POTION)
                && stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY)
                        .is(Potions.WATER)) {
            SurvivalCapability cap = player.getData(ModAttachments.SURVIVAL);
            cap.setThirst(cap.getThirst() + WATER_BOTTLE_THIRST);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        FEELS_LIKE_CACHE.remove(event.getEntity().getUUID());
    }

    /** Body-temp target from the felt environment: 37C inside the comfort
     *  band, sliding away from it outside (capped to 32.5-41.5C). */
    private static float targetBodyTemp(float feelsLikeC) {
        if (feelsLikeC > NEUTRAL_FEELS_MAX_C) {
            return Math.min(TEMP_TARGET_MAX,
                    BODY_TEMP_NORMAL + (feelsLikeC - NEUTRAL_FEELS_MAX_C) * TEMP_TARGET_SLOPE);
        }
        if (feelsLikeC < NEUTRAL_FEELS_MIN_C) {
            return Math.max(TEMP_TARGET_MIN,
                    BODY_TEMP_NORMAL - (NEUTRAL_FEELS_MIN_C - feelsLikeC) * TEMP_TARGET_SLOPE);
        }
        return BODY_TEMP_NORMAL;
    }
}
