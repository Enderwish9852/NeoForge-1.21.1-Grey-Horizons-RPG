package net.enderwish.Belliarium_Monstrarium_Subpack.item;

import net.enderwish.Belliarium_Monstrarium_Subpack.client.SkillBookScreen;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.ModDataComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * SkillBookItem
 *
 * Deliberately NOT a WrittenBookItem subclass -- see chat message intro
 * for why SkillBookScreen is a plain custom Screen instead of vanilla's
 * BookViewScreen.
 */
public class SkillBookItem extends Item {

    public SkillBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            String skillId = stack.get(ModDataComponents.SKILL_ID.get());
            if (skillId != null) {
                Minecraft.getInstance().setScreen(new SkillBookScreen(skillId, hand));
            }
        }
        return InteractionResultHolder.success(stack);
    }
}
