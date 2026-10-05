package net.enderwish.Belliarium_Monstrarium_Subpack.item;

import net.enderwish.Belliarium_Monstrarium_Subpack.client.SkillBookScreen;
import net.enderwish.Belliarium_Monstrarium_Subpack.client.SkillKeybindCaptureScreen;
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
 * BUGFIX (stuck on fluff page) -- use() now branches on LEARNING_COMPLETE.
 * Once learning is done there's nothing left to "read" -- right-clicking
 * now always returns to SkillKeybindCaptureScreen, so Esc-ing that screen
 * to bind later is fully recoverable instead of a dead end.
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
            if (skillId == null) return InteractionResultHolder.success(stack);

            boolean complete = stack.getOrDefault(ModDataComponents.LEARNING_COMPLETE.get(), false);
            if (complete) {
                Minecraft.getInstance().setScreen(new SkillKeybindCaptureScreen(skillId));
            } else {
                Minecraft.getInstance().setScreen(new SkillBookScreen(skillId, hand));
            }
        }
        return InteractionResultHolder.success(stack);
    }
}
