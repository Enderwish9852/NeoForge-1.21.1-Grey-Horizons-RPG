package net.enderwish.Belliarium_Monstrarium_Subpack.core.medical;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PainkillerManager {
    public static final PainkillerManager INSTANCE = new PainkillerManager();
    private PainkillerManager() {}

    private final Map<UUID, Long> activeUntilGameTime = new ConcurrentHashMap<>();

    public void activate(UUID playerId, long untilGameTime) {
        activeUntilGameTime.put(playerId, untilGameTime);
    }

    public boolean isActive(UUID playerId) {
        Long until = activeUntilGameTime.get(playerId);
        return until != null && until > 0; // tick() below clears expired entries to 0/removed
    }

    public void tick(UUID playerId, long currentGameTime) {
        Long until = activeUntilGameTime.get(playerId);
        if (until != null && currentGameTime >= until) {
            activeUntilGameTime.remove(playerId);
        }
    }
}
