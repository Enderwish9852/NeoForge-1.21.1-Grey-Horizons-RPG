package net.enderwish.Belliarium_Monstrarium_Subpack.core.gear.backpack;

import net.enderwish.Belliarium_Monstrarium_Subpack.core.ModAttachments;
import net.enderwish.Belliarium_Monstrarium_Subpack.item.gear.BackpackItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * BackpackExtensionHelper
 *
 * COMMON code (no client imports -- the Player mixins call this on both
 * sides). Bridges the player's BACKPACK_EXTENSION attachment to the live
 * special slot (container index 18 = left / 19 = right) of the backpack
 * currently equipped in the Curios "back" slot.
 *
 * THE VIRTUAL HELD STACK: BackpackItem.getContents hands out a fresh COPY
 * of every stack on every call (vanilla ItemContainerContents.copyInto),
 * so reading the slot per tick used to make PlayerMixin.getItemBySlot
 * return a DIFFERENT ItemStack instance each time -- vanilla's item-use
 * state machine needs one stable instance (LivingEntity.updatingUsingItem
 * stops using the moment the hand item stops being that same reference),
 * which is why drinking a water bottle from a special slot played the
 * animation and then silently did nothing. Fix: selecting a side seeds
 * the runtime-only BACKPACK_HELD attachment with a copy of the special
 * slot's stack; that ONE instance is what getItemBySlot(MAINHAND) hands
 * out while the extension is active. Explicit mainhand writes (a drink
 * finishing into a glass bottle) go through pushVirtual -- the virtual
 * stack is replaced AND the new state is written through into the live
 * special slot. In-place mutations (durability) are pushed back into the
 * slot by BackpackExtensionTickHandler's periodic sync, which also exits
 * the extension when the held item was used up or the slot/bag changed
 * out from under it.
 *
 * The side attachment value is a plain byte: NONE (0) means no extension held,
 * LEFT (1) / RIGHT (2) mean the player is currently holding that special
 * slot's item as a virtual extra hotbar slot. The client sets it locally
 * for prediction and mirrors the choice to the server via
 * BackpackExtensionSelectPacket; the server only ever sets it after
 * validating the equipped bag, config and slot contents.
 */
public final class BackpackExtensionHelper {

    private BackpackExtensionHelper() {}

    /** No extension held. */
    public static final byte NONE = 0;
    /** Left special slot (container index 18). */
    public static final byte LEFT = 1;
    /** Right special slot (container index 19). */
    public static final byte RIGHT = 2;

    /** Container indices of the two special slots, reserved by the backpack layout. */
    public static final int LEFT_CONTAINER_INDEX = 18;
    public static final int RIGHT_CONTAINER_INDEX = 19;

    /** The side the player is currently holding, or NONE. */
    public static byte side(Player player) {
        return player.getData(ModAttachments.BACKPACK_EXTENSION);
    }

    public static boolean isActive(Player player) {
        return side(player) != NONE;
    }

    /** Sets the locally-held extension side (attachment only -- no packet here). */
    public static void setSide(Player player, byte side) {
        player.setData(ModAttachments.BACKPACK_EXTENSION, side);
    }

    /**
     * Selects {@code side}: marks the extension active and seeds the
     * virtual held stack from that side's special slot (one stable copy --
     * the instance vanilla's use pipeline will operate on).
     */
    public static void select(Player player, byte side) {
        if (side != LEFT && side != RIGHT) return;
        setSide(player, side);
        player.setData(ModAttachments.BACKPACK_HELD, getSpecialStack(player, side));
    }

    /** Clears the locally-held extension and its virtual held stack
     *  (attachment only -- no packet here). */
    public static void clear(Player player) {
        player.setData(ModAttachments.BACKPACK_HELD, ItemStack.EMPTY);
        setSide(player, NONE);
    }

    /** The special slot's stack for {@code side}, or EMPTY when the equipped bag */
    /** lacks that special or the slot is empty. Independent of the attachment. */
    public static ItemStack getSpecialStack(Player player, byte side) {
        if (side != LEFT && side != RIGHT) return ItemStack.EMPTY;
        ItemStack backpack = BackpackHandler.getEquipped(player);
        if (backpack.isEmpty() || !(backpack.getItem() instanceof BackpackItem item)) return ItemStack.EMPTY;

        BackpackConfig config = item.config(backpack);
        if ((side == LEFT ? config.specialLeft() : config.specialRight()) == null) return ItemStack.EMPTY;

        int index = side == LEFT ? LEFT_CONTAINER_INDEX : RIGHT_CONTAINER_INDEX;
        List<ItemStack> contents = item.getContents(backpack);
        if (index < 0 || index >= contents.size()) return ItemStack.EMPTY;

        ItemStack stack = contents.get(index);
        return stack == null ? ItemStack.EMPTY : stack;
    }

    /** True when the equipped bag declares {@code side} and its special slot is non-empty. */
    public static boolean isAvailable(Player player, byte side) {
        return !getSpecialStack(player, side).isEmpty();
    }

    /**
     * The virtual held stack while the extension is active: the ONE stable
     * instance PlayerMixin.getItemBySlot(MAINHAND) hands out (see the class
     * doc). EMPTY when no extension is active or nothing was seeded --
     * vanilla then falls through to the real hotbar item.
     */
    public static ItemStack getExtensionStack(Player player) {
        if (!isActive(player)) return ItemStack.EMPTY;
        return player.getData(ModAttachments.BACKPACK_HELD);
    }

    /**
     * A mainhand write while the extension is active (vanilla's
     * setItemInHand -> setItemSlot, e.g. a drink finishing into a glass
     * bottle, or an item breaking): the virtual held stack becomes
     * {@code stack} and the new state is written through into the live
     * special slot. Called by PlayerMixin's setItemSlot redirect.
     */
    public static void pushVirtual(Player player, ItemStack stack) {
        if (!isActive(player)) return;
        player.setData(ModAttachments.BACKPACK_HELD, stack);
        writeVirtualToSlot(player, stack);
    }

    /**
     * Server-side consistency pass (BackpackExtensionTickHandler, every 10
     * ticks while active):
     *  - virtual held EMPTY -- the item was used up in place (e.g. the
     *    last ender pearl): clears the special slot it came from, exits.
     *  - special slot EMPTY -- it was taken out through the backpack GUI
     *    or the bag was unequipped/replaced: the outside world is the
     *    truth, exits WITHOUT writing anything back.
     *  - same item, different state -- an in-place mutation of the virtual
     *    (durability, count): writes the virtual back into the slot.
     *  - a different item entirely (GUI swap while active): exits, the
     *    slot wins.
     */
    public static void syncExtension(Player player) {
        if (!isActive(player)) return;
        ItemStack virtual = player.getData(ModAttachments.BACKPACK_HELD);
        if (virtual.isEmpty()) {
            writeVirtualToSlot(player, ItemStack.EMPTY);
            clear(player);
            return;
        }
        ItemStack slot = getSpecialStack(player, side(player));
        if (slot.isEmpty()) {
            clear(player);
            return;
        }
        if (ItemStack.isSameItem(virtual, slot)) {
            if (!ItemStack.matches(virtual, slot)) writeVirtualToSlot(player, virtual);
        } else {
            clear(player);
        }
    }

    /**
     * Writes {@code stack} into the active side's special slot on the live
     * Curios stack. Used by pushVirtual and syncExtension -- a raw write
     * (like vanilla's items.set), deliberately bypassing onEquipItem
     * semantics.
     */
    private static void writeVirtualToSlot(Player player, ItemStack stack) {
        byte s = side(player);
        if (s != LEFT && s != RIGHT) return;
        ItemStack backpack = BackpackHandler.getEquipped(player);
        if (backpack.isEmpty() || !(backpack.getItem() instanceof BackpackItem item)) return;

        BackpackConfig config = item.config(backpack);
        if ((s == LEFT ? config.specialLeft() : config.specialRight()) == null) return;

        int index = s == LEFT ? LEFT_CONTAINER_INDEX : RIGHT_CONTAINER_INDEX;
        List<ItemStack> contents = item.getContents(backpack);
        if (index < 0 || index >= contents.size()) return;
        contents.set(index, stack);
        item.writeContents(backpack, contents);
    }
}
