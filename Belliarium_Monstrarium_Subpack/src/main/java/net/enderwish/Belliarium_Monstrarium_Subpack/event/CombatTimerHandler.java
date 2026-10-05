package net.enderwish.Belliarium_Monstrarium_Subpack.event;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.combat.CombatState;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.combat.CombatTimerManager;
import net.enderwish.Belliarium_Monstrarium_Subpack.network.CombatTimerSyncPacket;
import net.enderwish.Belliarium_Monstrarium_Subpack.network.ModMessages;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@EventBusSubscriber(modid = BelliariumMonstrariumSubpack.MODID)
public class CombatTimerHandler {

    private static final int SCAN_INTERVAL_TICKS = 20;
    private static final int SCAN_CHUNK_RADIUS = 1;

    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        tickCounter++;
        if (tickCounter % SCAN_INTERVAL_TICKS != 0) return;

        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) continue;
            scanPlayer(level, player);
        }
    }

    private static void scanPlayer(ServerLevel level, ServerPlayer player) {
        CombatState state = CombatTimerManager.INSTANCE.getOrCreate(player.getUUID());

        ChunkPos center = player.chunkPosition();
        int minX = (center.x - SCAN_CHUNK_RADIUS) << 4;
        int minZ = (center.z - SCAN_CHUNK_RADIUS) << 4;
        int maxX = (center.x + SCAN_CHUNK_RADIUS + 1) << 4;
        int maxZ = (center.z + SCAN_CHUNK_RADIUS + 1) << 4;
        AABB scanBox = new AABB(minX, level.getMinBuildHeight(), minZ,
                maxX, level.getMaxBuildHeight(), maxZ);

        List<Mob> targetingPlayer = level.getEntitiesOfClass(Mob.class, scanBox,
                mob -> mob.isAlive() && mob.getTarget() == player);

        List<Mob> newlyTaggable = new ArrayList<>();
        for (Mob mob : targetingPlayer) {
            if (!state.isTagged(mob.getUUID()) && mob.getSensing().hasLineOfSight(player)) {
                newlyTaggable.add(mob);
            }
        }

        if (state.isInCombat()) {
            if (!targetingPlayer.isEmpty()) {
                if (!newlyTaggable.isEmpty()) {
                    state.tagMobs(newlyTaggable, level.getGameTime());
                }
                sync(player, state, null);
            } else {
                state.endCombat(CombatState.EndReason.ESCAPED);
                sync(player, state, "Battle end: Escaped");
            }
        } else if (!newlyTaggable.isEmpty()) {
            state.startCombat(level.getGameTime());
            state.tagMobs(newlyTaggable, level.getGameTime());
            sync(player, state, null);
        }
    }

    @SubscribeEvent
    public static void onMobDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer) return;
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;

        UUID deadMobId = event.getEntity().getUUID();
        for (ServerPlayer player : level.players()) {
            CombatState state = CombatTimerManager.INSTANCE.getOrCreate(player.getUUID());
            if (!state.isInCombat()) continue;
            if (state.untagMob(deadMobId) && state.hasNoTaggedMobsLeft()) {
                state.endCombat(CombatState.EndReason.SUCCESS);
                sync(player, state, "Battle end: Success");
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        CombatState state = CombatTimerManager.INSTANCE.getOrCreate(player.getUUID());
        if (!state.isInCombat()) return;
        state.endCombat(CombatState.EndReason.DEFEAT);
        sync(player, state, "Battle end: Defeat");
    }

    private static void sync(ServerPlayer player, CombatState state, String endMessage) {
        int elapsedSeconds = state.isInCombat()
                ? (int) ((player.level().getGameTime() - state.getCombatStartTick()) / 20L)
                : 0;
        ModMessages.sendToPlayer(
                new CombatTimerSyncPacket(state.isInCombat(), elapsedSeconds, endMessage, state.getBattleId()), player);
    }
}
