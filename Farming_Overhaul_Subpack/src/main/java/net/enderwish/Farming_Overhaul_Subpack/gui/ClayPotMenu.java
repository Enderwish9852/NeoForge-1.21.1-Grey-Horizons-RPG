package net.enderwish.Farming_Overhaul_Subpack.gui;

import net.enderwish.Farming_Overhaul_Subpack.block.clay_pot.ClayPotBlockEntity;
import net.enderwish.Farming_Overhaul_Subpack.core.crop.CropRegistry;
import net.enderwish.Farming_Overhaul_Subpack.core.food.FoodRegistry;
import net.enderwish.Farming_Overhaul_Subpack.init.ModMenuTypes;
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

public class ClayPotMenu extends AbstractContainerMenu {

    // ── ContainerData indices ─────────────────────────────────────────────────
    public static final int DATA_COOK_PROGRESS   = 0;
    public static final int DATA_COOK_TOTAL_TIME = 1;
    public static final int DATA_BOWLS_REMAINING = 2;
    public static final int DATA_COUNT           = 3;

    // ── Slot positions matching texture ───────────────────────────────────────
    public static final int GRID_X       = 41;
    public static final int GRID_Y       = 40;
    public static final int WATER_SLOT_X = 18;
    public static final int WATER_SLOT_Y = 76;

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
                        new net.minecraft.world.SimpleContainer(ClayPotBlockEntity.TOTAL_SLOTS) {
                            @Override
                            public ItemStack getItem(int slot) {
                                return blockEntity.getItem(slotIndex);
                            }
                            @Override
                            public void setItem(int slot, ItemStack stack) {
                                blockEntity.setItem(slotIndex, stack);
                            }
                            @Override
                            public int getContainerSize() {
                                return ClayPotBlockEntity.TOTAL_SLOTS;
                            }
                            @Override
                            public boolean isEmpty() {
                                return blockEntity.getItem(slotIndex).isEmpty();
                            }
                            @Override
                            public ItemStack removeItem(int slot, int amount) {
                                ItemStack stack = blockEntity.getItem(slotIndex);
                                if (stack.isEmpty()) return ItemStack.EMPTY;
                                ItemStack result;
                                if (amount >= stack.getCount()) {
                                    result = stack.copy();
                                    blockEntity.setItem(slotIndex, ItemStack.EMPTY);
                                } else {
                                    result = stack.split(amount);
                                    blockEntity.setItem(slotIndex, stack);
                                }
                                return result;
                            }
                            @Override
                            public ItemStack removeItemNoUpdate(int slot) {
                                ItemStack stack = blockEntity.getItem(slotIndex);
                                blockEntity.setItem(slotIndex, ItemStack.EMPTY);
                                return stack;
                            }
                            @Override
                            public void setChanged() { blockEntity.setChanged(); }
                            @Override
                            public boolean stillValid(Player player) { return true; }
                            @Override
                            public void clearContent() {
                                for (int i = 0; i < ClayPotBlockEntity.INGREDIENT_SLOTS; i++) {
                                    blockEntity.setItem(i, ItemStack.EMPTY);
                                }
                            }
                        },
                        slotIndex,
                        GRID_X + col * 18,
                        GRID_Y + row * 18
                ) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return stack.getItem().getFoodProperties(stack, null) != null
                                || FoodRegistry.INSTANCE.isRegistered(
                                stack.getItem().builtInRegistryHolder()
                                        .key().location().getPath())
                                || CropRegistry.INSTANCE.isRegistered(
                                stack.getItem().builtInRegistryHolder()
                                        .key().location().getPath());
                    }
                });
            }
        }

        // ── Water slot ────────────────────────────────────────────────────────
        this.addSlot(new Slot(
                new net.minecraft.world.SimpleContainer(ClayPotBlockEntity.TOTAL_SLOTS) {
                    @Override
                    public ItemStack getItem(int slot) {
                        return blockEntity.getItem(ClayPotBlockEntity.WATER_SLOT);
                    }
                    @Override
                    public void setItem(int slot, ItemStack stack) {
                        blockEntity.setItem(ClayPotBlockEntity.WATER_SLOT, stack);
                    }
                    @Override
                    public int getContainerSize() { return ClayPotBlockEntity.TOTAL_SLOTS; }
                    @Override
                    public boolean isEmpty() {
                        return blockEntity.getItem(ClayPotBlockEntity.WATER_SLOT).isEmpty();
                    }
                    @Override
                    public ItemStack removeItem(int slot, int amount) {
                        ItemStack stack = blockEntity.getItem(ClayPotBlockEntity.WATER_SLOT);
                        if (stack.isEmpty()) return ItemStack.EMPTY;
                        ItemStack result;
                        if (amount >= stack.getCount()) {
                            result = stack.copy();
                            blockEntity.setItem(ClayPotBlockEntity.WATER_SLOT, ItemStack.EMPTY);
                        } else {
                            result = stack.split(amount);
                            blockEntity.setItem(ClayPotBlockEntity.WATER_SLOT, stack);
                        }
                        return result;
                    }
                    @Override
                    public ItemStack removeItemNoUpdate(int slot) {
                        ItemStack stack = blockEntity.getItem(ClayPotBlockEntity.WATER_SLOT);
                        blockEntity.setItem(ClayPotBlockEntity.WATER_SLOT, ItemStack.EMPTY);
                        return stack;
                    }
                    @Override
                    public void setChanged() { blockEntity.setChanged(); }
                    @Override
                    public boolean stillValid(Player player) { return true; }
                    @Override
                    public void clearContent() {
                        blockEntity.setItem(ClayPotBlockEntity.WATER_SLOT, ItemStack.EMPTY);
                    }
                },
                ClayPotBlockEntity.WATER_SLOT,
                WATER_SLOT_X,
                WATER_SLOT_Y
        ) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.WATER_BUCKET);
            }
        });

        // ── Player inventory (3 rows) ─────────────────────────────────────────
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(
                        playerInventory,
                        col + row * 9 + 9,
                        8 + col * 18,
                        140 + row * 18
                ));
            }
        }

        // ── Player hotbar ─────────────────────────────────────────────────────
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(
                    playerInventory,
                    col,
                    8 + col * 18,
                    198
            ));
        }

        addDataSlots(data);
    }

    // ── Constructor (client side) ─────────────────────────────────────────────
    public ClayPotMenu(int containerId, Inventory playerInventory,
                       RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory,
                getBlockEntity(playerInventory, extraData),
                new SimpleContainerData(DATA_COUNT));
    }

    private static ClayPotBlockEntity getBlockEntity(Inventory playerInventory,
                                                     RegistryFriendlyByteBuf buf) {
        var pos = buf.readBlockPos();
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        if (be instanceof ClayPotBlockEntity clay) return clay;
        throw new IllegalStateException("No ClayPotBlockEntity at " + pos);
    }

    // ── Sync data getters ─────────────────────────────────────────────────────
    public int getCookProgress()   { return data.get(DATA_COOK_PROGRESS); }
    public int getCookTotalTime()  { return data.get(DATA_COOK_TOTAL_TIME); }
    public int getBowlsRemaining() { return data.get(DATA_BOWLS_REMAINING); }
    public ClayPotBlockEntity getBlockEntity() { return blockEntity; }

    @Override
    public boolean stillValid(Player player) {
        return !blockEntity.isRemoved() &&
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

            if (slotIndex < 10) {
                if (!this.moveItemStackTo(slotStack, 10, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Try water slot first for water buckets
                if (slotStack.is(Items.WATER_BUCKET)) {
                    if (!this.moveItemStackTo(slotStack, 9, 10, false)) {
                        if (!this.moveItemStackTo(slotStack, 0, 9, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                } else {
                    if (!this.moveItemStackTo(slotStack, 0, 9, false)) {
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
