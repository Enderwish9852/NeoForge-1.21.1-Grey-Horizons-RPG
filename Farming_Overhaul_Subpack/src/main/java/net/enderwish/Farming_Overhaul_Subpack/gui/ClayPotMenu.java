package net.enderwish.Farming_Overhaul_Subpack.gui;

import net.enderwish.Farming_Overhaul_Subpack.block.clay_pot.ClayPotBlockEntity;
import net.enderwish.Farming_Overhaul_Subpack.init.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * ClayPotMenu
 *
 * Container for the clay pot GUI.
 *
 * Slot layout:
 *   0-8   = ingredient grid (3x3)
 *   9     = water slot
 *   10-36 = player inventory (3 rows of 9)
 *   37-45 = player hotbar
 *
 * ContainerData (synced int values):
 *   0 = cookProgress
 *   1 = cookTotalTime
 *   2 = waterLevel
 *   3 = bowlsRemaining
 */
public class ClayPotMenu extends AbstractContainerMenu {

    // ── ContainerData indices ─────────────────────────────────────────────────

    public static final int DATA_COOK_PROGRESS   = 0;
    public static final int DATA_COOK_TOTAL_TIME = 1;
    public static final int DATA_WATER_LEVEL     = 2;
    public static final int DATA_BOWLS_REMAINING = 3;
    public static final int DATA_COUNT           = 4;

    // ── Slot positions (for screen rendering) ─────────────────────────────────

    // Ingredient grid top-left corner in GUI pixels
    public static final int GRID_X = 62;
    public static final int GRID_Y = 17;

    // Water slot position
    public static final int WATER_SLOT_X = 26;
    public static final int WATER_SLOT_Y = 35;

    // ── Fields ────────────────────────────────────────────────────────────────

    private final ClayPotBlockEntity blockEntity;
    private final ContainerData data;

    // ── Constructor (server side) ─────────────────────────────────────────────

    public ClayPotMenu(int containerId, Inventory playerInventory,
                       ClayPotBlockEntity blockEntity, ContainerData data) {
        super(ModMenuTypes.CLAY_POT.get(), containerId);
        this.blockEntity = blockEntity;
        this.data = data;

        checkContainerDataCount(data, DATA_COUNT);

        // ── Ingredient slots (3x3 grid) ───────────────────────────────────────
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slotIndex = row * 3 + col;
                this.addSlot(new Slot(
                        new net.minecraft.world.SimpleContainer(
                                ClayPotBlockEntity.TOTAL_SLOTS) {
                            @Override
                            public ItemStack getItem(int slot) {
                                return blockEntity.getItem(slot);
                            }
                            @Override
                            public void setItem(int slot, ItemStack stack) {
                                blockEntity.setItem(slot, stack);
                            }
                            @Override
                            public int getContainerSize() {
                                return ClayPotBlockEntity.TOTAL_SLOTS;
                            }
                            @Override
                            public boolean isEmpty() {
                                return false;
                            }
                            @Override
                            public ItemStack removeItem(int slot, int amount) {
                                return net.minecraft.world.item.ItemStack.EMPTY;
                            }
                            @Override
                            public ItemStack removeItemNoUpdate(int slot) {
                                return net.minecraft.world.item.ItemStack.EMPTY;
                            }
                            @Override
                            public void setChanged() {
                                blockEntity.setChanged();
                            }
                            @Override
                            public boolean stillValid(Player player) {
                                return true;
                            }
                            @Override
                            public void clearContent() {}
                        },
                        slotIndex,
                        GRID_X + col * 18,
                        GRID_Y + row * 18
                ));
            }
        }

        // ── Player inventory (3 rows) ─────────────────────────────────────────
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(
                        playerInventory,
                        col + row * 9 + 9,
                        8 + col * 18,
                        84 + row * 18
                ));
            }
        }

        // ── Player hotbar ─────────────────────────────────────────────────────
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(
                    playerInventory,
                    col,
                    8 + col * 18,
                    142
            ));
        }

        addDataSlots(data);
    }

    // ── Constructor (client side — called via network) ────────────────────────

    public ClayPotMenu(int containerId, Inventory playerInventory,
                       FriendlyByteBuf extraData) {
        this(containerId, playerInventory,
                getBlockEntity(playerInventory, extraData),
                new SimpleContainerData(DATA_COUNT));
    }

    private static ClayPotBlockEntity getBlockEntity(Inventory playerInventory,
                                                     FriendlyByteBuf buf) {
        var pos = buf.readBlockPos();
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        if (be instanceof ClayPotBlockEntity clay) return clay;
        throw new IllegalStateException("No ClayPotBlockEntity at " + pos);
    }

    // ── Sync data getters (read by screen) ────────────────────────────────────

    public int getCookProgress()   { return data.get(DATA_COOK_PROGRESS); }
    public int getCookTotalTime()  { return data.get(DATA_COOK_TOTAL_TIME); }
    public int getWaterLevel()     { return data.get(DATA_WATER_LEVEL); }
    public int getBowlsRemaining() { return data.get(DATA_BOWLS_REMAINING); }

    public ClayPotBlockEntity getBlockEntity() { return blockEntity; }

    // ── Required overrides ────────────────────────────────────────────────────

    @Override
    public boolean stillValid(Player player) {
        return blockEntity.isRemoved() ? false :
                player.distanceToSqr(
                        blockEntity.getBlockPos().getX() + 0.5,
                        blockEntity.getBlockPos().getY() + 0.5,
                        blockEntity.getBlockPos().getZ() + 0.5
                ) <= 64.0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);

        if (slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            result = slotStack.copy();

            // From ingredient grid → player inventory
            if (slotIndex < 9) {
                if (!this.moveItemStackTo(slotStack, 9, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            }
            // From player inventory → ingredient grid
            else {
                if (!this.moveItemStackTo(slotStack, 0, 9, false)) {
                    return ItemStack.EMPTY;
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
