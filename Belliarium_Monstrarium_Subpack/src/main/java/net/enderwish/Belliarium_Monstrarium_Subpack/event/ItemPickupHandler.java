package net.enderwish.Belliarium_Monstrarium_Subpack.event;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.Comparator;

/**
 * ItemPickupHandler
 *
 * Replaces vanilla's walk-over auto-suck pickup with a deliberate
 * right-click-to-collect interaction.
 *
 * onAutoPickup unconditionally blocks vanilla's magnetic pickup.
 *
 * onRightClickItem handles the case where the player actually targets the
 * ItemEntity directly (works when the crosshair lands on the tiny hitbox).
 *
 * onRightClickBlock is the fallback: item entity hitboxes are TINY
 * (~0.25 blocks), so when looking down at items the raytrace almost always
 * hits the BLOCK below instead of the entity. This handler scans for the
 * nearest ItemEntity within reach in front of the player and picks it up.
 */
@EventBusSubscriber(modid = BelliariumMonstrariumSubpack.MODID)
public class ItemPickupHandler {

    private static final double PICKUP_REACH = 4.0;

    @SubscribeEvent
    public static void onAutoPickup(ItemEntityPickupEvent.Pre event) {
        event.setCanPickup(TriState.FALSE);
    }

    /**
     * Direct hit on the item entity.
     */
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide()) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getTarget() instanceof ItemEntity itemEntity)) return;

        if (tryPickup(player, itemEntity)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    /**
     * Fallback: clicked the block, but an item is in front of the player.
     * Finds the nearest ItemEntity along the look ray and picks it up.
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide()) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;

        ItemEntity target = findLookedAtItem(player);
        if (target == null) return;

        if (tryPickup(player, target)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Finds the ItemEntity the player is looking at, within PICKUP_REACH.
     * Uses a manual ray-vs-box test so it doesn't get fooled by the tiny
     * vanilla hitbox the way vanilla's own raytrace does.
     */
    private static ItemEntity findLookedAtItem(ServerPlayer player) {
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = eye.add(look.scale(PICKUP_REACH));

        // Don't reach through solid blocks.
        HitResult blockHit = player.level().clip(new ClipContext(
                eye, end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        ));
        if (blockHit.getType() != HitResult.Type.MISS) {
            end = blockHit.getLocation();
        }

        // Grab every ItemEntity whose box the ray segment passes near,
        // then take the closest one to the eye.
        AABB searchBox = player.getBoundingBox()
                .expandTowards(look.scale(PICKUP_REACH))
                .inflate(1.0);

        Vec3 finalEnd = end;
        return player.level()
                .getEntities(player, searchBox, e -> e instanceof ItemEntity)
                .stream()
                .filter(e -> e.getBoundingBox().inflate(0.3).clip(eye, finalEnd).isPresent())
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(eye)))
                .map(e -> (ItemEntity) e)
                .orElse(null);
    }

    /**
     * Moves as much of the item's stack as fits into the player's inventory.
     * Discards the entity if fully collected, otherwise updates the stack.
     *
     * @return true if anything was picked up.
     */
    private static boolean tryPickup(ServerPlayer player, ItemEntity itemEntity) {
        ItemStack stack = itemEntity.getItem().copy();
        if (stack.isEmpty()) {
            itemEntity.discard();
            return false;
        }

        int before = stack.getCount();
        player.getInventory().add(stack);
        int pickedUp = before - stack.getCount();

        if (pickedUp <= 0) {
            return false; // inventory full, nothing moved
        }

        if (stack.isEmpty()) {
            itemEntity.discard();
        } else {
            itemEntity.setItem(stack);
        }

        // Optional: play a pickup sound so it feels responsive.
        player.level().playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                net.minecraft.sounds.SoundEvents.ITEM_PICKUP,
                net.minecraft.sounds.SoundSource.PLAYERS,
                0.2F,
                ((player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F
        );

        return true;
    }
}
