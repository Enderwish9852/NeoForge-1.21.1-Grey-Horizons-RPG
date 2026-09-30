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
 * BUGFIX (stamina recovery too slow) -- STAMINA_BASE_REGEN bumped 0.05 -> 0.35
 * (~7x). At zero weight this takes an empty stamina bar from 0->100 in about
 * 14 seconds instead of ~100. No drain constant anywhere in this file was
 * touched. Note: since stamina regen spends energy 1:1 (per last change),
 * energy will now visibly drop FASTER specifically while stamina is
 * recovering than before -- that's the faster regen consuming more energy
 * per second, not a change to energy's own drain rate.
 */
@EventBusSubscriber(modid = BelliariumMonstrariumSubpack.MODID)
public class SurvivalTickHandler {

    // Energy drain (unchanged)
    private static final float BASE_ENERGY_DRAIN_PER_TICK = 0.0015f;
    private static final float WEIGHT_ENERGY_DRAIN_MULTIPLIER = 5.0f;
    private static final float HUNGER_ENERGY_DRAIN_MULTIPLIER = 2.0f;
    private static final float WEIGHT_EXHAUSTION_PER_TICK = 0.002f;

    // Energy regen (unchanged)
    private static final float HUNGER_RAPID_THRESHOLD = 0.5f;
    private static final float RAPID_ENERGY_REGEN_PER_TICK = 0.05f;
    private static final float NORMAL_ENERGY_REGEN_PER_TICK = 0.01f;
    private static final float WEIGHT_REGEN_SLOWDOWN = 0.85f;
    private static final float RAPID_HUNGER_COST_PER_ENERGY = 0.05f;
    private static final float NORMAL_HUNGER_COST_PER_ENERGY = 0.01f;
    private static final float WEIGHT_HUNGER_COST_MULTIPLIER = 4.0f;

    // Stamina regen -- spends energy
    private static final float STAMINA_BASE_REGEN = 0.35f; // was 0.05f -- THE FIX
    private static final float SLEEPING_REGEN_MULTIPLIER = 3.0f;
    private static final float STAMINA_ENERGY_COST_RATIO = 1.0f;
    private static final float STAMINA_SPRINT_DRAIN = 0.15f;
    private static final float RECOVERY_THRESHOLD = 15.0f;
    private static final float MAXED_WEIGHT_STANDING_DRAIN = 0.03f;

    // Jumping (unchanged)
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

        float drainMultiplier = 1.0f
                + weightFraction * WEIGHT_ENERGY_DRAIN_MULTIPLIER
                + (1.0f - hungerFraction) * HUNGER_ENERGY_DRAIN_MULTIPLIER;
        cap.setEnergy(cap.getEnergy() - BASE_ENERGY_DRAIN_PER_TICK * drainMultiplier);

        if (weightFraction > 0f) {
            player.causeFoodExhaustion(WEIGHT_EXHAUSTION_PER_TICK * weightFraction);
        }

        if (cap.getEnergy() < 100f) {
            boolean rapidTier = hungerFraction >= HUNGER_RAPID_THRESHOLD;
            float baseRate = rapidTier ? RAPID_ENERGY_REGEN_PER_TICK : NORMAL_ENERGY_REGEN_PER_TICK;
            float hungerCostRate = rapidTier ? RAPID_HUNGER_COST_PER_ENERGY : NORMAL_HUNGER_COST_PER_ENERGY;

            float regenRate = baseRate * (1.0f - weightFraction * WEIGHT_REGEN_SLOWDOWN);
            float energyGain = Math.min(regenRate, 100f - cap.getEnergy());
            if (energyGain > 0f) {
                cap.setEnergy(cap.getEnergy() + energyGain);
                float exhaustionCost = energyGain * hungerCostRate
                        * (1.0f + weightFraction * WEIGHT_HUNGER_COST_MULTIPLIER);
                player.causeFoodExhaustion(exhaustionCost);
            }
        }

        if (cap.getEnergy() <= 0) {
            cap.setStamina(0);
        } else if (weightFraction >= MAXED_THRESHOLD) {
            cap.setStamina(cap.getStamina() - MAXED_WEIGHT_STANDING_DRAIN);
        } else if (player.isSprinting()) {
            cap.setStamina(cap.getStamina() - STAMINA_SPRINT_DRAIN);
        } else {
            float weightPenalty = 1.0f - weightFraction * 0.6f;
            float regenMultiplier = player.isSleeping() ? SLEEPING_REGEN_MULTIPLIER : 1.0f;

            float staminaGain = STAMINA_BASE_REGEN * weightPenalty * regenMultiplier;
            staminaGain = Math.min(staminaGain, 100f - cap.getStamina());

            float energyCost = staminaGain * STAMINA_ENERGY_COST_RATIO;
            if (energyCost > cap.getEnergy()) {
                staminaGain = cap.getEnergy() / STAMINA_ENERGY_COST_RATIO;
                energyCost = cap.getEnergy();
            }

            if (staminaGain > 0f) {
                cap.setStamina(cap.getStamina() + staminaGain);
                cap.setEnergy(cap.getEnergy() - energyCost);
            }
        }

        if (cap.getStamina() <= 0 && player.isSprinting()) {
            player.setSprinting(false);
        }

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
