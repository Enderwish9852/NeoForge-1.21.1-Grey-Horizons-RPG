package net.enderwish.Belliarium_Monstrarium_Subpack.core;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Singleton registry, same shape as CombatTimerManager/WeightRegistry. */
public class CombatLogManager {

    public static final CombatLogManager INSTANCE = new CombatLogManager();
    private CombatLogManager() {}

    private final Map<UUID, PlayerCombatLogState> states = new ConcurrentHashMap<>();

    public PlayerCombatLogState getOrCreate(UUID playerId) {
        return states.computeIfAbsent(playerId, id -> new PlayerCombatLogState());
    }
}