package net.enderwish.Belliarium_Monstrarium_Subpack.event;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.body.BodyPartDebuffs;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.gear.WeightRegistry;
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
 * NEW -- HEAVY_PICKUP_WEIGHT_KG gate added. Pickup already only ever fires
 * off MAIN_HAND (see the hand filters below), which lines up exactly with
 * "damaged arm -> can't pick up heavy items" being evaluated per the
 * MAIN_HAND-mapped arm -- no extra wiring needed to connect the two.
 */
@EventBusSubscriber(modid = BelliariumMonstrariumSubpack.MODID)
public class ItemPickupHandler {

    private static final double PICKUP_REACH = 4.0;
    private static final float HEAVY_PICKUP_WEIGHT_KG = 5.0f;

    @SubscribeEvent
    public static void onAutoPickup(ItemEntityPickupEvent.Pre event) {
        event.setCanPickup(TriState.FALSE);
    }

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

    private static ItemEntity findLookedAtItem(ServerPlayer player) {
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = eye.add(look.scale(PICKUP_REACH));

        HitResult blockHit = player.level().clip(new ClipContext(
                eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (blockHit.getType() != HitResult.Type.MISS) {
            end = blockHit.getLocation();
        }

        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(PICKUP_REACH)).inflate(1.0);

        Vec3 finalEnd = end;
        return player.level()
                .getEntities(player, searchBox, e -> e instanceof ItemEntity)
                .stream()
                .filter(e -> e.getBoundingBox().inflate(0.3).clip(eye, finalEnd).isPresent())
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(eye)))
                .map(e -> (ItemEntity) e)
                .orElse(null);
    }

    private static boolean tryPickup(ServerPlayer player, ItemEntity itemEntity) {
        ItemStack stack = itemEntity.getItem().copy();
        if (stack.isEmpty()) {
            itemEntity.discard();
            return false;
        }

        // THE NEW CHECK -- heavy items need a good main-hand-side arm.
        if (BodyPartDebuffs.isHandRedTier(player, InteractionHand.MAIN_HAND)
                && WeightRegistry.INSTANCE.getWeightKg(stack) >= HEAVY_PICKUP_WEIGHT_KG) {
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("That's too heavy to lift with your injured arm."), true);
            return false;
        }

        int before = stack.getCount();
        player.getInventory().add(stack);
        int pickedUp = before - stack.getCount();

        if (pickedUp <= 0) return false;

        if (stack.isEmpty()) {
            itemEntity.discard();
        } else {
            itemEntity.setItem(stack);
        }

        player.level().playSound(
                null, player.getX(), player.getY(), player.getZ(),
                net.minecraft.sounds.SoundEvents.ITEM_PICKUP,
                net.minecraft.sounds.SoundSource.PLAYERS,
                0.2F,
                ((player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F
        );

        return true;
    }
}
