package net.enderwish.Belliarium_Monstrarium_Subpack.core.status;

import net.enderwish.Belliarium_Monstrarium_Subpack.core.ModAttachments;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.body.BodyHealthCapability;
import net.minecraft.world.entity.player.Player;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * StatusManager
 *
 * Singleton, same shape as AdrenalineManager/PainkillerManager: a UUID-keyed
 * map, here storing only the CONDITIONAL (durational) statuses -- currently
 * just DEJA_VU. Permanent body-part statuses are never stored here; they're
 * derived live from BodyHealthCapability by {@link #activeBodyStatuses}, so
 * they can't go stale and can't be cleared by command, matching the spec
 * ("a status cannot be removed, only by doing what it tells u").
 */
public final class StatusManager {
    public static final StatusManager INSTANCE = new StatusManager();
    private StatusManager() {}

    private final Map<UUID, Map<StatusType, Long>> conditional = new ConcurrentHashMap<>();

    /** Applies a conditional status until {@code currentGameTime + durationTicks}. Permanent types are rejected. */
    public void apply(UUID playerId, StatusType type, long currentGameTime, long durationTicks) {
        if (!type.isConditional()) return;
        conditional.computeIfAbsent(playerId, k -> new EnumMap<>(StatusType.class))
                .put(type, currentGameTime + durationTicks);
    }

    public boolean has(UUID playerId, StatusType type) {
        if (!type.isConditional()) return false;
        Map<StatusType, Long> map = conditional.get(playerId);
        return map != null && map.containsKey(type);
    }

    /** Expires anything whose time is up. Called once per player per tick. */
    public void tick(UUID playerId, long currentGameTime) {
        Map<StatusType, Long> map = conditional.get(playerId);
        if (map == null || map.isEmpty()) return;
        map.entrySet().removeIf(e -> currentGameTime >= e.getValue());
    }

    /** /status clear -- drops every conditional status this player currently has. */
    public void clearConditional(UUID playerId) {
        conditional.remove(playerId);
    }

    /** /status remove "name" -- only removes a conditional (removable) status; returns false for a permanent one or a miss. */
    public boolean remove(UUID playerId, StatusType type) {
        if (!type.isConditional()) return false;
        Map<StatusType, Long> map = conditional.get(playerId);
        return map != null && map.remove(type) != null;
    }

    public Set<StatusType> activeConditional(UUID playerId) {
        Map<StatusType, Long> map = conditional.get(playerId);
        return map == null ? EnumSet.noneOf(StatusType.class) : EnumSet.copyOf(map.keySet());
    }

    /** The permanent body-part statuses currently true for this player, read live off BodyHealthCapability. */
    public Set<StatusType> activeBodyStatuses(Player player) {
        BodyHealthCapability cap = player.getData(ModAttachments.BODY_HEALTH);
        Set<StatusType> set = EnumSet.noneOf(StatusType.class);
        if (cap.isLeftArmDisabled()) set.add(StatusType.LEFT_HAND_BROKEN); else if (cap.isLeftArmRed()) set.add(StatusType.LEFT_HAND_INJURED);
        if (cap.isRightArmDisabled()) set.add(StatusType.RIGHT_HAND_BROKEN); else if (cap.isRightArmRed()) set.add(StatusType.RIGHT_HAND_INJURED);
        if (cap.isLeftLegDisabled()) set.add(StatusType.LEFT_LEG_BROKEN); else if (cap.isLeftLegRed()) set.add(StatusType.LEFT_LEG_INJURED);
        if (cap.isRightLegDisabled()) set.add(StatusType.RIGHT_LEG_BROKEN); else if (cap.isRightLegRed()) set.add(StatusType.RIGHT_LEG_INJURED);
        return set;
    }
}
