package net.enderwish.Belliarium_Monstrarium_Subpack.event;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.body.BodyHealthCapability;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.combat.CombatLogEntry;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.combat.CombatState;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.combat.CombatTimerManager;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.ModAttachments;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.gear.ArmorToughnessRegistry;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.medical.AdrenalineManager;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.survival.SurvivalCapability;
import net.enderwish.Belliarium_Monstrarium_Subpack.network.BodyHealthSyncPacket;
import net.enderwish.Belliarium_Monstrarium_Subpack.network.DeathReportPacket;
import net.enderwish.Belliarium_Monstrarium_Subpack.network.ModMessages;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BodyDamageHandler
 *
 * NEW -- Adrenaline integration. applyToBodyPart() is the single new choke
 * point every damage path now routes through: if Adrenaline is active for
 * the player, the (bodyPart, amount) pair is logged into AdrenalineManager
 * INSTEAD of being applied -- no cap mutation, no fatal check, no combat-
 * log entry, matching "all damage will be logged" / "will not take any
 * health damage" during the window. Armor mitigation (mitigateIfApplicable)
 * is UNCHANGED and still runs in real time either way -- armor is a
 * physical object that still visibly wears down; only the body's own
 * injury bookkeeping is what's deferred.
 *
 * resolveAdrenalineIfExpired(), called every tick, replays every logged
 * part the instant the window ends: real cap.damageXxx() + the normal
 * fatal check (so a lethal logged total still kills, exactly as it would
 * have in real time, just all at once) + a combat-log entry per part so
 * the Death Report scoreboard still shows what actually happened. Then:
 * stamina+energy to 0, hunger to 1 (half a drumstick), and stamina locked
 * from regenerating for a further window (see SurvivalTickHandler).
 */
@EventBusSubscriber(modid = BelliariumMonstrariumSubpack.MODID)
public class BodyDamageHandler {

    private static final Random RANDOM = new Random();
    private static final double SAFE_LANDING_METERS = 1.5;
    private static final double DAMAGE_PER_JOULE = 0.011;
    private static final double REFERENCE_MASS_KG = 60.0;
    private static final int ADRENALINE_STAMINA_LOCK_TICKS = 2400;

    private static boolean intentionalKillInProgress = false;

    private static final Set<UUID> pendingDeaths = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, FatalInfo> pendingFatalInfo = new ConcurrentHashMap<>();

    private record FatalInfo(String sourceName, double distance, String bodyPart, boolean wasInCombat) {}
    private record SourceInfo(String name, double distance) {}

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        sync(player, player.getData(ModAttachments.BODY_HEALTH));
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer newPlayer)) return;
        newPlayer.getData(ModAttachments.BODY_HEALTH).healAll();
    }

    @SubscribeEvent
    public static void onPlayerRespawnPlaced(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        sync(player, player.getData(ModAttachments.BODY_HEALTH));
    }

    @SubscribeEvent
    public static void onGameModeChange(PlayerEvent.PlayerChangeGameModeEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getNewGameMode() != GameType.CREATIVE) return;
        BodyHealthCapability cap = player.getData(ModAttachments.BODY_HEALTH);
        cap.healAll();
        sync(player, cap);
    }

    @SubscribeEvent
    public static void onPlayerTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        UUID uuid = player.getUUID();
        if (pendingDeaths.remove(uuid)) {
            if (player.isAlive()) killIntentionally(player);
            else pendingFatalInfo.remove(uuid);
            return;
        }

        resolveAdrenalineIfExpired(player);

        if (player.isCreative() || player.isSpectator()) return;
        if (player.isAlive() && player.getHealth() < player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    private static void resolveAdrenalineIfExpired(ServerPlayer player) {
        AdrenalineManager.ActiveAdrenaline active = AdrenalineManager.INSTANCE.get(player.getUUID());
        if (active == null) return;
        if (player.level().getGameTime() < active.endTick) return;

        BodyHealthCapability cap = player.getData(ModAttachments.BODY_HEALTH);
        for (Map.Entry<String, AdrenalineManager.PendingDamage> entry : active.pendingByPart.entrySet()) {
            applyNow(player, cap, entry.getKey(), entry.getValue().amount, entry.getValue().lastSource);
        }
        AdrenalineManager.INSTANCE.clear(player.getUUID());

        SurvivalCapability survival = player.getData(ModAttachments.SURVIVAL);
        survival.setEnergy(0f);
        survival.setStamina(0f);
        player.getFoodData().setFoodLevel(1);
        AdrenalineManager.INSTANCE.lockStaminaUntil(player.getUUID(),
                player.level().getGameTime() + ADRENALINE_STAMINA_LOCK_TICKS);

        if (player.isAlive() && player.getHealth() < player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
        sync(player, cap);
    }

    @SubscribeEvent
    public static void onLivingHeal(LivingHealEvent event) {
        if (event.getEntity() instanceof ServerPlayer) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.isCreative()) return;
        event.setCanceled(true);
        float realDamage = calculateFallDamage(event.getDistance(), REFERENCE_MASS_KG);
        if (realDamage > 0) player.hurt(player.damageSources().fall(), realDamage);
    }

    @SubscribeEvent
    public static void onPlayerDamage(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.isCreative()) return;
        if (intentionalKillInProgress) return;

        BodyHealthCapability cap = player.getData(ModAttachments.BODY_HEALTH);
        DamageSource source = event.getSource();
        float amount = event.getNewDamage();
        boolean armorApplies = !source.is(DamageTypeTags.BYPASSES_ARMOR);

        if (source.is(DamageTypes.FALL)) {
            handleFallDamage(player, cap, source, amount, armorApplies);
        } else if (source.is(DamageTypes.EXPLOSION)) {
            float headPortion = mitigateIfApplicable(player, EquipmentSlot.HEAD, amount * 0.7f, armorApplies);
            applyToBodyPart(player, cap, "Head", headPortion, source, true);

            float leftLegPortion = mitigateIfApplicable(player, EquipmentSlot.LEGS, amount * 0.15f, armorApplies);
            applyToBodyPart(player, cap, "Left Leg", leftLegPortion, source, false);

            float rightLegPortion = mitigateIfApplicable(player, EquipmentSlot.LEGS, amount * 0.15f, armorApplies);
            applyToBodyPart(player, cap, "Right Leg", rightLegPortion, source, false);
        } else {
            double roll = RANDOM.nextDouble();
            if (roll > 0.85) {
                float applied = mitigateIfApplicable(player, EquipmentSlot.HEAD, amount, armorApplies);
                applyToBodyPart(player, cap, "Head", applied, source, true);
            } else if (roll > 0.45) {
                float applied = mitigateIfApplicable(player, EquipmentSlot.CHEST, amount, armorApplies);
                applyToBodyPart(player, cap, "Torso", applied, source, true);
            } else if (roll > 0.25) {
                String part = RANDOM.nextBoolean() ? "Left Arm" : "Right Arm";
                applyToBodyPart(player, cap, part, amount, source, false);
            } else if (roll > 0.10) {
                float applied = mitigateIfApplicable(player, EquipmentSlot.LEGS, amount, armorApplies);
                String part = RANDOM.nextBoolean() ? "Left Leg" : "Right Leg";
                applyToBodyPart(player, cap, part, applied, source, false);
            } else {
                float applied = mitigateIfApplicable(player, EquipmentSlot.FEET, amount, armorApplies);
                String part = RANDOM.nextBoolean() ? "Left Foot" : "Right Foot";
                applyToBodyPart(player, cap, part, applied, source, false);
            }
        }

        if (player.isAlive() && player.getHealth() < player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
        sync(player, cap);
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.isCreative()) return;

        if (intentionalKillInProgress) {
            sendDeathReport(player);
            return;
        }
        event.setCanceled(true);
        player.setHealth(player.getMaxHealth());
    }

    private static void handleFallDamage(ServerPlayer player, BodyHealthCapability cap, DamageSource source,
                                         float amount, boolean armorApplies) {
        float perLeg = amount / 2.0f;
        float mitigatedPerLeg = mitigateIfApplicable(player, EquipmentSlot.LEGS, perLeg, armorApplies);

        float leftLegBefore = cap.getLeftLeg();
        float rightLegBefore = cap.getRightLeg();
        applyToBodyPart(player, cap, "Left Leg (fall)", mitigatedPerLeg, source, false);
        applyToBodyPart(player, cap, "Right Leg (fall)", mitigatedPerLeg, source, false);

        float torsoOverflow = Math.max(0, mitigatedPerLeg - leftLegBefore) + Math.max(0, mitigatedPerLeg - rightLegBefore);
        if (torsoOverflow > 0) {
            float mitigatedTorso = mitigateIfApplicable(player, EquipmentSlot.CHEST, torsoOverflow, armorApplies);
            applyToBodyPart(player, cap, "Torso (fall)", mitigatedTorso, source, true);
        }
    }

    public static float calculateFallDamage(double fallHeightMeters, double massKg) {
        double effectiveHeight = Math.max(0, fallHeightMeters - SAFE_LANDING_METERS);
        double joules = massKg * 9.8 * effectiveHeight;
        return (float) (joules * DAMAGE_PER_JOULE);
    }

    /** THE adrenaline choke point -- see class doc comment. */
    private static void applyToBodyPart(ServerPlayer player, BodyHealthCapability cap, String bodyPart,
                                        float amount, DamageSource source, boolean isCritical) {
        if (amount <= 0f) return;

        if (AdrenalineManager.INSTANCE.isActive(player.getUUID())) {
            AdrenalineManager.INSTANCE.logDamage(player.getUUID(), bodyPart, amount, source);
            return;
        }
        applyNow(player, cap, bodyPart, amount, source);
        if (isCritical) checkFatal(player, cap, bodyPart);
    }

    /** Used both for the live (non-adrenaline) path above and for the end-of-window replay. */
    private static void applyNow(ServerPlayer player, BodyHealthCapability cap, String bodyPart,
                                 float amount, DamageSource source) {
        switch (bodyPart) {
            case "Head" -> cap.damageHead(amount);
            case "Torso", "Torso (fall)" -> cap.damageTorso(amount);
            case "Left Arm" -> cap.damageLeftArm(amount);
            case "Right Arm" -> cap.damageRightArm(amount);
            case "Left Leg", "Left Leg (fall)" -> cap.damageLeftLeg(amount);
            case "Right Leg", "Right Leg (fall)" -> cap.damageRightLeg(amount);
            case "Left Foot" -> cap.damageLeftFoot(amount);
            case "Right Foot" -> cap.damageRightFoot(amount);
            default -> { return; }
        }
        logCombatDamage(player, source, amount, bodyPart);
        if (bodyPart.startsWith("Head") || bodyPart.startsWith("Torso")) checkFatal(player, cap, bodyPart);
    }

    private static void checkFatal(ServerPlayer player, BodyHealthCapability cap, String bodyPart) {
        if (bodyPart.startsWith("Head") && cap.isHeadFatal()) markPendingDeath(player, null, bodyPart);
        if (bodyPart.startsWith("Torso") && cap.isTorsoFatal()) markPendingDeath(player, null, bodyPart);
    }

    private static void markPendingDeath(ServerPlayer player, DamageSource source, String bodyPart) {
        UUID uuid = player.getUUID();
        if (pendingDeaths.contains(uuid)) return;

        SourceInfo info = source != null ? describeSource(source, player) : new SourceInfo("unknown causes", -1);
        boolean wasInCombat = CombatTimerManager.INSTANCE.isInCombat(uuid);
        pendingFatalInfo.put(uuid, new FatalInfo(info.name(), info.distance(), bodyPart, wasInCombat));
        pendingDeaths.add(uuid);
    }

    private static float mitigateIfApplicable(ServerPlayer player, EquipmentSlot slot, float rawAmount, boolean armorApplies) {
        if (!armorApplies || rawAmount <= 0f) return rawAmount;
        ItemStack armorStack = player.getItemBySlot(slot);
        if (armorStack.isEmpty()) return rawAmount;
        if (!(player.level() instanceof ServerLevel serverLevel)) return rawAmount;

        float toughness = ArmorToughnessRegistry.INSTANCE.getToughness(armorStack);
        float mitigated = Math.max(0f, rawAmount - toughness);
        int remainingDurability = armorStack.getMaxDamage() - armorStack.getDamageValue();

        if (mitigated <= remainingDurability) {
            if (mitigated > 0f) {
                armorStack.hurtAndBreak((int) Math.ceil(mitigated), serverLevel, player,
                        item -> player.setItemSlot(slot, ItemStack.EMPTY));
            }
            return 0f;
        } else {
            float spillover = mitigated - remainingDurability;
            if (remainingDurability > 0) {
                armorStack.hurtAndBreak(remainingDurability, serverLevel, player,
                        item -> player.setItemSlot(slot, ItemStack.EMPTY));
            }
            return spillover;
        }
    }

    private static void killIntentionally(ServerPlayer player) {
        intentionalKillInProgress = true;
        try { player.kill(); } finally { intentionalKillInProgress = false; }
    }

    private static SourceInfo describeSource(DamageSource source, ServerPlayer player) {
        if (source.getEntity() != null) {
            return new SourceInfo(source.getEntity().getName().getString(),
                    source.getEntity().position().distanceTo(player.position()));
        } else if (source.getDirectEntity() != null) {
            return new SourceInfo(source.getDirectEntity().getName().getString(),
                    source.getDirectEntity().position().distanceTo(player.position()));
        }
        return new SourceInfo(source.getMsgId(), -1);
    }

    private static void logCombatDamage(ServerPlayer player, DamageSource source, float amount, String bodyPart) {
        if (amount <= 0f || source == null) return;
        CombatState state = CombatTimerManager.INSTANCE.getOrCreate(player.getUUID());
        if (!state.isInCombat()) return;

        int secondsIntoBattle = (int) ((player.level().getGameTime() - state.getCombatStartTick()) / 20L);
        SourceInfo info = describeSource(source, player);
        state.logDamage(new CombatLogEntry(secondsIntoBattle, info.name(), info.distance(), amount, bodyPart));
    }

    private static void sendDeathReport(ServerPlayer player) {
        UUID uuid = player.getUUID();
        FatalInfo info = pendingFatalInfo.remove(uuid);
        CombatState state = CombatTimerManager.INSTANCE.getOrCreate(uuid);
        BodyHealthCapability cap = player.getData(ModAttachments.BODY_HEALTH);

        boolean combatDeath = info != null && info.wasInCombat();
        List<CombatLogEntry> log = combatDeath ? new ArrayList<>(state.getLog()) : List.of();

        String headline;
        if (combatDeath) {
            headline = player.getName().getString() + " was killed by " + info.sourceName()
                    + ". Cause of death: " + info.bodyPart() + ".";
        } else {
            headline = player.getCombatTracker().getDeathMessage().getString();
        }

        ModMessages.sendToPlayer(new DeathReportPacket(
                combatDeath, headline,
                cap.getHeadPct(), cap.getTorsoPct(), cap.getLeftArmPct(), cap.getRightArmPct(),
                cap.getLeftLegPct(), cap.getRightLegPct(), cap.getLeftFootPct(), cap.getRightFootPct(),
                log
        ), player);
    }

    private static void sync(ServerPlayer player, BodyHealthCapability cap) {
        ModMessages.sendToPlayer(new BodyHealthSyncPacket(
                cap.getHead(), cap.getTorso(), cap.getLeftArm(), cap.getRightArm(),
                cap.getLeftLeg(), cap.getRightLeg(), cap.getLeftFoot(), cap.getRightFoot()
        ), player);
    }
}
