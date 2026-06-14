package net.enderwish.Farming_Overhaul_Subpack.block.cutting_board;

import net.enderwish.Farming_Overhaul_Subpack.core.cutting_board.CuttingBoardRecipeRegistry;
import net.enderwish.Farming_Overhaul_Subpack.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * CuttingBoardBlockEntity
 *
 * Slots:
 *   0-8  = ingredient grid (3×3)
 *   9    = tool slot (knife / cleaver)
 *   10   = container slot (bowl / bundle)
 *   11   = primary output slot
 *   12   = secondary output slot (peels, seeds)
 *
 * Progress only advances while activePlayers is non-empty — exploit proof.
 * activePlayers is NOT saved to NBT intentionally.
 *
 * Cross-tool penalty:
 *   Primary output count reduced by 1 (min 1)
 *   Secondary output: always secondary_min (not random max)
 *   Tool durability damage doubled
 *   Knife cross-using cleaver recipe: chop time increased 50%
 */
public class CuttingBoardBlockEntity extends BlockEntity {

    // ── Constants ─────────────────────────────────────────────────────────────
    public static final int INGREDIENT_SLOTS      = 9;
    public static final int TOOL_SLOT             = 9;
    public static final int CONTAINER_SLOT        = 10;
    public static final int PRIMARY_OUTPUT_SLOT   = 11;
    public static final int SECONDARY_OUTPUT_SLOT = 12;
    public static final int TOTAL_SLOTS           = 13;

    // ── State ─────────────────────────────────────────────────────────────────
    private final NonNullList<ItemStack> items =
            NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);

    private int chopProgress  = 0;
    private int chopTotalTime = 0;

    private ItemStack primaryOutputItem   = ItemStack.EMPTY;
    private ItemStack secondaryOutputItem = ItemStack.EMPTY;

    private float currentRecipeSpoilReduction = 0.0f;
    private boolean isCrossTool = false;

    // ── Active players — exploit-proof GUI tracking ───────────────────────────
    private final Set<UUID> activePlayers = new HashSet<>();

    // ── Constructor ───────────────────────────────────────────────────────────
    public CuttingBoardBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CUTTING_BOARD.get(), pos, state);
    }

    // ── Player tracking ───────────────────────────────────────────────────────

    public void addActivePlayer(Player player) {
        activePlayers.add(player.getUUID());
        setChanged();
    }

    public void removeActivePlayer(Player player) {
        activePlayers.remove(player.getUUID());
        setChanged();
    }

    public boolean hasActivePlayers() {
        return !activePlayers.isEmpty();
    }

    // ── Tick ──────────────────────────────────────────────────────────────────

    public static void tick(Level level, BlockPos pos, BlockState state,
                            CuttingBoardBlockEntity entity) {
        if (level.isClientSide()) return;
        if (!entity.hasActivePlayers()) return;

        // Wait for player to collect BOTH outputs before starting next recipe
        if (!entity.items.get(PRIMARY_OUTPUT_SLOT).isEmpty() ||
                !entity.items.get(SECONDARY_OUTPUT_SLOT).isEmpty()) return;

        // No active chop — search for matching recipe
        if (entity.chopTotalTime <= 0) {
            List<ItemStack> grid      = entity.items.subList(0, INGREDIENT_SLOTS);
            ItemStack       tool      = entity.items.get(TOOL_SLOT);
            ItemStack       container = entity.items.get(CONTAINER_SLOT);

            CuttingBoardRecipeRegistry.INSTANCE
                    .findMatch(grid, tool, container)
                    .ifPresent(match -> {
                        entity.isCrossTool = match.isCrossTool();

                        // Knife cross-using cleaver recipe: 50% longer chop time
                        int time = match.recipe().chopTimeTicks();
                        if (entity.isCrossTool) time = (int) (time * 1.5f);
                        entity.chopTotalTime = time;
                        entity.chopProgress  = 0;

                        entity.currentRecipeSpoilReduction =
                                match.recipe().spoilReduction();

                        // Primary output — cross-tool loses 1 item (min 1)
                        ItemStack primary = match.recipe().getOutput();
                        if (entity.isCrossTool) {
                            primary = primary.copyWithCount(
                                    Math.max(1, primary.getCount() - 1));
                        }
                        entity.primaryOutputItem = primary;

                        // Secondary output — knife gets random min-max
                        // cleaver cross-tool always gets min via getSecondaryOutput
                        entity.secondaryOutputItem = match.recipe()
                                .getSecondaryOutput(
                                        entity.isCrossTool,
                                        level.getRandom());

                        entity.setChanged();
                    });
            return;
        }

        // Advance chop progress
        entity.chopProgress++;
        entity.setChanged();

        if (entity.chopProgress >= entity.chopTotalTime) {

            // ── Place outputs ─────────────────────────────────────────────────
            entity.items.set(PRIMARY_OUTPUT_SLOT,
                    entity.primaryOutputItem.copy());

            if (!entity.secondaryOutputItem.isEmpty()) {
                entity.items.set(SECONDARY_OUTPUT_SLOT,
                        entity.secondaryOutputItem.copy());
            }

            // ── Damage tool ───────────────────────────────────────────────────
            ItemStack tool = entity.items.get(TOOL_SLOT);
            if (!tool.isEmpty() &&
                    level instanceof net.minecraft.server.level.ServerLevel sl) {
                int damage = entity.isCrossTool ? 2 : 1;
                tool.hurtAndBreak(damage, sl, null,
                        item -> entity.items.set(TOOL_SLOT, ItemStack.EMPTY));
                entity.items.set(TOOL_SLOT, tool);
            }

            // ── Clear ingredient grid ─────────────────────────────────────────
            for (int i = 0; i < INGREDIENT_SLOTS; i++) {
                entity.items.set(i, ItemStack.EMPTY);
            }

            // ── Reset state ───────────────────────────────────────────────────
            entity.chopProgress        = 0;
            entity.chopTotalTime       = 0;
            entity.primaryOutputItem   = ItemStack.EMPTY;
            entity.secondaryOutputItem = ItemStack.EMPTY;
            entity.isCrossTool         = false;
            entity.setChanged();
        }
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public NonNullList<ItemStack> getItems()       { return items; }
    public int getChopProgress()                   { return chopProgress; }
    public int getChopTotalTime()                  { return chopTotalTime; }
    public ItemStack getItem(int slot)             { return items.get(slot); }
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        setChanged();
    }

    // ── Save / Load ───────────────────────────────────────────────────────────

    @Override
    protected void saveAdditional(CompoundTag tag,
                                  HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        ContainerHelper.saveAllItems(tag, items, provider);
        tag.putInt("ChopProgress",  chopProgress);
        tag.putInt("ChopTotalTime", chopTotalTime);
        tag.putFloat("SpoilReduction", currentRecipeSpoilReduction);
        tag.putBoolean("IsCrossTool", isCrossTool);
        if (!primaryOutputItem.isEmpty())
            tag.put("PrimaryOutputItem", primaryOutputItem.save(provider));
        if (!secondaryOutputItem.isEmpty())
            tag.put("SecondaryOutputItem", secondaryOutputItem.save(provider));
    }

    @Override
    public void loadAdditional(CompoundTag tag,
                               HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        ContainerHelper.loadAllItems(tag, items, provider);
        chopProgress  = tag.getInt("ChopProgress");
        chopTotalTime = tag.getInt("ChopTotalTime");
        currentRecipeSpoilReduction = tag.getFloat("SpoilReduction");
        isCrossTool   = tag.getBoolean("IsCrossTool");
        if (tag.contains("PrimaryOutputItem"))
            primaryOutputItem = ItemStack.parse(provider,
                            tag.getCompound("PrimaryOutputItem"))
                    .orElse(ItemStack.EMPTY);
        if (tag.contains("SecondaryOutputItem"))
            secondaryOutputItem = ItemStack.parse(provider,
                            tag.getCompound("SecondaryOutputItem"))
                    .orElse(ItemStack.EMPTY);
        // activePlayers intentionally NOT saved — prevents exploits on restart
    }
}
