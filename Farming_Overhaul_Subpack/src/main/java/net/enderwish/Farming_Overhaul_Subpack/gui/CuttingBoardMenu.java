package net.enderwish.Farming_Overhaul_Subpack.gui;

import net.enderwish.Farming_Overhaul_Subpack.block.cutting_board.CuttingBoardBlockEntity;
import net.enderwish.Farming_Overhaul_Subpack.init.ModMenuTypes;
import net.enderwish.Farming_Overhaul_Subpack.item.ModItems;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;

public class CuttingBoardMenu extends AbstractContainerMenu {

    // ── ContainerData ─────────────────────────────────────────────────────────
    public static final int DATA_CHOP_PROGRESS   = 0;
    public static final int DATA_CHOP_TOTAL_TIME = 1;
    public static final int DATA_COUNT           = 2;

    // ── Slot screen positions ─────────────────────────────────────────────────
    public static final int GRID_X                  = 52;
    public static final int GRID_Y                  = 44;
    public static final int TOOL_SLOT_X             = 32;
    public static final int TOOL_SLOT_Y             = 62;
    public static final int CONTAINER_SLOT_X        = 32;
    public static final int CONTAINER_SLOT_Y        = 80;
    public static final int PRIMARY_OUTPUT_SLOT_X   = 141;
    public static final int PRIMARY_OUTPUT_SLOT_Y   = 53;
    public static final int SECONDARY_OUTPUT_SLOT_X = 141;
    public static final int SECONDARY_OUTPUT_SLOT_Y = 71;

    // ── Fields ────────────────────────────────────────────────────────────────
    private final CuttingBoardBlockEntity blockEntity;
    private final ContainerData data;
    private final Player player;

    // ── Server-side constructor ───────────────────────────────────────────────
    public CuttingBoardMenu(int containerId, Inventory playerInventory,
                            CuttingBoardBlockEntity blockEntity,
                            ContainerData data) {
        super(ModMenuTypes.CUTTING_BOARD.get(), containerId);
        this.blockEntity = blockEntity;
        this.data        = data;
        this.player      = playerInventory.player;

        checkContainerDataCount(data, DATA_COUNT);

        // ── Ingredient grid (slots 0-8) ───────────────────────────────────────
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                final int slotIndex = row * 3 + col;
                addSlot(makeProxySlot(slotIndex,
                        GRID_X + col * 18,
                        GRID_Y + row * 18));
            }
        }

        // ── Tool slot (slot 9) — knife and cleaver only ───────────────────────
        addSlot(new ProxySlot(blockEntity,
                CuttingBoardBlockEntity.TOOL_SLOT,
                TOOL_SLOT_X, TOOL_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                String id = net.minecraft.core.registries.BuiltInRegistries
                        .ITEM.getKey(stack.getItem()).toString();
                return id.contains("knife") || id.contains("cleaver");
            }
        });

        // ── Container slot (slot 10) — bowl or bundle ─────────────────────────
        addSlot(new ProxySlot(blockEntity,
                CuttingBoardBlockEntity.CONTAINER_SLOT,
                CONTAINER_SLOT_X, CONTAINER_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.BOWL)
                        || stack.is(ModItems.BUNDLE.get());
            }
        });

        // ── Primary output (slot 11) — take only ─────────────────────────────
        addSlot(new ProxySlot(blockEntity,
                CuttingBoardBlockEntity.PRIMARY_OUTPUT_SLOT,
                PRIMARY_OUTPUT_SLOT_X, PRIMARY_OUTPUT_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) { return false; }
        });

        // ── Secondary output (slot 12) — take only ────────────────────────────
        addSlot(new ProxySlot(blockEntity,
                CuttingBoardBlockEntity.SECONDARY_OUTPUT_SLOT,
                SECONDARY_OUTPUT_SLOT_X, SECONDARY_OUTPUT_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) { return false; }
        });

        // ── Player inventory (slots 13-39) ────────────────────────────────────
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory,
                        col + row * 9 + 9,
                        8 + col * 18,
                        140 + row * 18));
            }
        }

        // ── Hotbar (slots 40-48) ──────────────────────────────────────────────
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col,
                    8 + col * 18, 198));
        }

        addDataSlots(data);
        blockEntity.addActivePlayer(player);
    }

    // ── Client-side constructor ───────────────────────────────────────────────
    public CuttingBoardMenu(int containerId, Inventory playerInventory,
                            RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory,
                getBlockEntity(playerInventory, extraData),
                new SimpleContainerData(DATA_COUNT));
    }

    private static CuttingBoardBlockEntity getBlockEntity(
            Inventory inv, RegistryFriendlyByteBuf buf) {
        var pos = buf.readBlockPos();
        BlockEntity be = inv.player.level().getBlockEntity(pos);
        if (be instanceof CuttingBoardBlockEntity cb) return cb;
        throw new IllegalStateException(
                "No CuttingBoardBlockEntity at " + pos);
    }

    // ── Remove player when GUI closes ─────────────────────────────────────────
    @Override
    public void removed(Player player) {
        super.removed(player);
        blockEntity.removeActivePlayer(player);
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public int getChopProgress()  { return data.get(DATA_CHOP_PROGRESS); }
    public int getChopTotalTime() { return data.get(DATA_CHOP_TOTAL_TIME); }
    public CuttingBoardBlockEntity getBlockEntity() { return blockEntity; }

    @Override
    public boolean stillValid(Player player) {
        return !blockEntity.isRemoved() &&
                player.distanceToSqr(
                        blockEntity.getBlockPos().getX() + 0.5,
                        blockEntity.getBlockPos().getY() + 0.5,
                        blockEntity.getBlockPos().getZ() + 0.5) <= 64.0;
    }

    // ── Shift-click ───────────────────────────────────────────────────────────
    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);

        if (slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            result = slotStack.copy();

            // Board slots (0-12) → player inventory
            if (slotIndex < 13) {
                if (!this.moveItemStackTo(slotStack, 13,
                        this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            }
            // Player inventory → board slots
            else {
                String id = net.minecraft.core.registries.BuiltInRegistries
                        .ITEM.getKey(slotStack.getItem()).toString();

                if (id.contains("knife") || id.contains("cleaver")) {
                    // → tool slot only (no peeler)
                    if (!this.moveItemStackTo(slotStack, 9, 10, false))
                        return ItemStack.EMPTY;
                } else if (slotStack.is(Items.BOWL)
                        || slotStack.is(ModItems.BUNDLE.get())) {
                    // → container slot
                    if (!this.moveItemStackTo(slotStack, 10, 11, false))
                        return ItemStack.EMPTY;
                } else {
                    // → ingredient grid
                    if (!this.moveItemStackTo(slotStack, 0, 9, false))
                        return ItemStack.EMPTY;
                }
            }

            if (slotStack.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }

        return result;
    }

    // ── Proxy slot helper ─────────────────────────────────────────────────────
    private ProxySlot makeProxySlot(int entitySlot, int x, int y) {
        return new ProxySlot(blockEntity, entitySlot, x, y);
    }

    private static class ProxySlot extends Slot {

        private final CuttingBoardBlockEntity be;
        private final int beSlot;

        ProxySlot(CuttingBoardBlockEntity be, int beSlot, int x, int y) {
            super(new net.minecraft.world.SimpleContainer(
                    CuttingBoardBlockEntity.TOTAL_SLOTS) {}, beSlot, x, y);
            this.be     = be;
            this.beSlot = beSlot;
        }

        @Override public ItemStack getItem() { return be.getItem(beSlot); }

        @Override
        public void set(ItemStack stack) {
            be.setItem(beSlot, stack);
            setChanged();
        }

        @Override
        public ItemStack remove(int amount) {
            ItemStack stack = be.getItem(beSlot);
            if (stack.isEmpty()) return ItemStack.EMPTY;
            if (amount >= stack.getCount()) {
                be.setItem(beSlot, ItemStack.EMPTY);
                return stack;
            }
            ItemStack result = stack.split(amount);
            be.setItem(beSlot, stack);
            return result;
        }

        @Override public boolean hasItem()              { return !be.getItem(beSlot).isEmpty(); }
        @Override public void setChanged()              { be.setChanged(); }
        public boolean stillValid(Player p)  { return true; }
        @Override public boolean mayPlace(ItemStack s) { return true; }
    }
}
