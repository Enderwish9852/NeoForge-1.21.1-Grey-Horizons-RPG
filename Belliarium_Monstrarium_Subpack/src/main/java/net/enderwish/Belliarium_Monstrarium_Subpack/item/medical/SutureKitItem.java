package net.enderwish.Belliarium_Monstrarium_Subpack.item.medical;

import net.enderwish.Belliarium_Monstrarium_Subpack.client.MedTargetScreen;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.body.BodyPartTier;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.medical.TargetedMedItem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * SutureKitItem
 *
 * Heals RED up to the midpoint of YELLOW (0.5) -- stable, not cosmetically
 * healed, a Bandage is still needed afterward. The exact target fraction
 * is my own judgment call, not something you specified -- tell me if you
 * want it landing somewhere else in the Yellow band.
 */
public class SutureKitItem extends Item implements TargetedMedItem {

    public SutureKitItem(Properties properties) { super(properties); }

    @Override
    public BodyPartTier treatsTier() { return BodyPartTier.RED; }

    @Override
    public float healTargetFraction() { return 0.5f; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            Minecraft.getInstance().setScreen(new MedTargetScreen(hand.ordinal(), -1, stack));
        }
        return InteractionResultHolder.success(stack);
    }
}
