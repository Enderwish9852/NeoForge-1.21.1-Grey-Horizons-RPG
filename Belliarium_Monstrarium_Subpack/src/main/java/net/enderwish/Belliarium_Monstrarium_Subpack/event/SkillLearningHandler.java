package net.enderwish.Belliarium_Monstrarium_Subpack.event;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.ModDataComponents;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.skills.SkillDefinition;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.skills.SkillLearningManager;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.skills.SkillRegistry;
import net.enderwish.Belliarium_Monstrarium_Subpack.item.SkillBookItem;
import net.enderwish.Belliarium_Monstrarium_Subpack.network.ModMessages;
import net.enderwish.Belliarium_Monstrarium_Subpack.network.SkillLearningCompletePacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SkillLearningHandler
 *
 * CLARIFICATION (not a bug) -- see class doc comment summary in chat
 * message. onExplicitClose now sends a chat confirmation with the exact
 * saved tick count on close, so progress persistence is directly visible
 * rather than something you have to take my word for.
 */
@EventBusSubscriber(modid = BelliariumMonstrariumSubpack.MODID)
public class SkillLearningHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("GHBelliarium/SkillLearning");
    private static final long HEARTBEAT_TIMEOUT_TICKS = 60;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide()) return;

        SkillLearningManager.ActiveLearning active = SkillLearningManager.INSTANCE.get(player.getUUID());
        if (active == null) return;

        long gameTime = player.level().getGameTime();
        boolean stale = gameTime - active.lastHeartbeatTick() > HEARTBEAT_TIMEOUT_TICKS;

        ItemStack stack = player.getItemInHand(active.hand());
        boolean stillHoldingBook = !stack.isEmpty() && stack.getItem() instanceof SkillBookItem
                && active.skillId().equals(stack.get(ModDataComponents.SKILL_ID.get()));

        if (stale || !stillHoldingBook) {
            if (stale) {
                LOGGER.warn("{} stopped sending learning heartbeats for skill '{}' -- learning paused.",
                        player.getName().getString(), active.skillId());
            }
            if (!stack.isEmpty()) player.getCooldowns().removeCooldown(stack.getItem());
            SkillLearningManager.INSTANCE.clear(player.getUUID());
            return;
        }

        SkillRegistry.INSTANCE.getSkill(active.skillId()).ifPresent(def -> advance(player, stack, def));
    }

    private static void advance(ServerPlayer player, ItemStack stack, SkillDefinition def) {
        if (stack.getOrDefault(ModDataComponents.LEARNING_COMPLETE.get(), false)) return;

        int progress = stack.getOrDefault(ModDataComponents.LEARNING_PROGRESS.get(), 0) + 1;
        stack.set(ModDataComponents.LEARNING_PROGRESS.get(), progress);

        if (progress >= def.getLearningTimeTicks()) {
            stack.set(ModDataComponents.LEARNING_COMPLETE.get(), true);
            player.getCooldowns().removeCooldown(stack.getItem());
            ModMessages.sendToPlayer(new SkillLearningCompletePacket(def.skillId()), player);
        }
    }

    public static void onHeartbeat(ServerPlayer player, InteractionHand hand, String skillId) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty() || !(stack.getItem() instanceof SkillBookItem)) return;
        if (!skillId.equals(stack.get(ModDataComponents.SKILL_ID.get()))) return;

        boolean wasActive = SkillLearningManager.INSTANCE.get(player.getUUID()) != null;
        SkillLearningManager.INSTANCE.heartbeat(player.getUUID(), hand, skillId, player.level().getGameTime());

        if (!wasActive) {
            SkillRegistry.INSTANCE.getSkill(skillId).ifPresent(def -> {
                boolean complete = stack.getOrDefault(ModDataComponents.LEARNING_COMPLETE.get(), false);
                if (!complete) {
                    int progress = stack.getOrDefault(ModDataComponents.LEARNING_PROGRESS.get(), 0);
                    int remaining = Math.max(1, def.getLearningTimeTicks() - progress);
                    player.getCooldowns().addCooldown(stack.getItem(), remaining);
                }
            });
        }
    }

    public static void onExplicitClose(ServerPlayer player, InteractionHand hand, String skillId) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.isEmpty()) player.getCooldowns().removeCooldown(stack.getItem());
        SkillLearningManager.INSTANCE.clear(player.getUUID());

        SkillRegistry.INSTANCE.getSkill(skillId).ifPresent(def -> {
            int progress = stack.isEmpty() ? 0 : stack.getOrDefault(ModDataComponents.LEARNING_PROGRESS.get(), 0);
            player.displayClientMessage(Component.literal(
                    "Learning paused -- progress saved: " + progress + " / " + def.getLearningTimeTicks() + " ticks."), true);
        });
    }
}
