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
 * BUGFIX (log still lingering after battle ends/death) -- battleId was only
 * bumped in startCombat(), so the client (which clears whenever it sees a
 * NEW battleId) only ever cleared when a fresh fight began -- never on any
 * end reason, including death. Now bumped on both start AND end, so ending
 * a battle for any reason is itself a "this id changed" signal too.
 */
public class CombatState {

    public enum EndReason { SUCCESS, DEFEAT, ESCAPED }

    private boolean inCombat = false;
    private long combatStartTick = -1;
    private int currentGroup = 0;
    private long battleId = 0;
    private final Map<UUID, Integer> taggedMobs = new HashMap<>();
    private final List<CombatLogEntry> log = new ArrayList<>();
    private EndReason lastEndReason = null;

    public boolean isInCombat() { return inCombat; }
    public long getCombatStartTick() { return combatStartTick; }
    public long getBattleId() { return battleId; }
    public List<CombatLogEntry> getLog() { return log; }
    public EndReason getLastEndReason() { return lastEndReason; }

    public void startCombat(long gameTime) {
        inCombat = true;
        combatStartTick = gameTime;
        currentGroup = 0;
        taggedMobs.clear();
        log.clear();
        lastEndReason = null;
        battleId++;
    }

    public boolean isTagged(UUID mobId) {
        return taggedMobs.containsKey(mobId);
    }

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
        taggedMobs.clear();
        battleId++; // THE FIX -- ending a battle now also counts as "this id changed"
    }
}
