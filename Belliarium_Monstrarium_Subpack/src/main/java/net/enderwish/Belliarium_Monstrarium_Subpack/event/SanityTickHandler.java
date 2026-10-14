package net.enderwish.Belliarium_Monstrarium_Subpack.event;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumConfig;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.ModAttachments;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.status.StatusManager;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.status.StatusType;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.survival.SanityCapability;
import net.enderwish.Belliarium_Monstrarium_Subpack.network.ModMessages;
import net.enderwish.Belliarium_Monstrarium_Subpack.network.SanitySyncPacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * SanityTickHandler
 *
 * NEW -- the sanity system's server brain.
 *
 * - On FIRST JOIN: grants HIVE_MIND_INFLUENCE permanently (it never
 *   expires and can't be removed -- the brainworms are forever).
 * - Every tick: drains sanity while any sanity-draining status is held.
 *   Rate = BelliariumConfig drainPerMinute (default 1.0/min, i.e. the
 *   player has ~1h40m of continuous play from 100 to death).
 * - Sanity NEVER self-regenerates -- only sources move it.
 * - Milestones at 50 / 25 / 1 fire onMilestone() -- intentionally empty
 *   TODO hooks (whispers, hallucinations, screen effects) until the
 *   trauma/insanity-visuals system is designed.
 * - At 0: killByInsanity() -- a REAL death that bypasses the Survivor's
 *   Diary rewind (the book can rewind a body, not a lost mind).
 * - On respawn after insanity death: sanity resets to 100 (the influence
 *   status itself is permanent, so the slow drain starts again).
 */
@EventBusSubscriber(modid = BelliariumMonstrariumSubpack.MODID)
public class SanityTickHandler {

    private static final float[] MILESTONES = {50f, 25f, 1f};

    /** Last synced value per player, so we only send packets on change. */
    private static final Map<UUID, Float> LAST_SYNCED = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // Everyone carries the Hive Mind from the moment they first enter
        // the world, forever. apply() is idempotent.
        StatusManager.INSTANCE.apply(player.getUUID(), StatusType.HIVE_MIND_INFLUENCE,
                player.level().getGameTime(), -1L);

        sync(player);
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer newPlayer)) return;
        if (!event.isWasDeath()) return; // end-portal travel keeps sanity

        // Death wipes the mind clean: fresh 100, drain starts over.
        newPlayer.setData(ModAttachments.SANITY, new SanityCapability());
        sync(newPlayer);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        // NOTE: creative players are NOT exempt — sanity is a mental stat
        // and the Hive Mind drains it in every game mode (also makes the
        // system testable without leaving creative). Only the dead are
        // spared (their sanity resets on respawn).
        if (player.isDeadOrDying()) return;
        if (player.level().getGameTime() % 20L != 0L) return; // once per second

        SanityCapability sanity = player.getData(ModAttachments.SANITY);
        float before = sanity.get();

        // ── Status-driven drain (Hive Mind Influence) ────────────────────
        if (StatusManager.INSTANCE.has(player.getUUID(), StatusType.HIVE_MIND_INFLUENCE)) {
            double perMinute = BelliariumConfig.SANITY_DRAIN_PER_MINUTE.get();
            sanity.add((float) (-perMinute / 60.0)); // per second
        }

        // ── Milestones (crossing detection, effects TODO) ────────────────
        for (float m : MILESTONES) {
            if (before > m && sanity.get() <= m) onMilestone(player, m);
        }

        // ── Death at zero ─────────────────────────────────────────────────
        if (sanity.get() <= 0f) {
            LAST_SYNCED.remove(player.getUUID());
            BodyDamageHandler.killByInsanity(player);
            return;
        }

        if (sanity.get() != before) sync(player);
    }

    /** TODO: wire the 50/25/1 sanity milestone effects (whispers, hallucinations, vignette) here. */
    private static void onMilestone(ServerPlayer player, float milestone) {
        // intentionally empty -- effects land with the trauma system
    }

    private static void sync(ServerPlayer player) {
        SanityCapability cap = player.getData(ModAttachments.SANITY);
        LAST_SYNCED.put(player.getUUID(), cap.get());
        ModMessages.sendToPlayer(new SanitySyncPacket(cap.get()), player);
    }
}
