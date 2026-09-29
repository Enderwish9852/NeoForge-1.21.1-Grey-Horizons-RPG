package net.enderwish.Belliarium_Monstrarium_Subpack.event;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.BodyHealthCapability;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.CombatLogEntry;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.CombatState;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.CombatTimerManager;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.ModAttachments;
import net.enderwish.Belliarium_Monstrarium_Subpack.network.BodyHealthSyncPacket;
import net.enderwish.Belliarium_Monstrarium_Subpack.network.DeathReportPacket;
import net.enderwish.Belliarium_Monstrarium_Subpack.network.ModMessages;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
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
 * BUGFIX (double-death / stuck respawn screen) -- see class-level explanation
 * in the accompanying chat message. Short version: killIntentionally() used to
 * call player.kill() SYNCHRONOUSLY from inside the same LivingDamageEvent.Post
 * call stack as the original mob attack, causing vanilla's own health<=0->die()
 * check to fire a SECOND time on the outer call once control returned to it --
 * two overlapping death sequences on the same entity, same tick. Fix: a fatal
 * hit now only records the UUID in pendingDeaths (+ fatal-hit info in
 * pendingFatalInfo); the real kill() call happens on the NEXT tick from
 * onPlayerTick, fully outside any damage-event call stack.
 *
 * NEW -- markPendingDeath snapshots the fatal hit's source name/distance and
 * whether a Combat Timer battle was active, for the Death Report screen.
 * sendDeathReport (called once the deferred kill's LivingDeathEvent comes
 * through) builds and sends that report: full combat log + "<Player> was
 * killed by <Source>. Cause of death: <BodyPart>." for an in-battle death, or
 * vanilla's own built-in death message for a death with no active battle.
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
        String bodyPart;

        if (source.is(DamageTypes.FALL)) {
            bodyPart = handleFallDamage(player, cap, source, amount);
        } else if (source.is(DamageTypes.EXPLOSION)) {
            damageCritical(player, cap, true, amount * 0.7f, source, "Head");
            cap.damageLeftLeg(amount * 0.15f);
            cap.damageRightLeg(amount * 0.15f);
            bodyPart = "Head/Legs (explosion)";
        } else {
            double roll = RANDOM.nextDouble();
            if (roll > 0.85) {
                damageCritical(player, cap, true, amount, source, "Head");
                bodyPart = "Head";
            } else if (roll > 0.45) {
                damageCritical(player, cap, false, amount, source, "Torso");
                bodyPart = "Torso";
            } else if (roll > 0.25) {
                if (RANDOM.nextBoolean()) { cap.damageLeftArm(amount); bodyPart = "Left Arm"; }
                else { cap.damageRightArm(amount); bodyPart = "Right Arm"; }
            } else if (roll > 0.10) {
                if (RANDOM.nextBoolean()) { cap.damageLeftLeg(amount); bodyPart = "Left Leg"; }
                else { cap.damageRightLeg(amount); bodyPart = "Right Leg"; }
            } else {
                if (RANDOM.nextBoolean()) { cap.damageLeftFoot(amount); bodyPart = "Left Foot"; }
                else { cap.damageRightFoot(amount); bodyPart = "Right Foot"; }
            }
        }

        logCombatDamage(player, source, amount, bodyPart);

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

    private static String handleFallDamage(ServerPlayer player, BodyHealthCapability cap, DamageSource source, float amount) {
        float perLeg = amount / 2.0f;

        float leftLegBefore = cap.getLeftLeg();
        float rightLegBefore = cap.getRightLeg();
        cap.damageLeftLeg(perLeg);
        cap.damageRightLeg(perLeg);

        float torsoOverflow = Math.max(0, perLeg - leftLegBefore) + Math.max(0, perLeg - rightLegBefore);
        if (torsoOverflow > 0) {
            damageCritical(player, cap, false, torsoOverflow, source, "Torso");
            return "Legs/Torso (fall)";
        }
        return "Legs (fall)";
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
            if (cap.isHeadFatal()) markPendingDeath(player, source, bodyPart);
        } else {
            cap.damageTorso(amount);
            if (cap.isTorsoFatal()) markPendingDeath(player, source, bodyPart);
        }
    }

    private static void markPendingDeath(ServerPlayer player, DamageSource source, String bodyPart) {
        UUID uuid = player.getUUID();
        if (pendingDeaths.contains(uuid)) return; // keep the first fatal cause this tick

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
        CombatState state = CombatTimerManager.INSTANCE.getOrCreate(player.getUUID());
        if (!state.isInCombat()) return;

        int secondsIntoBattle = (int) ((player.level().getGameTime() - state.getCombatStartTick()) / 20L);
        SourceInfo info = describeSource(source, player);
        state.logDamage(new CombatLogEntry(secondsIntoBattle, info.name(), info.distance(), amount, bodyPart));
    }

    /**
     * VERIFY IF COMPILE FAILS -- CombatTracker#getDeathMessage() is written
     * from memory of long-stable vanilla API (used internally for vanilla's
     * own death messages), not confirmed against source pasted in this
     * conversation. Ctrl/cmd-click getCombatTracker() to check if it errors.
     */
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
