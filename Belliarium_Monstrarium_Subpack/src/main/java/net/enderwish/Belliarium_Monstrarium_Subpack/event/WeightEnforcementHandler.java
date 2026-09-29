package net.enderwish.Belliarium_Monstrarium_Subpack.event;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.WeightRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * WeightEnforcementHandler
 *
 * NEW: enforceMaxWeight() fills in the "refuse item" logic. Instead of
 * guessing at one pickup-interception event (items can enter an inventory
 * via ground pickup, crafting, container transfers... catching each path
 * separately is fragile), total weight is checked every tick and whole
 * stacks are ejected from the END of the inventory backward until back
 * under the cap. Ejecting from the back keeps your hotbar safe. Effect
 * for the player: the item pops back out almost instantly with a message.
 */
@EventBusSubscriber(modid = BelliariumMonstrariumSubpack.MODID)
public class WeightEnforcementHandler {

    private static final double REFERENCE_MASS_KG = 60.0;
    public static final float MAX_CARRY_KG = (float) (REFERENCE_MASS_KG * 0.5); // 30kg

    public static float getCurrentWeight(Player player) {
        float total = 0f;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty()) total += WeightRegistry.INSTANCE.getWeightKg(stack);
        }
        return total;
    }

    public static float getWeightFraction(Player player) {
        return getCurrentWeight(player) / MAX_CARRY_KG;
    }

    private static void enforceMaxWeight(Player player) {
        float total = getCurrentWeight(player);
        if (total <= MAX_CARRY_KG) return;

        Inventory inventory = player.getInventory();
        boolean ejectedAny = false;

        for (int i = inventory.getContainerSize() - 1; i >= 0 && total > MAX_CARRY_KG; i--) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) continue;

            float stackWeight = WeightRegistry.INSTANCE.getWeightKg(stack);
            inventory.setItem(i, ItemStack.EMPTY);
            player.drop(stack, false);
            total -= stackWeight;
            ejectedAny = true;
        }

        if (ejectedAny) {
            player.displayClientMessage(Component.literal("Exceeded max carry weight."), true);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.isCreative() || player.isSpectator()) return;
        if (player.level().isClientSide()) return;

        enforceMaxWeight(player);

        float fraction = getWeightFraction(player);

        if (fraction >= 0.75f) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2, false, false));
        } else if (fraction >= 0.50f) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, false));
        } else if (fraction >= 0.25f) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, false));
        }
    }
}
