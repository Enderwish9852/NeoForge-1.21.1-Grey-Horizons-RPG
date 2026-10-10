package net.enderwish.Belliarium_Monstrarium_Subpack.core.medical;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PainkillerManager {
    public static final PainkillerManager INSTANCE = new PainkillerManager();
    private PainkillerManager() {}

    private final Map<UUID, Long> activeUntilGameTime = new ConcurrentHashMap<>();

    /** Keeps whichever expiry is later, so a short effect can never cut a long one short. */
    public void activate(UUID playerId, long untilGameTime) {
        activeUntilGameTime.merge(playerId, untilGameTime, Math::max);
    }

    public boolean isActive(UUID playerId) {
        return activeUntilGameTime.containsKey(playerId);
    }

    /** Called every server tick per player by MedicalTickHandler. */
    public void tick(UUID playerId, long currentGameTime) {
        Long until = activeUntilGameTime.get(playerId);
        if (until != null && currentGameTime >= until) {
            activeUntilGameTime.remove(playerId);
        }
    }

    public void clear(UUID playerId) {
        activeUntilGameTime.remove(playerId);
    }
}
