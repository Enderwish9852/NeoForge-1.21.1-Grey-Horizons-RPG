package net.enderwish.Belliarium_Monstrarium_Subpack.event;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.ItemRarity;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.ModDataComponents;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.RepairChargesComponent;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.ToolProfileDefinition;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.ToolProfileRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * RepairChargeHandler
 *
 * Repair happens via "combining with materials in crafting grid" per your
 * spec -- vanilla's own built-in "combine two damaged copies of the same
 * item" crafting recipe, NOT an anvil, grindstone, or Mending.
 *
 * Detected structurally (same PlayerEvent.ItemCraftedEvent + raw-grid-read
 * approach already proven in Farming's CraftingSpoilageHandler) rather than
 * by hunting the exact vanilla repair-recipe class name: a "repair" here is
 * exactly 2 non-empty grid stacks, both the same item as the output, with
 * the output ending up LESS damaged than the lesser-damaged input -- the
 * only thing that produces that result is vanilla's own combine-repair.
 *
 * At 0 charges: ItemCraftedEvent fires only AFTER vanilla has already
 * produced the repaired output and isn't cancelable, so instead of trying
 * to block the craft, the output's damage is forced back to what it would
 * have been WITHOUT the repair (the lesser-damaged input's value) --
 * undoing the free durability vanilla just granted. Chat feedback reuses
 * the exact instant-message pattern WeightEnforcementHandler already uses.
 */
@EventBusSubscriber(modid = BelliariumMonstrariumSubpack.MODID)
public class RepairChargeHandler {

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        ItemStack output = event.getCrafting();
        if (output.isEmpty()) return;

        String itemId = BuiltInRegistries.ITEM.getKey(output.getItem()).toString();
        ToolProfileRegistry.INSTANCE.getProfile(itemId).ifPresent(profile ->
                handlePossibleRepair(player, event, output, profile));
    }

    private static void handlePossibleRepair(Player player, PlayerEvent.ItemCraftedEvent event,
                                             ItemStack output, ToolProfileDefinition profile) {
        ItemStack inputA = ItemStack.EMPTY;
        ItemStack inputB = ItemStack.EMPTY;

        for (int i = 0; i < event.getInventory().getContainerSize(); i++) {
            ItemStack slotStack = event.getInventory().getItem(i);
            if (slotStack.isEmpty()) continue;
            if (!slotStack.is(output.getItem())) return; // not a same-item combine
            if (inputA.isEmpty()) inputA = slotStack;
            else if (inputB.isEmpty()) inputB = slotStack;
            else return; // more than 2 inputs -- not a repair combine
        }

        if (inputA.isEmpty() || inputB.isEmpty()) return; // not a 2-input combine

        int lesserDamage = Math.min(inputA.getDamageValue(), inputB.getDamageValue());
        if (output.getDamageValue() >= lesserDamage) return; // not actually repaired

        ItemRarity rarity = profile.rarity();
        RepairChargesComponent charges = output.getOrDefault(
                ModDataComponents.REPAIR_CHARGES.get(), new RepairChargesComponent(rarity.getMaxRepairCharges()));

        if (charges.hasChargesLeft()) {
            output.set(ModDataComponents.REPAIR_CHARGES.get(), charges.spend());
        } else {
            output.setDamageValue(lesserDamage); // undo vanilla's free repair bonus
            player.displayClientMessage(Component.literal("This item has no repair charges left."), true);
        }
    }
}
