package net.enderwish.Belliarium_Monstrarium_Subpack.event;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.body.BodyPartDebuffs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * BodyPartDebuffHandler
 *
 * Grey arm -> that hand's item drops (checked every tick but naturally
 * fires once, since an empty hand has nothing left to drop) and the hand
 * is fully locked (all interactions cancelled below). Red arm (not grey)
 * is the lighter restriction -- attacks and item pickup only.
 *
 * Grey leg -> forced into Pose.SWIMMING, the SAME pose vanilla already
 * uses for crawling through 1-block gaps on land, reused here rather than
 * inventing a new one (a real new Pose enum value isn't something a mod
 * can add). Red leg -> flat 50% movement speed penalty.
 *
 * VERIFY IN-GAME -- forcing Pose.SWIMMING here competes with vanilla's own
 * per-tick pose recalculation; I can't rule out vanilla silently
 * overriding it back some tick. If the crawl pose flickers, this needs a
 * mixin into the pose-refresh method instead -- flag it and I'll dig in.
 *
 * VERIFY IF COMPILE FAILS -- AttributeModifier's ResourceLocation-keyed
 * constructor (replacing the older UUID+String form) is my best-confidence
 * read of 1.21's attribute-modifier changes, not confirmed against source
 * pasted in this conversation.
 */
@EventBusSubscriber(modid = BelliariumMonstrariumSubpack.MODID)
public class BodyPartDebuffHandler {

    private static final ResourceLocation RED_LEG_ID =
            ResourceLocation.fromNamespaceAndPath(BelliariumMonstrariumSubpack.MODID, "red_leg_limp");
    private static final ResourceLocation GREY_LEG_ID =
            ResourceLocation.fromNamespaceAndPath(BelliariumMonstrariumSubpack.MODID, "grey_leg_crawl");

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        if (player.isCreative() || player.isSpectator()) return;

        dropIfHandGrey(player, InteractionHand.MAIN_HAND);
        dropIfHandGrey(player, InteractionHand.OFF_HAND);
        applyLegEffects(player);
    }

    private static void dropIfHandGrey(Player player, InteractionHand hand) {
        if (!BodyPartDebuffs.isHandLocked(player, hand)) return;

        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty()) return;

        ItemEntity dropped = new ItemEntity(player.level(),
                player.getX(), player.getY() + 0.3, player.getZ(), stack.copy());
        dropped.setPickUpDelay(40);
        player.level().addFreshEntity(dropped);
        player.setItemInHand(hand, ItemStack.EMPTY);
    }

    private static void applyLegEffects(Player player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;

        speed.removeModifier(RED_LEG_ID);
        speed.removeModifier(GREY_LEG_ID);

        if (BodyPartDebuffs.isEitherLegGrey(player)) {
            player.setPose(Pose.SWIMMING);
            speed.addPermanentModifier(new AttributeModifier(
                    GREY_LEG_ID, -0.85, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        } else if (BodyPartDebuffs.isEitherLegRed(player)) {
            speed.addPermanentModifier(new AttributeModifier(
                    RED_LEG_ID, -0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player.isCreative()) return;
        if (BodyPartDebuffs.isHandRedTier(player, InteractionHand.MAIN_HAND)
                || BodyPartDebuffs.isHandLocked(player, InteractionHand.MAIN_HAND)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (BodyPartDebuffs.isHandLocked(event.getEntity(), event.getHand())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (BodyPartDebuffs.isHandLocked(event.getEntity(), event.getHand())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (BodyPartDebuffs.isHandLocked(event.getEntity(), event.getHand())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }
}
