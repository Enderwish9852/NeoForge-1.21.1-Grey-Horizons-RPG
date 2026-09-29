package net.enderwish.Belliarium_Monstrarium_Subpack.core;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CombatTimerManager
 *
 * Singleton, in-memory only -- same "static INSTANCE registry" shape as
 * WeightRegistry elsewhere in this project, minus the JSON-reload-listener
 * part since there's no data file backing this one.
 */
public class CombatTimerManager {

    public static final CombatTimerManager INSTANCE = new CombatTimerManager();
    private CombatTimerManager() {}

    private final Map<UUID, CombatState> states = new ConcurrentHashMap<>();

    public CombatState getOrCreate(UUID playerId) {
        return states.computeIfAbsent(playerId, id -> new CombatState());
    }

    public boolean isInCombat(UUID playerId) {
        CombatState state = states.get(playerId);
        return state != null && state.isInCombat();
    }
}
