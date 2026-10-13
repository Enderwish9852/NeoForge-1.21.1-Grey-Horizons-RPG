package net.enderwish.Belliarium_Monstrarium_Subpack.core.diary;

import net.enderwish.Belliarium_Monstrarium_Subpack.network.DeathReportPacket;
import net.enderwish.Belliarium_Monstrarium_Subpack.network.ModMessages;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DiaryLimbo
 *
 * The state between "would have died" and "woke up". The player stays alive
 * (BodyDamageHandler's pinned vanilla health and cancelled deaths already guarantee
 * that), takes no further body damage, and is held still while the report screen plays.
 * In-memory only: a relog re-enters limbo (see DiaryLimboHandler).
 */
public final class DiaryLimbo {
    private DiaryLimbo() {}

    private static final Set<UUID> IN_LIMBO = ConcurrentHashMap.newKeySet();

    public static boolean isInLimbo(UUID id) { return id != null && IN_LIMBO.contains(id); }

    /** A rewind is possible: tracking is active for this player, a diary is carried, and nothing is mid-rewind. */
    public static boolean canRewind(ServerPlayer player) {
        DayTrackerData data = DayTracker.getActiveData();
        return data != null
                && player.getUUID().equals(data.getOwner())
                && !DayTracker.isWakingUp()
                && !DiaryBook.find(player).isEmpty();
    }

    public static void enter(ServerPlayer player, DeathReportPacket packet) {
        IN_LIMBO.add(player.getUUID());
        ModMessages.sendToPlayer(packet, player);
    }

    public static void leave(UUID id) { IN_LIMBO.remove(id); }

    public static void tickFreeze(ServerPlayer player) {
        if (!IN_LIMBO.contains(player.getUUID())) return;
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 9, false, false));
        player.setSprinting(false);
    }
}
