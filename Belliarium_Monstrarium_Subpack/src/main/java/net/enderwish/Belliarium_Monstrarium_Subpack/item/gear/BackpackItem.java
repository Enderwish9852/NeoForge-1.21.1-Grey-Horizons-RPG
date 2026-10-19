package net.enderwish.Belliarium_Monstrarium_Subpack.item.gear;

import net.enderwish.Belliarium_Monstrarium_Subpack.core.ModDataComponents;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.gear.backpack.BackpackConfig;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.gear.backpack.BackpackRegistry;
import net.enderwish.Belliarium_Monstrarium_Subpack.gui.BackpackMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;

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

    /**
     * Right-click while holding the bag opens that bag's GUI, bound to the
     * hand stack (B still opens the EQUIPPED bag). The menu-open buffer
     * carries the hand binding so the client menu resolves the same stack.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            BackpackConfig config = config(stack);
            serverPlayer.openMenu(new MenuProvider() {
                @Override
                public net.minecraft.network.chat.Component getDisplayName() {
                    return stack.getHoverName();
                }

                @Override
                public AbstractContainerMenu createMenu(int windowId, Inventory inv, Player p) {
                    return new BackpackMenu(windowId, inv, config,
                            p.getItemInHand(hand), BackpackMenu.BIND_HAND, hand);
                }
            }, buf -> BackpackMenu.writeConfig(buf, config, BackpackMenu.BIND_HAND, hand));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
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

    /**
     * Number of GENERAL slots (container indices 0..N-1). The last two
     * container indices are reserved for the special (category-filtered)
     * slots when the config declares them — general placement must never
     * write into those.
     */
    private int generalSlotCount(ItemStack backpack) {
        BackpackConfig cfg = config(backpack);
        int specials = (cfg.specialLeft() != null ? 1 : 0) + (cfg.specialRight() != null ? 1 : 0);
        return Math.max(0, cfg.slots() - specials);
    }

    /** True if any GENERAL slot can accept this stack (merge or empty slot). Backpacks never fit — no nesting. */
    public boolean hasRoomFor(ItemStack backpack, ItemStack incoming) {
        if (incoming.getItem() instanceof BackpackItem) return false;
        List<ItemStack> slots = getContents(backpack);
        int general = generalSlotCount(backpack);
        for (int i = 0; i < general; i++) {
            ItemStack s = slots.get(i);
            if (s.isEmpty()) return true;
            if (ItemStack.isSameItemSameComponents(s, incoming)
                    && s.getCount() + incoming.getCount() <= s.getMaxStackSize()) return true;
        }
        return false;
    }

    /** Inserts into an existing mergeable GENERAL stack first, then an empty GENERAL slot. Returns leftover count. Backpacks never fit — no nesting. */
    public int insert(ItemStack backpack, ItemStack incoming) {
        if (incoming.getItem() instanceof BackpackItem) return incoming.getCount();
        List<ItemStack> slots = getContents(backpack);
        int general = generalSlotCount(backpack);
        int remaining = incoming.getCount();

        for (int i = 0; i < general && remaining > 0; i++) {
            ItemStack s = slots.get(i);
            if (!s.isEmpty() && ItemStack.isSameItemSameComponents(s, incoming)) {
                int canTake = Math.min(remaining, s.getMaxStackSize() - s.getCount());
                if (canTake > 0) {
                    s.grow(canTake);
                    remaining -= canTake;
                }
            }
        }
        for (int i = 0; i < general && remaining > 0; i++) {
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
