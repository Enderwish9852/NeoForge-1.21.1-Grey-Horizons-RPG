package net.enderwish.Belliarium_Monstrarium_Subpack.item.medical;

import net.enderwish.Belliarium_Monstrarium_Subpack.client.BandageRollModel;
import net.enderwish.Belliarium_Monstrarium_Subpack.client.MedTargetScreen;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.ModAttachments;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.body.BodyHealthCapability;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.body.BodyPart;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.body.BodyPartTier;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.medical.BandagePending;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.medical.TargetedMedItem;
import net.enderwish.Belliarium_Monstrarium_Subpack.network.MedicalAnimPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * BandageItem — TargetedMedItem (YELLOW tier) with hold-to-use.
 *
 * Right-click opens the part-selection screen. After choosing a part,
 * the player must HOLD right-click for the full wrap duration
 * (USE_DURATION_TICKS); the treatment applies the moment the hold
 * completes. Releasing early cancels the hold (pending choice stays
 * — re-hold to continue).
 *
 * While the hold is running, the third-person bandage_wrap animation
 * plays on the player (PlayerAnimator, broadcast to everyone) and the
 * in-hand roll spins (GeckoLib first-person item animation).
 */
public class BandageItem extends Item implements TargetedMedItem, GeoItem {

    /** Hold length = the 54-tick wrap choreography + a small settle. */
    public static final int USE_DURATION_TICKS = 60;

    private final AnimatableInstanceCache cache =
            GeckoLibUtil.createInstanceCache(this);

    // First-person rendering is registered explicitly in
    // client/BandageItemRenderRegistration (RegisterClientExtensionsEvent) —
    // a createGeoRenderer/renderProvider override was never consulted by
    // vanilla, which is why the roll rendered as a flat bobbing sprite.

    public BandageItem(Properties properties) { super(properties); }

    @Override
    public BodyPartTier treatsTier() { return BodyPartTier.YELLOW; }

    @Override
    public float healTargetFraction() { return 1.0f; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // the authoritative pending state lives server-side; the client
        // mirrors it (MedPendingNotifyPacket) — check the right side
        boolean chosen = level.isClientSide()
                ? BandagePending.getClient() != null
                : BandagePending.get(player) != null;

        if (!chosen) {
            // first click: open the part-selection screen
            if (level.isClientSide()) {
                Minecraft.getInstance().setScreen(new MedTargetScreen(hand.ordinal(), -1, stack));
            }
            return InteractionResultHolder.success(stack);
        }

        if (!level.isClientSide()) {
            // hold begins: broadcast the wrap animation to everyone
            PacketDistributor.sendToAllPlayers(new MedicalAnimPacket(
                    player.getUUID(), "bandage_wrap"));
        } else {
            // first-person: spin the roll in hand
            this.triggerAnim(player, hand.ordinal(), "main", "spin");
        }

        // CRITICAL: the engine never starts the hold for you — every vanilla
        // hold-to-use item (bow, brush, food...) calls startUsingItem in its
        // own use(). Without this the hold never engages: use() re-fired
        // every 4 ticks (glitchy restarted animation) and finishUsingItem
        // — the actual heal — never ran.
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack, net.minecraft.world.entity.LivingEntity entity) {
        return USE_DURATION_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE; // arms are driven by the PlayerAnimator choreography
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, net.minecraft.world.entity.LivingEntity entity) {
        // client side: clear the mirror so the next right-click re-opens
        // the picker instead of silently starting a phantom hold
        if (level.isClientSide()) {
            BandagePending.clearClient();
            return stack;
        }
        if (!(entity instanceof ServerPlayer player)) return stack;

        BandagePending.Pending pending = BandagePending.clear(player);
        if (pending == null) return stack;

        BodyPart part = BodyPart.fromDisplayName(pending.bodyPartName());
        if (part == null) return stack;

        BodyHealthCapability cap = player.getData(ModAttachments.BODY_HEALTH);
        BodyPartTier currentTier = BodyPartTier.from(part.getPct(cap));

        if (currentTier != treatsTier() || !canTreat(part)) {
            player.displayClientMessage(Component.literal("That treatment won't help there."), true);
            return stack;
        }

        applyTreatment(cap, part);
        player.displayClientMessage(Component.literal("Bandage applied."), true);
        stack.shrink(1);
        return stack;
    }

    // ── GeckoLib (first-person in-hand model) ────────────────────────────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // one-shot triggerable anim: the in-hand roll spins for ~3s
        controllers.add(new software.bernie.geckolib.animation.AnimationController<>(
                this, "main", state -> software.bernie.geckolib.animation.PlayState.STOP)
                .triggerableAnim("spin",
                        software.bernie.geckolib.animation.RawAnimation.begin()
                                .thenPlayAndHold("spin")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public double getTick(Object itemStack) {
        // continuous 20-tick/s clock for the in-hand renderer
        return itemStack instanceof Entity entity
                ? entity.tickCount
                : (double) (System.nanoTime() / 50_000_000L);
    }
}
