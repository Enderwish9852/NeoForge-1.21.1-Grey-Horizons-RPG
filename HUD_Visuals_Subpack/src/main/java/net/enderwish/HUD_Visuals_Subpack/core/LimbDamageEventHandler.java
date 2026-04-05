package net.enderwish.HUD_Visuals_Subpack.core;

import net.enderwish.HUD_Visuals_Subpack.HUDVisualsSubpack;
import net.enderwish.HUD_Visuals_Subpack.network.ModMessages;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Random;

/**
 * Handles logic for the permanent Limb Damage System, Starvation, Exhaustion, and Thirst effects.
 * This version retains the full complex damage logic (fall-off, explosions) while adding the Creative Mode fix.
 */
@EventBusSubscriber(modid = HUDVisualsSubpack.MOD_ID)
public class LimbDamageEventHandler {
    private static final Random RANDOM = new Random();

    // 12000 ticks = 10 minutes total survival
    private static final int MAX_STARVATION_TICKS = 12000;
    // 2400 ticks = 2 minutes "Green Zone" (Grace Period)
    private static final int GREEN_ZONE_MAX_TICKS = 2400;

    /**
     * Resets stats and heals the player on respawn.
     */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.Clone event) {
        if (event.getEntity() instanceof ServerPlayer newPlayer) {
            PlayerCapability newCap = newPlayer.getData(ModAttachments.PLAYER_CAP);
            if (newCap != null) {
                newCap.healAll();
                newCap.setEnergy(100.0f);
                newCap.setThirst(100.0f);
                newCap.setStarvationTimer(0);
                syncToClient(newPlayer, newCap);
            }
        }
    }

    /**
     * Main simulation loop for player stats.
     */
    @SubscribeEvent
    public static void onPlayerTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (!player.isAlive()) return;

            PlayerCapability cap = player.getData(ModAttachments.PLAYER_CAP);
            if (cap == null) return;

            // --- 1. CREATIVE MODE OVERRIDE ---
            if (player.isCreative() || player.isSpectator()) {
                // Check if any limb health, survival stat, or vanilla hunger needs resetting
                if (cap.getHeadHealth() < 100 || cap.getThirst() < 100 || cap.getEnergy() < 100 ||
                        cap.getStarvationTimer() > 0 || player.getFoodData().getFoodLevel() < 20) {

                    cap.healAll();
                    cap.setEnergy(100.0f);
                    cap.setThirst(100.0f);
                    cap.setStarvationTimer(0);

                    // Reset vanilla hunger and saturation for Creative
                    player.getFoodData().setFoodLevel(20);
                    player.getFoodData().setSaturation(20.0f);

                    syncToClient(player, cap);
                }
                return; // Stop all survival depletion logic for creative players
            }

            // --- 2. SURVIVAL LOCK HEALTH ---
            if (player.getHealth() < player.getMaxHealth()) {
                player.setHealth(player.getMaxHealth());
            }

            // --- 3. RUN SIMULATION LOGIC ---
            handleEnergyLogic(player, cap);
            handleStarvationLogic(player, cap);
            handleThirstLogic(player, cap);

            // --- 4. SYNC (Heartbeat every half second) ---
            if (player.level().getGameTime() % 10 == 0) {
                syncToClient(player, cap);
            }
        }
    }

    /**
     * Starvation timer logic with "Tug-of-War" mechanics.
     */
    private static void handleStarvationLogic(ServerPlayer player, PlayerCapability cap) {
        FoodData foodData = player.getFoodData();
        int hunger = foodData.getFoodLevel();
        int timer = cap.getStarvationTimer();

        if (hunger <= 0 && timer == 0) {
            cap.setStarvationTimer(1);
            return;
        }

        if (timer > 0) {
            cap.setStarvationTimer(timer + 1);

            if (timer <= GREEN_ZONE_MAX_TICKS) {
                if (hunger >= 4) {
                    cap.setStarvationTimer(0);
                    return;
                }
            }
            else if (timer > GREEN_ZONE_MAX_TICKS && timer < MAX_STARVATION_TICKS) {
                if (hunger > 0) {
                    int rewoundTime = Math.max(GREEN_ZONE_MAX_TICKS, timer - (hunger * 1200));
                    cap.setStarvationTimer(rewoundTime);
                    foodData.setFoodLevel(0);
                }

                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 1, false, false));
            }

            if (timer > 9600) {
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 40, 1, false, false));
            }

            if (timer >= MAX_STARVATION_TICKS) {
                player.kill();
                cap.setStarvationTimer(0);
            }
        }
    }

    /**
     * Logic for Thirst drainage and dehydration effects.
     */
    private static void handleThirstLogic(ServerPlayer player, PlayerCapability cap) {
        float currentThirst = cap.getThirst();
        float drain = 0.005f;

        if (player.isSprinting()) drain *= 3.0f;

        Holder<Biome> biomeHolder = player.level().getBiome(player.blockPosition());
        float baseTemp = biomeHolder.value().getBaseTemperature();

        if (baseTemp >= 1.5f) {
            drain *= 2.0f;
        }

        cap.setThirst(Math.max(0, currentThirst - drain));

        if (currentThirst <= 0) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, false));

            if (player.level().getGameTime() % 40 == 0) {
                cap.damageTorso(0.5f);
            }
        }
    }

    /**
     * Manages energy recovery and exhaustion effects.
     */
    private static void handleEnergyLogic(ServerPlayer player, PlayerCapability cap) {
        float currentEnergy = cap.getEnergy();
        float hungerPct = player.getFoodData().getFoodLevel() / 20.0f;

        if (player.isSprinting()) {
            float drain = (hungerPct < 0.2f) ? 0.08f : 0.05f;
            cap.setEnergy(Math.max(0, currentEnergy - drain));

            if (cap.getEnergy() <= 0) {
                player.setSprinting(false);
            }
        } else {
            float baseRecovery = (player.getDeltaMovement().horizontalDistanceSqr() < 0.001) ? 0.04f : 0.025f;

            if (hungerPct >= 0.6f) cap.setEnergy(Math.min(100.0f, currentEnergy + (baseRecovery * 1.5f)));
            else if (hungerPct >= 0.2f) cap.setEnergy(Math.min(100.0f, currentEnergy + baseRecovery));
            else cap.setEnergy(Math.max(0, currentEnergy - 0.01f));
        }

        if (currentEnergy <= 0) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, false, false));
        }
    }

    @SubscribeEvent
    public static void onPlayerDamage(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.isCreative()) return;

        PlayerCapability cap = player.getData(ModAttachments.PLAYER_CAP);
        if (cap == null) return;

        float amount = event.getNewDamage();

        if (event.getSource().is(DamageTypes.FALL)) {
            float remainingDamage = amount;
            float footHealthBefore = cap.getLeftFootHealth();
            cap.damageLFoot(remainingDamage);
            cap.damageRFoot(remainingDamage);
            remainingDamage = Math.max(0, remainingDamage - footHealthBefore);

            if (remainingDamage > 0) {
                float legHealthBefore = cap.getLeftLegHealth();
                cap.damageLLeg(remainingDamage);
                cap.damageRLeg(remainingDamage);
                remainingDamage = Math.max(0, remainingDamage - legHealthBefore);
            }

            if (remainingDamage > 0) {
                damageCriticalPart(player, cap, "torso", remainingDamage);
            }

        } else if (event.getSource().is(DamageTypes.EXPLOSION)) {
            damageCriticalPart(player, cap, "torso", amount * 0.7f);
            cap.damageLLeg(amount * 0.15f);
            cap.damageRLeg(amount * 0.15f);
        } else {
            double hitY = RANDOM.nextDouble();
            if (hitY > 0.85) {
                damageCriticalPart(player, cap, "head", amount);
            } else if (hitY > 0.45) {
                damageCriticalPart(player, cap, "torso", amount);
            } else if (hitY > 0.25) {
                if (RANDOM.nextBoolean()) cap.damageLArm(amount); else cap.damageRArm(amount);
            } else if (hitY > 0.10) {
                if (RANDOM.nextBoolean()) cap.damageLLeg(amount); else cap.damageRLeg(amount);
            } else {
                if (RANDOM.nextBoolean()) cap.damageLFoot(amount); else cap.damageRFoot(amount);
            }
        }

        syncToClient(player, cap);
    }

    private static void damageCriticalPart(ServerPlayer player, PlayerCapability cap, String part, float amount) {
        if (part.equals("head")) {
            cap.damageHead(amount);
            if (cap.getHeadHealth() <= 0) player.kill();
        } else if (part.equals("torso")) {
            cap.damageTorso(amount);
            if (cap.getTorsoHealth() <= 0) player.kill();
        }
    }

    private static void syncToClient(ServerPlayer player, PlayerCapability cap) {
        ModMessages.sendToPlayer(new net.enderwish.HUD_Visuals_Subpack.network.LimbSyncPacket(
                cap.getBPM(),
                cap.getEnergy(),
                cap.getThirst(),
                cap.getHunger(),
                cap.hasWatchEquipped(),
                cap.getHeadHealth(),
                cap.getTorsoHealth(),
                cap.getLeftArmHealth(),
                cap.getRightArmHealth(),
                cap.getLeftLegHealth(),
                cap.getRightLegHealth(),
                cap.getLeftFootHealth(),
                cap.getRightFootHealth(),
                cap.getStarvationTimer(),
                cap.getPollenExposure(),
                cap.getCoreTemp(),
                cap.getWetness()
        ), player);
    }
}