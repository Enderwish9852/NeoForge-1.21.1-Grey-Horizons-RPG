package net.enderwish.Belliarium_Monstrarium_Subpack.gui;

import net.enderwish.Belliarium_Monstrarium_Subpack.core.gear.ToolProfileRegistry;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.gear.backpack.BackpackConfig;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.gear.backpack.BackpackHandler;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.integration.CuriosHooks;
import net.enderwish.Belliarium_Monstrarium_Subpack.init.ModMenuTypes;
import net.enderwish.Belliarium_Monstrarium_Subpack.item.gear.BackpackItem;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * BackpackMenu
 *
 * Container menu for the equipped backpack (Curios "back" slot). The bag
 * slots are a write-through view over the LIVE stack's vanilla
 * CONTAINER component: every setChanged() mirrors the slot list back on
 * to the stack, so contents persist through unequip/drop/death with
 * shulker-box semantics.
 *
 * The menu owns the layout: the full BackpackConfig is serialized into
 * the menu-open buffer on the server, so client and server menus always
 * agree on slot count, special slots and pixel coordinates without the
 * client ever touching BackpackRegistry (which is server-side only).
 */
public class BackpackMenu extends AbstractContainerMenu {

    /** Pixel layout for one backpack tier. All values match the GUI PNGs. */
    public record Layout(
            int imageWidth, int imageHeight,
            int bagStartX, int bagRow1Y, int bagRow2Y,
            int specialLeftX, int specialLeftY,
            int specialRightX, int specialRightY,
            int hotbarX, int hotbarY) {}

    private static final Layout SCHOOL = new Layout(
            176, 78, 8, 18, -100,
            -100, -100, -100, -100, 8, 54);
    private static final Layout RUCKSACK = new Layout(
            176, 93, 8, 18, 36,
            -100, -100, -100, -100, 8, 69);
    private static final Layout BIG = new Layout(
            212, 92, 26, 18, 36,
            8, 36, 188, 36, 26, 68);

    /**
     * Layout derivation (per tier, from the user's spec rectangles):
     * each spec rectangle (e.g. (7,17) to (25,35)) is a full 18x18 cell
     * INCLUDING its 1px borders, and its stated bottom-right corner sits
     * one pixel PAST the cell's real edge — so the cell actually spans
     * (7,17)-(24,34), its 16x16 item area starts one pixel inside, and
     * every SLOT coordinate (where the game draws the item and hover
     * highlight) is the spec top-left PLUS ONE. The image size is the
     * spec size verbatim.
     */

    /** Which stack this menu is bound to: the Curios "back" stack or one held in a hand. */
    public static final byte BIND_EQUIPPED = 0;
    public static final byte BIND_HAND = 1;

    private final String backpackId;
    private final BackpackConfig config;
    private final Layout layout;
    private final int bagSlots;       // total container slots = this.slots' bag range
    private final int generalSlots;   // container indices 0..generalSlots-1
    private final List<Integer> specialSlotIndices = new ArrayList<>();
    private final BackpackStackContainer container;
    private final byte bind;
    private final InteractionHand hand;
    private final ItemStack bound;

    // ── Constructors ──────────────────────────────────────────────────────────

    /** Server side: called while opening the menu with the live stack in hand. */
    public BackpackMenu(int containerId, Inventory playerInventory,
                        BackpackConfig config, ItemStack backpackStack) {
        this(containerId, playerInventory, config, backpackStack, BIND_EQUIPPED, null);
    }

    public BackpackMenu(int containerId, Inventory playerInventory,
                        BackpackConfig config, ItemStack backpackStack,
                        byte bind, InteractionHand hand) {
        super(ModMenuTypes.BACKPACK.get(), containerId);
        this.backpackId = config.id();
        this.config = config;
        this.layout = layoutOf(config);
        this.bagSlots = config.slots();
        this.generalSlots = bagSlots
                - (config.specialLeft() != null ? 1 : 0)
                - (config.specialRight() != null ? 1 : 0);
        this.container = new BackpackStackContainer(backpackStack, config);
        this.bind = bind;
        this.hand = hand;
        this.bound = backpackStack;

        int specialCount = bagSlots - generalSlots;

        // General bag slots (container indices 0..generalSlots-1) — no nesting:
        // a backpack may never be placed inside a backpack.
        for (int i = 0; i < generalSlots; i++) {
            int row = i / 9, col = i % 9;
            int x = layout.bagStartX() + col * 18;
            int y = row == 0 ? layout.bagRow1Y() : layout.bagRow2Y();
            this.addSlot(new GeneralSlot(container, i, x, y));
        }

        // Special bag slots (the LAST container indices, category-filtered)
        int nextSlotIndex = generalSlots;
        if (config.specialLeft() != null) {
            this.addSlot(new SpecialSlot(container, bagSlots - specialCount,
                    layout.specialLeftX(), layout.specialLeftY(), config.specialLeft()));
            specialSlotIndices.add(nextSlotIndex++);
        }
        if (config.specialRight() != null) {
            this.addSlot(new SpecialSlot(container, bagSlots - 1,
                    layout.specialRightX(), layout.specialRightY(), config.specialRight()));
            specialSlotIndices.add(nextSlotIndex++);
        }

        // Player hotbar (Inventory slots 0-8)
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col,
                    layout.hotbarX() + col * 18, layout.hotbarY()));
        }
    }

    /** Client side: bind mode + hand ordinal + config arrive in the menu-open buffer. */
    public BackpackMenu(int containerId, Inventory playerInventory,
                        RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory, readBufferData(extraData), playerInventory.player);
    }

    /** Everything the menu-open buffer carries. */
    private record BufferData(byte bind, InteractionHand hand, BackpackConfig config) {}

    /** Read order must mirror writeConfig: bind byte, hand byte, then the config fields. */
    private static BufferData readBufferData(FriendlyByteBuf buf) {
        byte bind = buf.readByte();
        InteractionHand hand = buf.readByte() == 0
                ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        return new BufferData(bind, hand, readConfig(buf));
    }

    private BackpackMenu(int containerId, Inventory playerInventory,
                        BufferData data, Player player) {
        this(containerId, playerInventory, data.config(), resolveStack(player, data));
    }

    /** The stack this menu edits, resolved on the client from the bind bytes. */
    private static ItemStack resolveStack(Player player, BufferData data) {
        return data.bind() == BIND_HAND
                ? player.getItemInHand(data.hand())
                : CuriosHooks.getBackpack(player);
    }

    /** Serializes the config into the menu-open buffer (server, equipped binding). */
    public static void writeConfig(FriendlyByteBuf buf, BackpackConfig config) {
        writeConfig(buf, config, BIND_EQUIPPED, InteractionHand.MAIN_HAND);
    }

    /** Serializes the bind + hand + config into the menu-open buffer (server). */
    public static void writeConfig(FriendlyByteBuf buf, BackpackConfig config,
                                   byte bind, InteractionHand hand) {
        buf.writeByte(bind);
        buf.writeByte(hand == InteractionHand.MAIN_HAND ? 0 : 1);
        buf.writeUtf(config.id());
        buf.writeVarInt(config.slots());
        buf.writeFloat(config.selfWeightKg());
        buf.writeFloat(config.carryCapBonusKg());
        buf.writeFloat(config.speedBonusPct());
        buf.writeFloat(config.staminaEfficiencyPct());
        writeNullableString(buf, config.specialLeft());
        writeNullableString(buf, config.specialRight());
    }

    private static BackpackConfig readConfig(FriendlyByteBuf buf) {
        String id = buf.readUtf();
        int slots = buf.readVarInt();
        float selfWeightKg = buf.readFloat();
        float carryCapBonusKg = buf.readFloat();
        float speedBonusPct = buf.readFloat();
        float staminaEfficiencyPct = buf.readFloat();
        String specialLeft = readNullableString(buf);
        String specialRight = readNullableString(buf);
        return new BackpackConfig(id, slots, selfWeightKg, carryCapBonusKg,
                speedBonusPct, staminaEfficiencyPct, specialLeft, specialRight);
    }

    private static void writeNullableString(FriendlyByteBuf buf, String s) {
        buf.writeBoolean(s != null);
        if (s != null) buf.writeUtf(s);
    }

    private static String readNullableString(FriendlyByteBuf buf) {
        return buf.readBoolean() ? buf.readUtf() : null;
    }

    /** Per-tier layout, keyed by slot count — exactly matching the PNGs. */
    private static Layout layoutOf(BackpackConfig config) {
        if (config.slots() == 9) return SCHOOL;
        if (config.slots() == 18) return RUCKSACK;
        return BIG;
    }

    // ── Accessors the screen needs ───────────────────────────────────────────

    public String backpackId() {
        return backpackId;
    }

    public Layout layout() {
        return layout;
    }

    // ── Write-through container over the live stack's CONTAINER component ────

    private static class BackpackStackContainer implements Container {

        private final ItemStack backpack;
        private final BackpackItem item;
        private final NonNullList<ItemStack> items;

        BackpackStackContainer(ItemStack backpack, BackpackConfig config) {
            this.backpack = backpack;
            this.item = backpack.getItem() instanceof BackpackItem bp ? bp : null;
            this.items = NonNullList.withSize(config.slots(), ItemStack.EMPTY);
            if (item != null) {
                List<ItemStack> current = item.getContents(backpack);
                for (int i = 0; i < items.size() && i < current.size(); i++) {
                    items.set(i, current.get(i));
                }
            }
        }

        @Override
        public int getContainerSize() { return items.size(); }

        @Override
        public boolean isEmpty() {
            for (ItemStack s : items) if (!s.isEmpty()) return false;
            return true;
        }

        @Override
        public ItemStack getItem(int index) {
            if (index < 0 || index >= items.size()) return ItemStack.EMPTY;
            return items.get(index);
        }

        @Override
        public ItemStack removeItem(int index, int amount) {
            if (index < 0 || index >= items.size()) return ItemStack.EMPTY;
            ItemStack stack = items.get(index);
            if (stack.isEmpty()) return ItemStack.EMPTY;
            ItemStack result;
            if (amount >= stack.getCount()) {
                result = stack.copy();
                items.set(index, ItemStack.EMPTY);
            } else {
                result = stack.split(amount);
                items.set(index, stack);
            }
            setChanged();
            return result;
        }

        @Override
        public ItemStack removeItemNoUpdate(int index) {
            if (index < 0 || index >= items.size()) return ItemStack.EMPTY;
            ItemStack stack = items.get(index);
            items.set(index, ItemStack.EMPTY);
            return stack;
        }

        @Override
        public void setItem(int index, ItemStack stack) {
            if (index < 0 || index >= items.size()) return;
            items.set(index, stack);
            setChanged();
        }

        @Override
        public void setChanged() {
            if (item != null) {
                item.writeContents(backpack, items);
            }
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public void clearContent() {
            for (int i = 0; i < items.size(); i++) {
                items.set(i, ItemStack.EMPTY);
            }
            setChanged();
        }
    }

    // ── Special (category-filtered) slot ─────────────────────────────────────

    /** General bag slot: anything except another backpack (no nesting). */
    private static class GeneralSlot extends Slot {

        GeneralSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return !(stack.getItem() instanceof BackpackItem);
        }
    }

    private static class SpecialSlot extends Slot {

        private final String category;

        SpecialSlot(Container container, int index, int x, int y, String category) {
            super(container, index, x, y);
            this.category = category;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return !(stack.getItem() instanceof BackpackItem)
                    && ToolProfileRegistry.matchesCategory(stack, category);
        }
    }

    // ── Menu plumbing ────────────────────────────────────────────────────────

    @Override
    public boolean stillValid(Player player) {
        // Identity guard: the menu is only valid while the EXACT stack it was
        // opened with is still where it was opened from. Swap the equipped bag,
        // drop the held bag or move it, and the menu closes instead of writing
        // edits into a detached stack.
        ItemStack current = bind == BIND_HAND
                ? player.getItemInHand(hand)
                : BackpackHandler.getEquipped(player);
        return current == bound && !current.isEmpty();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            result = slotStack.copy();

            if (slotIndex < bagSlots) {
                // bag slot -> hotbar (special slots move out freely)
                if (!this.moveItemStackTo(slotStack, bagSlots, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // hotbar -> bag: matching special slot first, then general
                boolean moved = false;
                for (int idx : specialSlotIndices) {
                    if (this.moveItemStackTo(slotStack, idx, idx + 1, false)) {
                        moved = true;
                        break;
                    }
                }
                if (!moved) {
                    if (!this.moveItemStackTo(slotStack, 0, generalSlots, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }
}
