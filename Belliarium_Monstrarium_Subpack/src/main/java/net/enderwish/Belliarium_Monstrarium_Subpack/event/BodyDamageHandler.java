package net.enderwish.Belliarium_Monstrarium_Subpack.event;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.gear.ArmorToughnessRegistry;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.body.BodyHealthCapability;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.combat.CombatLogEntry;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.combat.CombatState;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.combat.CombatTimerManager;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.ModAttachments;
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
 * BUGFIX (creative didn't actually heal, just repainted green) -- new
 * onGameModeChange heals the REAL BodyHealthCapability the instant a
 * player switches TO creative, not just the HUD's display. BodyHealthHUD's
 * old display-only creative override is removed in that file now that
 * this is real -- see its own doc comment.
 *
 * VERIFY IF COMPILE FAILS -- PlayerEvent.PlayerChangeGameModeEvent's exact
 * getter names (getCurrentGameMode()/getNewGameMode()) are the standard,
 * long-stable Forge/NeoForge shape for this event, not confirmed against
 * source pasted in this conversation. GameType itself IS already confirmed
 * real in this project (FrontierTitleScreen already imports and uses it).
 */
@EventBusSubscriber(modid = BelliariumMonstrariumSubpack.MODID)
public class BodyDamageHandler {

    private static final Random RANDOM = new Random();
    private static final double SAFE_LANDING_METERS = 1.5;
    private static final double DAMAGE_PER_JOULE = 0.011;
    private static final double REFERENCE_MASS_KG = 60.0;

    private static boolean intentionalKillInProgress = false;

    private static final Set<UUID> pendingDeaths = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, FatalInfo> pendingFatalInfo = new ConcurrentHashMap<>();

    private record FatalInfo(String sourceName, double distance, String bodyPart, boolean wasInCombat) {}
    private record SourceInfo(String name, double distance) {}

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        BodyHealthCapability cap = player.getData(ModAttachments.BODY_HEALTH);
        sync(player, cap);
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer newPlayer)) return;
        BodyHealthCapability cap = newPlayer.getData(ModAttachments.BODY_HEALTH);
        cap.healAll();
    }

    @SubscribeEvent
    public static void onPlayerRespawnPlaced(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        BodyHealthCapability cap = player.getData(ModAttachments.BODY_HEALTH);
        sync(player, cap);
    }

    /** THE FIX -- see class doc comment. */
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
            if (player.isAlive()) {
                killIntentionally(player);
            } else {
                pendingFatalInfo.remove(uuid);
            }
            return;
        }

        if (player.isCreative() || player.isSpectator()) return;
        if (player.isAlive() && player.getHealth() < player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    @SubscribeEvent
    public static void onLivingHeal(LivingHealEvent event) {
        if (event.getEntity() instanceof ServerPlayer) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.isCreative()) return;

        event.setCanceled(true);

        float realDamage = calculateFallDamage(event.getDistance(), REFERENCE_MASS_KG);
        if (realDamage > 0) {
            player.hurt(player.damageSources().fall(), realDamage);
        }
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
            damageCritical(player, cap, true, headPortion, source, "Head");

            float leftLegPortion = mitigateIfApplicable(player, EquipmentSlot.LEGS, amount * 0.15f, armorApplies);
            cap.damageLeftLeg(leftLegPortion);
            logCombatDamage(player, source, leftLegPortion, "Left Leg");

            float rightLegPortion = mitigateIfApplicable(player, EquipmentSlot.LEGS, amount * 0.15f, armorApplies);
            cap.damageRightLeg(rightLegPortion);
            logCombatDamage(player, source, rightLegPortion, "Right Leg");
        } else {
            double roll = RANDOM.nextDouble();
            if (roll > 0.85) {
                float applied = mitigateIfApplicable(player, EquipmentSlot.HEAD, amount, armorApplies);
                damageCritical(player, cap, true, applied, source, "Head");
            } else if (roll > 0.45) {
                float applied = mitigateIfApplicable(player, EquipmentSlot.CHEST, amount, armorApplies);
                damageCritical(player, cap, false, applied, source, "Torso");
            } else if (roll > 0.25) {
                if (RANDOM.nextBoolean()) { cap.damageLeftArm(amount); logCombatDamage(player, source, amount, "Left Arm"); }
                else { cap.damageRightArm(amount); logCombatDamage(player, source, amount, "Right Arm"); }
            } else if (roll > 0.10) {
                float applied = mitigateIfApplicable(player, EquipmentSlot.LEGS, amount, armorApplies);
                if (RANDOM.nextBoolean()) { cap.damageLeftLeg(applied); logCombatDamage(player, source, applied, "Left Leg"); }
                else { cap.damageRightLeg(applied); logCombatDamage(player, source, applied, "Right Leg"); }
            } else {
                float applied = mitigateIfApplicable(player, EquipmentSlot.FEET, amount, armorApplies);
                if (RANDOM.nextBoolean()) { cap.damageLeftFoot(applied); logCombatDamage(player, source, applied, "Left Foot"); }
                else { cap.damageRightFoot(applied); logCombatDamage(player, source, applied, "Right Foot"); }
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
        cap.damageLeftLeg(mitigatedPerLeg);
        cap.damageRightLeg(mitigatedPerLeg);
        logCombatDamage(player, source, mitigatedPerLeg, "Left Leg (fall)");
        logCombatDamage(player, source, mitigatedPerLeg, "Right Leg (fall)");

        float torsoOverflow = Math.max(0, mitigatedPerLeg - leftLegBefore) + Math.max(0, mitigatedPerLeg - rightLegBefore);
        if (torsoOverflow > 0) {
            float mitigatedTorso = mitigateIfApplicable(player, EquipmentSlot.CHEST, torsoOverflow, armorApplies);
            damageCritical(player, cap, false, mitigatedTorso, source, "Torso (fall)");
        }
    }

    public static float calculateFallDamage(double fallHeightMeters, double massKg) {
        double effectiveHeight = Math.max(0, fallHeightMeters - SAFE_LANDING_METERS);
        double joules = massKg * 9.8 * effectiveHeight;
        return (float) (joules * DAMAGE_PER_JOULE);
    }

    private static void damageCritical(ServerPlayer player, BodyHealthCapability cap, boolean head, float amount,
                                       DamageSource source, String bodyPart) {
        if (head) {
            cap.damageHead(amount);
            logCombatDamage(player, source, amount, bodyPart);
            if (cap.isHeadFatal()) markPendingDeath(player, source, bodyPart);
        } else {
            cap.damageTorso(amount);
            logCombatDamage(player, source, amount, bodyPart);
            if (cap.isTorsoFatal()) markPendingDeath(player, source, bodyPart);
        }
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

    private static void markPendingDeath(ServerPlayer player, DamageSource source, String bodyPart) {
        UUID uuid = player.getUUID();
        if (pendingDeaths.contains(uuid)) return;

        SourceInfo info = describeSource(source, player);
        boolean wasInCombat = CombatTimerManager.INSTANCE.isInCombat(uuid);
        pendingFatalInfo.put(uuid, new FatalInfo(info.name(), info.distance(), bodyPart, wasInCombat));
        pendingDeaths.add(uuid);
    }

    private static void killIntentionally(ServerPlayer player) {
        intentionalKillInProgress = true;
        try {
            player.kill();
        } finally {
            intentionalKillInProgress = false;
        }
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
        if (amount <= 0f) return;
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
