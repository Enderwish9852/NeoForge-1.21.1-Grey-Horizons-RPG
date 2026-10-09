package net.enderwish.Belliarium_Monstrarium_Subpack.item.medical;

import net.enderwish.Belliarium_Monstrarium_Subpack.client.MedTargetScreen;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.body.BodyPartTier;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.medical.TargetedMedItem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class BandageItem extends Item implements TargetedMedItem {

    public BandageItem(Properties properties) { super(properties); }

    @Override
    public BodyPartTier treatsTier() { return BodyPartTier.YELLOW; }

    @Override
    public float healTargetFraction() { return 1.0f; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            Minecraft.getInstance().setScreen(new MedTargetScreen(hand.ordinal(), -1, stack));
        }
        return InteractionResultHolder.success(stack);
    }
}