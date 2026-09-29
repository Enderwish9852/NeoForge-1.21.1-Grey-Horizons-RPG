package net.enderwish.Belliarium_Monstrarium_Subpack.core;

import net.minecraft.world.entity.Mob;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * CombatState
 *
 * NEW -- isTagged(UUID) exposes membership in taggedMobs, needed by
 * CombatTimerHandler's escape-bug fix. Everything else unchanged.
 */
public class CombatState {

    public enum EndReason { SUCCESS, DEFEAT, ESCAPED }

    private boolean inCombat = false;
    private long combatStartTick = -1;
    private int currentGroup = 0;
    private final Map<UUID, Integer> taggedMobs = new HashMap<>();
    private final List<CombatLogEntry> log = new ArrayList<>();
    private EndReason lastEndReason = null;

    public boolean isInCombat() { return inCombat; }
    public long getCombatStartTick() { return combatStartTick; }
    public List<CombatLogEntry> getLog() { return log; }
    public EndReason getLastEndReason() { return lastEndReason; }

    public void startCombat(long gameTime) {
        inCombat = true;
        combatStartTick = gameTime;
        currentGroup = 0;
        taggedMobs.clear();
        log.clear();
        lastEndReason = null;
    }

    public boolean isTagged(UUID mobId) {
        return taggedMobs.containsKey(mobId);
    }

    /** Returns how many of the given mobs were NOT already tagged this battle. */
    public int tagMobs(List<Mob> mobs, long gameTime) {
        currentGroup++;
        int added = 0;
        for (Mob mob : mobs) {
            if (taggedMobs.putIfAbsent(mob.getUUID(), currentGroup) == null) {
                added++;
            }
        }
        return added;
    }

    /** Returns true if the given UUID was actually tagged (and removes it). */
    public boolean untagMob(UUID mobId) {
        return taggedMobs.remove(mobId) != null;
    }

    public boolean hasNoTaggedMobsLeft() {
        return taggedMobs.isEmpty();
    }

    public void logDamage(CombatLogEntry entry) {
        if (inCombat) log.add(entry);
    }

    public void endCombat(EndReason reason) {
        inCombat = false;
        lastEndReason = reason;
    }
}
