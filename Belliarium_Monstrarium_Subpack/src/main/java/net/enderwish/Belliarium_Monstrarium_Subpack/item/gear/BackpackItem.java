package net.enderwish.Belliarium_Monstrarium_Subpack.item.gear;

import net.enderwish.Belliarium_Monstrarium_Subpack.core.ModDataComponents;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.gear.backpack.BackpackConfig;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.gear.backpack.BackpackRegistry;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.List;

/**
 * BackpackItem
 *
 * A tier backpack: equips into the Curios "back" slot (validity comes from
 * the curios:item/back tag, same as the wrist watch). Contents live in the
 * stack's vanilla DataComponents.CONTAINER, so they persist through
 * unequip, drop and death exactly like a shulker box.
 *
 * The tier's tuning (slots, self weight, buffs) lives in
 * data/gh_belliarium_monstrarium/backpacks/&lt;id&gt;.json — this class only
 * reads it. The backpack id is stamped on every stack at registration.
 */
public class BackpackItem extends Item {

    public BackpackItem(Properties properties) {
        super(properties);
    }

    public String backpackId(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.BACKPACK_ID.get(), "satchel");
    }

    public BackpackConfig config(ItemStack stack) {
        return BackpackRegistry.INSTANCE.getOrFallback(backpackId(stack));
    }

    // ── Contents (vanilla CONTAINER component — shulker semantics) ──────────

    public List<ItemStack> getContents(ItemStack stack) {
        ItemContainerContents comp = stack.getOrDefault(
                net.minecraft.core.component.DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        // copyInto is slot-aligned: index i = slot i, padded with EMPTY
        net.minecraft.core.NonNullList<ItemStack> tmp =
                net.minecraft.core.NonNullList.withSize(config(stack).slots(), ItemStack.EMPTY);
        comp.copyInto(tmp);
        return tmp;
    }

    public void writeContents(ItemStack stack, List<ItemStack> contents) {
        stack.set(net.minecraft.core.component.DataComponents.CONTAINER,
                ItemContainerContents.fromItems(contents));
    }

    /** True if any slot can accept this stack (merge or empty slot). */
    public boolean hasRoomFor(ItemStack backpack, ItemStack incoming) {
        List<ItemStack> slots = getContents(backpack);
        for (ItemStack s : slots) {
            if (s.isEmpty()) return true;
            if (ItemStack.isSameItemSameComponents(s, incoming)
                    && s.getCount() + incoming.getCount() <= s.getMaxStackSize()) return true;
        }
        return false;
    }

    /** Inserts into an existing mergeable stack first, then an empty slot. Returns leftover count. */
    public int insert(ItemStack backpack, ItemStack incoming) {
        List<ItemStack> slots = getContents(backpack);
        int remaining = incoming.getCount();

        for (int i = 0; i < slots.size() && remaining > 0; i++) {
            ItemStack s = slots.get(i);
            if (!s.isEmpty() && ItemStack.isSameItemSameComponents(s, incoming)) {
                int canTake = Math.min(remaining, s.getMaxStackSize() - s.getCount());
                if (canTake > 0) {
                    s.grow(canTake);
                    remaining -= canTake;
                }
            }
        }
        for (int i = 0; i < slots.size() && remaining > 0; i++) {
            if (slots.get(i).isEmpty()) {
                int take = Math.min(remaining, incoming.getMaxStackSize());
                slots.set(i, incoming.copyWithCount(take));
                remaining -= take;
            }
        }
        if (remaining != incoming.getCount()) {
            writeContents(backpack, slots);
        }
        return remaining;
    }
}
