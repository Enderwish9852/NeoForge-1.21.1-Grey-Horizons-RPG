package net.enderwish.Belliarium_Monstrarium_Subpack.event;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.ModAttachments;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.SurvivalCapability;
import net.enderwish.Belliarium_Monstrarium_Subpack.network.ModMessages;
import net.enderwish.Belliarium_Monstrarium_Subpack.network.SurvivalSyncPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEvent.LivingJumpEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * SurvivalTickHandler
 *
 * NEW -- energy regeneration, gated by a 50% food threshold:
 *   - Hunger >= 50% ("rapid" tier): actively pushes energy toward 100%,
 *     paying for it with food exhaustion (same causeFoodExhaustion mechanism
 *     the weight-drain line below already uses). Weight both slows this
 *     regen AND multiplies its exhaustion cost.
 *   - Hunger < 50% ("passive" tier): a much slower trickle, no extra
 *     exhaustion cost, still slowed further by weight.
 *   hungerFraction is re-read fresh every tick, so if rapid-tier regen ever
 *   pulls hunger below 50%, the very next tick naturally drops into the
 *   passive tier -- that's what makes "keep energy at 100% if all possible"
 *   self-limiting without a separate hard cutoff.
 *   RAPID_/PASSIVE_ENERGY_REGEN_PER_TICK and the hunger-cost constants below
 *   are first-pass tuning numbers, not derived from anything -- adjust freely.
 *
 * BUGFIX (sync timing) -- same issue as BodyDamageHandler: sending
 * SurvivalSyncPacket during PlayerEvent.Clone is unreliable. Data mutation
 * (healAll()) stays on Clone; the network sync now happens on
 * PlayerRespawnEvent instead.
 *
 * NEW -- onPlayerJoin syncs the persisted SurvivalCapability to the client
 * on login/rejoin, which previously never happened at all.
 */
@EventBusSubscriber(modid = BelliariumMonstrariumSubpack.MODID)
public class SurvivalTickHandler {

    // Energy drain
    private static final float BASE_ENERGY_DRAIN_PER_TICK = 0.0015f;
    private static final float WEIGHT_ENERGY_DRAIN_MULTIPLIER = 5.0f;
    private static final float HUNGER_ENERGY_DRAIN_MULTIPLIER = 2.0f;
    private static final float WEIGHT_EXHAUSTION_PER_TICK = 0.002f;

    // Energy regen (new)
    private static final float HUNGER_RAPID_THRESHOLD = 0.5f;
    private static final float RAPID_ENERGY_REGEN_PER_TICK = 0.05f;
    private static final float PASSIVE_ENERGY_REGEN_PER_TICK = 0.005f;
    private static final float WEIGHT_REGEN_SLOWDOWN = 0.85f;
    private static final float RAPID_HUNGER_COST_PER_ENERGY = 0.05f;
    private static final float WEIGHT_HUNGER_COST_MULTIPLIER = 4.0f;

    // Stamina
    private static final float STAMINA_SPRINT_DRAIN = 0.15f;
    private static final float STAMINA_BASE_REGEN = 0.05f;
    private static final float SLEEPING_REGEN_MULTIPLIER = 3.0f;
    private static final float RECOVERY_THRESHOLD = 15.0f;
    private static final float MAXED_WEIGHT_STANDING_DRAIN = 0.03f;

    // Jumping
    private static final float MAXED_THRESHOLD = 0.98f;
    private static final float HALF_HEIGHT_WEIGHT_THRESHOLD = 0.5f;
    private static final float BASE_JUMP_STAMINA_COST = 3.0f;
    private static final float JUMP_COST_WEIGHT_MULTIPLIER = 4.0f;
    private static final double HALF_HEIGHT_VELOCITY_SCALE = 0.63;

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        SurvivalCapability cap = player.getData(ModAttachments.SURVIVAL);
        ModMessages.sendToPlayer(
                new SurvivalSyncPacket(cap.getEnergy(), cap.getStamina(), cap.isCollapsed()), player);
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer newPlayer)) return;
        SurvivalCapability cap = newPlayer.getData(ModAttachments.SURVIVAL);
        cap.healAll();
        // Sync moved to PlayerRespawnEvent -- see class doc comment.
    }

    @SubscribeEvent
    public static void onPlayerRespawnPlaced(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        SurvivalCapability cap = player.getData(ModAttachments.SURVIVAL);
        ModMessages.sendToPlayer(
                new SurvivalSyncPacket(cap.getEnergy(), cap.getStamina(), cap.isCollapsed()), player);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        if (player.isCreative() || player.isSpectator()) return;

        SurvivalCapability cap = player.getData(ModAttachments.SURVIVAL);
        float weightFraction = Mth.clamp(WeightEnforcementHandler.getWeightFraction(player), 0f, 1f);
        float hungerFraction = player.getFoodData().getFoodLevel() / 20.0f;

        // Energy drain: scales with weight and how hungry the player is
        float drainMultiplier = 1.0f
                + weightFraction * WEIGHT_ENERGY_DRAIN_MULTIPLIER
                + (1.0f - hungerFraction) * HUNGER_ENERGY_DRAIN_MULTIPLIER;
        cap.setEnergy(cap.getEnergy() - BASE_ENERGY_DRAIN_PER_TICK * drainMultiplier);

        // Heavy carrying also makes vanilla hunger drain a little faster
        if (weightFraction > 0f) {
            player.causeFoodExhaustion(WEIGHT_EXHAUSTION_PER_TICK * weightFraction);
        }

        // Energy regen: two hunger tiers, both slowed by weight (see class doc comment)
        if (cap.getEnergy() < 100f) {
            if (hungerFraction >= HUNGER_RAPID_THRESHOLD) {
                float regenRate = RAPID_ENERGY_REGEN_PER_TICK * (1.0f - weightFraction * WEIGHT_REGEN_SLOWDOWN);
                float energyGain = Math.min(regenRate, 100f - cap.getEnergy());
                if (energyGain > 0f) {
                    cap.setEnergy(cap.getEnergy() + energyGain);
                    float exhaustionCost = energyGain * RAPID_HUNGER_COST_PER_ENERGY
                            * (1.0f + weightFraction * WEIGHT_HUNGER_COST_MULTIPLIER);
                    player.causeFoodExhaustion(exhaustionCost);
                }
            } else {
                float passiveRate = PASSIVE_ENERGY_REGEN_PER_TICK * (1.0f - weightFraction * WEIGHT_REGEN_SLOWDOWN);
                if (passiveRate > 0f) {
                    cap.setEnergy(cap.getEnergy() + Math.min(passiveRate, 100f - cap.getEnergy()));
                }
            }
        }

        // Stamina
        if (cap.getEnergy() <= 0) {
            cap.setStamina(0);
        } else if (weightFraction >= MAXED_THRESHOLD) {
            cap.setStamina(cap.getStamina() - MAXED_WEIGHT_STANDING_DRAIN);
        } else if (player.isSprinting()) {
            cap.setStamina(cap.getStamina() - STAMINA_SPRINT_DRAIN);
        } else {
            float weightPenalty = 1.0f - weightFraction * 0.6f;
            float energyEfficiency = Math.max(0.1f, cap.getEnergy() / 100.0f);
            float regenMultiplier = player.isSleeping() ? SLEEPING_REGEN_MULTIPLIER : 1.0f;
            cap.setStamina(cap.getStamina()
                    + STAMINA_BASE_REGEN * weightPenalty * energyEfficiency * regenMultiplier);
        }

        if (cap.getStamina() <= 0 && player.isSprinting()) {
            player.setSprinting(false);
        }

        // Collapse
        if (!cap.isCollapsed() && cap.getEnergy() <= 0 && cap.getStamina() <= 0) {
            cap.setCollapsed(true);
        } else if (cap.isCollapsed()
                && cap.getEnergy() > RECOVERY_THRESHOLD && cap.getStamina() > RECOVERY_THRESHOLD) {
            cap.setCollapsed(false);
        }

        if (cap.isCollapsed()) {
            player.setShiftKeyDown(true);
            player.setSprinting(false);
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 9, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 9, false, false));
        } else if (cap.getStamina() <= 0 || cap.getEnergy() <= 0) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, false));
        }

        if (player.level().getGameTime() % 10 == 0) {
            ModMessages.sendToPlayer(
                    new SurvivalSyncPacket(cap.getEnergy(), cap.getStamina(), cap.isCollapsed()),
                    (ServerPlayer) player);
        }
    }

    @SubscribeEvent
    public static void onLivingJump(LivingJumpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.isCreative() || player.isSpectator()) return;

        float weightFraction = Mth.clamp(WeightEnforcementHandler.getWeightFraction(player), 0f, 1f);
        Vec3 motion = player.getDeltaMovement();

        if (weightFraction >= MAXED_THRESHOLD) {
            player.setDeltaMovement(motion.x, 0.0, motion.z);
            return;
        }

        if (weightFraction >= HALF_HEIGHT_WEIGHT_THRESHOLD) {
            player.setDeltaMovement(motion.x, motion.y * HALF_HEIGHT_VELOCITY_SCALE, motion.z);
        }

        SurvivalCapability cap = player.getData(ModAttachments.SURVIVAL);
        float cost = BASE_JUMP_STAMINA_COST * (1.0f + weightFraction * JUMP_COST_WEIGHT_MULTIPLIER);
        cap.setStamina(cap.getStamina() - cost);
    }
}
