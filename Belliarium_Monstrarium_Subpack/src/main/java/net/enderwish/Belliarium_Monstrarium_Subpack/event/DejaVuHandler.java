package net.enderwish.Belliarium_Monstrarium_Subpack.event;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.diary.DayTracker;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.diary.DayTrackerData;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.diary.DejaVuMemory;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.status.StatusManager;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.status.StatusType;
import net.enderwish.Belliarium_Monstrarium_Subpack.network.DejaVuClearPacket;
import net.enderwish.Belliarium_Monstrarium_Subpack.network.DejaVuFadePacket;
import net.enderwish.Belliarium_Monstrarium_Subpack.network.DejaVuTeleportPacket;
import net.enderwish.Belliarium_Monstrarium_Subpack.network.ModMessages;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DejaVuHandler
 *
 * Two jobs:
 *  1. While a day is being tracked, tags every container the owner interacts
 *     with as "looted" (DayTrackerData.markLooted) -- containers never
 *     opened that day stay untouched after a rewind.
 *  2. After a wake-up, while the player carries Deja Vu, opening one of
 *     those tagged positions again is intercepted: the real loot is denied
 *     (container emptied for real), a client-side fade cue plays every time
 *     (same intensity, no "first time only"), and there's a flat 5% chance
 *     of the short-range "bad dream" teleport.
 */
@EventBusSubscriber(modid = BelliariumMonstrariumSubpack.MODID)
public final class DejaVuHandler {
    private DejaVuHandler() {}

    private static final double TELEPORT_CHANCE = 0.05;
    private static final int TELEPORT_CHUNK_RADIUS = 1; // current chunk + 8 neighbors

    private static final Set<UUID> hadDejaVu = ConcurrentHashMap.newKeySet();

    /** Player -> remembered container position whose block should be deleted once they close its GUI. */
    private static final java.util.Map<UUID, Long> pendingGhostDestroy = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ServerLevel level = (ServerLevel) event.getLevel();
        BlockPos pos = event.getPos();
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof Container container)) return;
        long posKey = pos.asLong();

        boolean hasDejaVu = StatusManager.INSTANCE.has(player.getUUID(), StatusType.DEJA_VU);
        boolean remembered = hasDejaVu && DejaVuMemory.INSTANCE.isRemembered(player.getUUID(), posKey);

        DayTrackerData tracking = DayTracker.getActiveData();
        if (!remembered && DayTracker.isTracking() && tracking != null && player.getUUID().equals(tracking.getOwner())) {
            tracking.markLooted(posKey);
            tracking.setDirty();
            return; // mid-day: no interception, this is the real loot run
        }

        if (!remembered) return;

        // Let the vanilla container GUI open normally -- do NOT cancel. We just
        // empty it for real and arrange for the whole block to vanish once the
        // player closes that screen (see onContainerClose below).
        intercept(level, player, pos, container, be);
    }

    private static void intercept(ServerLevel level, ServerPlayer player, BlockPos pos, Container container, BlockEntity be) {
        List<Integer> slots = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                slots.add(i);
                ids.add(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
                counts.add(stack.getCount());
            }
        }
        container.clearContent();
        be.setChanged();

        pendingGhostDestroy.put(player.getUUID(), pos.asLong());

        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 1.0f, 0.5f);
        ModMessages.sendToPlayer(new DejaVuFadePacket(slots, ids, counts), player);

        if (player.getRandom().nextDouble() < TELEPORT_CHANCE) {
            teleportNearby(level, player);
            ModMessages.sendToPlayer(new DejaVuTeleportPacket(), player);
        }
        // Mental trauma hook -- intentionally a no-op stub until that system exists.
        // DejaVuTrauma.onTrigger(player);
    }

    @SubscribeEvent
    public static void onContainerClose(PlayerContainerEvent.Close event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Long posKey = pendingGhostDestroy.remove(player.getUUID());
        if (posKey == null) return;

        BlockPos pos = BlockPos.of(posKey);
        ServerLevel level = player.serverLevel();
        level.removeBlock(pos, false);
        DejaVuMemory.INSTANCE.forget(player.getUUID(), posKey);
        ModMessages.sendToPlayer(new DejaVuClearPacket(), player);
    }

    private static void teleportNearby(ServerLevel level, ServerPlayer player) {
        BlockPos origin = player.blockPosition();
        int chunkX = origin.getX() >> 4;
        int chunkZ = origin.getZ() >> 4;

        for (int attempt = 0; attempt < 20; attempt++) {
            int dx = player.getRandom().nextInt(16 * (2 * TELEPORT_CHUNK_RADIUS + 1)) - 16 * TELEPORT_CHUNK_RADIUS;
            int dz = player.getRandom().nextInt(16 * (2 * TELEPORT_CHUNK_RADIUS + 1)) - 16 * TELEPORT_CHUNK_RADIUS;
            int x = (chunkX << 4) + 8 + dx;
            int z = (chunkZ << 4) + 8 + dz;
            BlockPos safe = findSafeColumn(level, x, z);
            if (safe != null) {
                player.teleportTo(safe.getX() + 0.5, safe.getY(), safe.getZ() + 0.5);
                return;
            }
        }
    }

    /** Solid (or water) ground, 2 air blocks above, nothing damaging directly underneath. */
    private static BlockPos findSafeColumn(ServerLevel level, int x, int z) {
        int top = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z);
        for (int y = top; y > level.getMinBuildHeight() + 1; y--) {
            BlockPos ground = new BlockPos(x, y - 1, z);
            BlockPos feet = new BlockPos(x, y, z);
            BlockPos head = new BlockPos(x, y + 1, z);
            BlockState groundState = level.getBlockState(ground);
            if (groundState.isAir()) continue;
            if (groundState.getBlock() == net.minecraft.world.level.block.Blocks.LAVA
                    || groundState.getBlock() == net.minecraft.world.level.block.Blocks.MAGMA_BLOCK
                    || groundState.getBlock() == net.minecraft.world.level.block.Blocks.CACTUS
                    || groundState.getBlock() == net.minecraft.world.level.block.Blocks.CAMPFIRE) continue;
            boolean groundOk = groundState.isFaceSturdy(level, ground, net.minecraft.core.Direction.UP)
                    || groundState.getFluidState().is(net.minecraft.tags.FluidTags.WATER);
            if (!groundOk) continue;
            if (!level.getBlockState(feet).isAir() && !level.getBlockState(feet).getFluidState().is(net.minecraft.tags.FluidTags.WATER)) continue;
            if (!level.getBlockState(head).isAir()) continue;
            return feet;
        }
        return null;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        UUID id = player.getUUID();
        StatusManager.INSTANCE.tick(id, player.level().getGameTime());

        boolean has = StatusManager.INSTANCE.has(id, StatusType.DEJA_VU);
        if (has) {
            hadDejaVu.add(id);
        } else if (hadDejaVu.remove(id)) {
            DejaVuMemory.INSTANCE.clear(id); // status just expired: forget remembered containers
        }
    }
}
