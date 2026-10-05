package net.enderwish.Belliarium_Monstrarium_Subpack.core;

import net.minecraft.world.InteractionHand;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SkillLearningManager
 *
 * In-memory only. Tracks, per player, which hand holds an actively-being-
 * learned book and when the last heartbeat arrived -- see
 * SkillLearningHandler for why a heartbeat instead of a single open/close
 * pair.
 */
public class SkillLearningManager {

    public static final SkillLearningManager INSTANCE = new SkillLearningManager();
    private SkillLearningManager() {}

    public record ActiveLearning(InteractionHand hand, String skillId, long lastHeartbeatTick) {}

    private final Map<UUID, ActiveLearning> active = new ConcurrentHashMap<>();

    public void heartbeat(UUID playerId, InteractionHand hand, String skillId, long gameTime) {
        active.put(playerId, new ActiveLearning(hand, skillId, gameTime));
    }

    public void clear(UUID playerId) { active.remove(playerId); }
    public ActiveLearning get(UUID playerId) { return active.get(playerId); }
}
