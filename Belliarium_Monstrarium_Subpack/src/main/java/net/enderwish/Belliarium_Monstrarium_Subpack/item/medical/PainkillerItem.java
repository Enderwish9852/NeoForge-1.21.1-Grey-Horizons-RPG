package net.enderwish.Belliarium_Monstrarium_Subpack.item.medical;

import net.enderwish.Belliarium_Monstrarium_Subpack.core.medical.PainkillerManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** DURATION_TICKS made public -- FirstAidTriage and UseKitSlotPacket both read it directly now. */
public class PainkillerItem extends Item {

    public static final int DURATION_TICKS = 2400; // 2 minutes, first-pass number

    public PainkillerItem(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && player instanceof ServerPlayer) {
            PainkillerManager.INSTANCE.activate(player.getUUID(), level.getGameTime() + DURATION_TICKS);
            stack.shrink(1);
        }
        return InteractionResultHolder.success(stack);
    }
}
