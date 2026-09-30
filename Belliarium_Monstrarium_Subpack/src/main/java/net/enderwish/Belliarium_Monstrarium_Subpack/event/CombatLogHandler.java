package net.enderwish.Belliarium_Monstrarium_Subpack.event;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.CombatLogManager;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.CombatLogRowType;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.CombatState;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.CombatTimerManager;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.PlayerCombatLogState;
import net.enderwish.Belliarium_Monstrarium_Subpack.network.ExpLogPacket;
import net.enderwish.Belliarium_Monstrarium_Subpack.network.KillFeedPacket;
import net.enderwish.Belliarium_Monstrarium_Subpack.network.ModMessages;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * CombatLogHandler
 *
 * Drives the new Combat Log HUD (kill feed + exp log). ASSUMPTION -- flagged
 * as an open question in chat, not silently guessed: this is a purely
 * session-scoped VISUAL log for now, same lifetime as CombatState/
 * PlayerCombatLogState -- no persisted running xp total exists yet. If a
 * real persisted stat is wanted later, none of this xp math needs to
 * change, only where the numbers ultimately get written to.
 *
 * Melee vs ranged: DamageSource#getEntity() already resolves to the "true"
 * attacker for both cases in vanilla's model (the shooter for an arrow, not
 * the arrow itself) -- getDirectEntity() is what tells melee and ranged
 * apart: melee has direct == the player themselves, ranged has some
 * projectile as the direct entity instead.
 *
 * COMBO_WINDOW_TICKS and DAMAGE_XP_RATIO are first-pass placeholder numbers,
 * same status as every other tuning constant in this project.
 */
@EventBusSubscriber(modid = BelliariumMonstrariumSubpack.MODID)
public class CombatLogHandler {

    private static final int COMBO_WINDOW_TICKS = 30; // 1.5s between melee hits to keep a combo alive
    private static final float DAMAGE_XP_RATIO = 1.0f; // 1 damage point = 1 xp, placeholder
    private static final float ENTER_BATTLE_XP = 10f;
    private static final float COMBAT_TIME_MILESTONE_SECONDS = 10f;
    private static final float COMBAT_TIME_XP_PER_MILESTONE = 5f;

    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        tickCounter++;
        if (tickCounter % 20 != 0) return; // once per second

        for (ServerPlayer player : level.players()) {
            checkBattleTransitions(player, level);
            checkComboTimeout(player, level);
        }
    }

    private static void checkBattleTransitions(ServerPlayer player, ServerLevel level) {
        CombatState combat = CombatTimerManager.INSTANCE.getOrCreate(player.getUUID());
        PlayerCombatLogState logState = CombatLogManager.INSTANCE.getOrCreate(player.getUUID());

        boolean nowInCombat = combat.isInCombat();
        if (nowInCombat && !logState.wasInCombat()) {
            logState.resetForNewBattle();
            ModMessages.sendToPlayer(ExpLogPacket.show(CombatLogRowType.ENTER_BATTLE,
                    "Entering Battle", ENTER_BATTLE_XP, true), player);
        }
        logState.setWasInCombat(nowInCombat);

        if (!nowInCombat) return;

        int secondsIntoBattle = (int) ((level.getGameTime() - combat.getCombatStartTick()) / 20L);
        if (secondsIntoBattle >= logState.getLastCombatTimeMilestoneSecond() + COMBAT_TIME_MILESTONE_SECONDS) {
            logState.setLastCombatTimeMilestoneSecond(
                    (int) (Math.floor(secondsIntoBattle / COMBAT_TIME_MILESTONE_SECONDS) * COMBAT_TIME_MILESTONE_SECONDS));
            logState.addCombatTimeXp(COMBAT_TIME_XP_PER_MILESTONE);
            ModMessages.sendToPlayer(ExpLogPacket.show(CombatLogRowType.COMBAT_TIME,
                    "Combat Time", logState.getCumulativeCombatTimeXp(), false), player);
        }
    }

    private static void checkComboTimeout(ServerPlayer player, ServerLevel level) {
        PlayerCombatLogState logState = CombatLogManager.INSTANCE.getOrCreate(player.getUUID());
        if (logState.getComboCount() < 2) return;
        if (level.getGameTime() - logState.getLastMeleeHitTick() > COMBO_WINDOW_TICKS) {
            breakCombo(player, logState);
        }
    }

    private static void breakCombo(ServerPlayer player, PlayerCombatLogState logState) {
        logState.breakCombo();
        logState.resetDamageXp();
        ModMessages.sendToPlayer(ExpLogPacket.fadeOut(CombatLogRowType.COMBO), player);
        ModMessages.sendToPlayer(ExpLogPacket.restart(CombatLogRowType.DAMAGE, logState.getLastAttackTypeLabel()), player);
    }

    /** Kill feed -- ANY mob a player kills, including passive mobs, per your spec. */
    @SubscribeEvent
    public static void onMobDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer) return;
        if (!(event.getEntity() instanceof LivingEntity victim)) return;

        ServerPlayer killer = resolveAttacker(event.getSource());
        if (killer == null) return;

        String weaponId = killer.getMainHandItem().isEmpty()
                ? "minecraft:air"
                : net.minecraft.core.registries.BuiltInRegistries.ITEM
                .getKey(killer.getMainHandItem().getItem()).toString();

        ModMessages.sendToPlayer(new KillFeedPacket(
                killer.getName().getString(), weaponId, victim.getName().getString()), killer);
    }

    /** Outgoing damage tracking -- feeds the Damage xp row and melee combo. */
    @SubscribeEvent
    public static void onOutgoingDamage(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer) return; // that's incoming, handled in BodyDamageHandler

        DamageSource source = event.getSource();
        ServerPlayer attacker = resolveAttacker(source);
        if (attacker == null) return;

        boolean melee = source.getDirectEntity() == attacker;
        String label = melee ? "Hit" : "Range";

        PlayerCombatLogState logState = CombatLogManager.INSTANCE.getOrCreate(attacker.getUUID());
        logState.setLastAttackTypeLabel(label);
        logState.addDamageXp(event.getNewDamage() * DAMAGE_XP_RATIO);

        ModMessages.sendToPlayer(ExpLogPacket.show(CombatLogRowType.DAMAGE,
                "(" + label + ") Damage", logState.getCumulativeDamageXp(), false), attacker);

        if (melee) {
            long gameTime = attacker.level().getGameTime();
            logState.registerMeleeHit(gameTime, COMBO_WINDOW_TICKS);
            int combo = logState.getComboCount();
            if (combo >= 2) {
                float comboXp = (float) Math.round(Math.sqrt(combo));
                ModMessages.sendToPlayer(ExpLogPacket.show(CombatLogRowType.COMBO,
                        "Combo x" + combo, comboXp, false), attacker);
            }
        }
    }

    private static ServerPlayer resolveAttacker(DamageSource source) {
        return source.getEntity() instanceof ServerPlayer player ? player : null;
    }
}
