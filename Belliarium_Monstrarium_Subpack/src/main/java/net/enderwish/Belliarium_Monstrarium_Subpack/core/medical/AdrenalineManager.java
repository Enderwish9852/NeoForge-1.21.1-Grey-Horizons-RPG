package net.enderwish.Belliarium_Monstrarium_Subpack.core.medical;

import net.minecraft.world.damagesource.DamageSource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AdrenalineManager
 *
 * Two SEPARATE lifespans tracked here, deliberately not merged: "active"
 * (the no-damage/no-debuff window itself) clears the instant it ends, but
 * "stamina locked" is a longer AFTERMATH that outlives it -- "stamina will
 * not replenish" after the crash, per your spec, not during the effect.
 */
public class AdrenalineManager {
    public static final AdrenalineManager INSTANCE = new AdrenalineManager();
    private AdrenalineManager() {}

    public static final class PendingDamage {
        public float amount;
        public DamageSource lastSource;
    }

    public static final class ActiveAdrenaline {
        public long endTick;
        public final Map<String, PendingDamage> pendingByPart = new HashMap<>();
    }

    private final Map<UUID, ActiveAdrenaline> active = new ConcurrentHashMap<>();
    private final Map<UUID, Long> staminaLockedUntil = new ConcurrentHashMap<>();

    public void activate(UUID playerId, long endTick) {
        ActiveAdrenaline a = new ActiveAdrenaline();
        a.endTick = endTick;
        active.put(playerId, a);
    }

    public boolean isActive(UUID playerId) { return active.containsKey(playerId); }
    public ActiveAdrenaline get(UUID playerId) { return active.get(playerId); }
    public void clear(UUID playerId) { active.remove(playerId); }

    public void logDamage(UUID playerId, String bodyPart, float amount, DamageSource source) {
        ActiveAdrenaline a = active.get(playerId);
        if (a == null) return;
        PendingDamage pd = a.pendingByPart.computeIfAbsent(bodyPart, k -> new PendingDamage());
        pd.amount += amount;
        pd.lastSource = source;
    }

    public void lockStaminaUntil(UUID playerId, long gameTime) {
        staminaLockedUntil.put(playerId, gameTime);
    }

    public boolean isStaminaLocked(UUID playerId, long currentGameTime) {
        Long until = staminaLockedUntil.get(playerId);
        return until != null && currentGameTime < until;
    }
}
