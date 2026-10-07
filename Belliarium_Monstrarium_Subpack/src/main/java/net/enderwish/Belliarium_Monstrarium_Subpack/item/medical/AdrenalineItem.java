package net.enderwish.Belliarium_Monstrarium_Subpack.item.medical;

import net.enderwish.Belliarium_Monstrarium_Subpack.core.medical.AdrenalineManager;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.medical.PainkillerManager;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * AdrenalineItem
 *
 * DURATION_TICKS made public for UseKitSlotPacket. Also quietly removed
 * STAMINA_LOCK_TICKS -- it was declared here but never actually used; the
 * real aftermath lock duration lives as its own constant inside
 * BodyDamageHandler (ADRENALINE_STAMINA_LOCK_TICKS). Worth knowing that's
 * a duplicated number across two files right now, not a single source of
 * truth -- flagging it rather than silently rewriting BodyDamageHandler
 * again this turn since you didn't ask for that file this round.
 */
public class AdrenalineItem extends Item {

    public static final int DURATION_TICKS = 1200; // first-pass number

    public AdrenalineItem(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            long endTick = level.getGameTime() + DURATION_TICKS;
            AdrenalineManager.INSTANCE.activate(player.getUUID(), endTick);
            PainkillerManager.INSTANCE.activate(player.getUUID(), endTick);
            stack.shrink(1);
        }
        return InteractionResultHolder.success(stack);
    }
}
