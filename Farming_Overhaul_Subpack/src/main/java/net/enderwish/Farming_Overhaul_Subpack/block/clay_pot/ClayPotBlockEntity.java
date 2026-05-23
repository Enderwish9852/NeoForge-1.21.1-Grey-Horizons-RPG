package net.enderwish.Farming_Overhaul_Subpack.block.clay_pot;

import net.enderwish.Farming_Overhaul_Subpack.core.claypot.ClayPotRecipeRegistry;
import net.enderwish.Farming_Overhaul_Subpack.core.crop.CropRegistry;
import net.enderwish.Farming_Overhaul_Subpack.core.food.FoodRegistry;
import net.enderwish.Farming_Overhaul_Subpack.core.spoilage.ModDataComponents;
import net.enderwish.Farming_Overhaul_Subpack.core.spoilage.SpoilageComponent;
import net.enderwish.Farming_Overhaul_Subpack.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * ClayPotBlockEntity
 *
 * Stores all state for the clay pot block:
 *   - 9 ingredient slots (3x3 grid)
 *   - 1 water slot (bucket input)
 *   - Cook progress and total cook time
 *   - Water level (0 = empty, 1 = full)
 *   - Output item and remaining bowl count
 *   - Campfire lit state
 *   - Current recipe spoil reduction and water requirement
 */
public class ClayPotBlockEntity extends BlockEntity {

    // ── Constants ─────────────────────────────────────────────────────────────

    public static final int INGREDIENT_SLOTS = 9;
    public static final int WATER_SLOT       = 9;
    public static final int TOTAL_SLOTS      = 10;
    public static final int BOWLS_PER_BATCH  = 9;

    // ── State ─────────────────────────────────────────────────────────────────

    private final NonNullList<ItemStack> items =
            NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);

    private int cookProgress   = 0;
    private int cookTotalTime  = 0;
    private int waterLevel     = 0;
    private int bowlsRemaining = 0;
    private ItemStack outputItem = ItemStack.EMPTY;

    private float currentRecipeSpoilReduction  = 0.0f;
    private boolean currentRecipeRequiresWater = false;

    // ── Constructor ───────────────────────────────────────────────────────────

    public ClayPotBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CLAY_POT.get(), pos, state);
    }

    // ── Tick ──────────────────────────────────────────────────────────────────

    public static void tick(Level level, BlockPos pos, BlockState state,
                            ClayPotBlockEntity entity) {
        if (level.isClientSide()) return;

        // If output is ready — wait for player to collect with bowl
        if (entity.bowlsRemaining > 0) return;

        // Check if campfire below is lit
        if (!isCampfireLit(level, pos)) return;

        // If no active cook time — check for a matching recipe
        if (entity.cookTotalTime <= 0) {
            List<ItemStack> grid = entity.items.subList(0, INGREDIENT_SLOTS);
            ClayPotRecipeRegistry.INSTANCE.findMatch(grid, entity.waterLevel)
                    .ifPresent(recipe -> {
                        entity.cookTotalTime               = recipe.cookTimeTicks();
                        entity.cookProgress                = 0;
                        entity.outputItem                  = recipe.getOutput();
                        entity.currentRecipeSpoilReduction = recipe.spoilReduction();
                        entity.currentRecipeRequiresWater  = recipe.requiresWater();
                        entity.setChanged();
                    });
            return;
        }

        // Advance cook progress
        entity.cookProgress++;
        entity.setChanged();

        if (entity.cookProgress >= entity.cookTotalTime) {
            // Cooking done — calculate spoilage for output
            float worstSpoilProgress = getWorstSpoilProgress(entity);
            float finalProgress = Math.max(0.0f,
                    worstSpoilProgress - entity.currentRecipeSpoilReduction);

            // Attach spoilage to output item
            attachSpoilage(entity.outputItem, finalProgress);

            entity.cookProgress   = 0;
            entity.cookTotalTime  = 0;
            entity.bowlsRemaining = BOWLS_PER_BATCH;

            // Consume water if recipe required it
            if (entity.currentRecipeRequiresWater) {
                entity.waterLevel = 0;
            }

            entity.setChanged();
        }
    }

    // ── Campfire check ────────────────────────────────────────────────────────

    public static boolean isCampfireLit(Level level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        if (!below.is(Blocks.CAMPFIRE) && !below.is(Blocks.SOUL_CAMPFIRE)) return false;
        return below.getValue(CampfireBlock.LIT);
    }

    // ── Bowl collection ───────────────────────────────────────────────────────

    public ItemStack collectBowl() {
        if (bowlsRemaining <= 0 || outputItem.isEmpty()) return ItemStack.EMPTY;

        ItemStack bowl = outputItem.copy();
        bowlsRemaining--;
        setChanged();

        if (bowlsRemaining <= 0) {
            outputItem = ItemStack.EMPTY;
            for (int i = 0; i < INGREDIENT_SLOTS; i++) {
                items.set(i, ItemStack.EMPTY);
            }
        }

        return bowl;
    }

    // ── Water ─────────────────────────────────────────────────────────────────

    public boolean fillWater() {
        if (waterLevel >= 1) return false;
        waterLevel = 1;
        setChanged();
        return true;
    }

    // ── Spoilage helpers ──────────────────────────────────────────────────────

    private static float getWorstSpoilProgress(ClayPotBlockEntity entity) {
        float worst = 0.0f;
        for (int i = 0; i < INGREDIENT_SLOTS; i++) {
            ItemStack stack = entity.items.get(i);
            if (stack.isEmpty()) continue;
            SpoilageComponent comp = stack.get(ModDataComponents.SPOILAGE.get());
            if (comp == null) continue;
            if (comp.getProgress() > worst) worst = comp.getProgress();
        }
        return worst;
    }

    private static void attachSpoilage(ItemStack output, float inheritedProgress) {
        if (output.isEmpty()) return;

        String itemId = output.getItem()
                .builtInRegistryHolder()
                .key().location().getPath();

        int maxTicks = 0;
        if (FoodRegistry.INSTANCE.isRegistered(itemId)) {
            maxTicks = FoodRegistry.INSTANCE.getByName(itemId).spoilTicks();
        } else if (CropRegistry.INSTANCE.isRegistered(itemId)) {
            maxTicks = CropRegistry.INSTANCE.getByName(itemId).spoilTicks();
        }

        if (maxTicks <= 0) return;

        int startTicks = (int) (inheritedProgress * maxTicks);
        output.set(
                ModDataComponents.SPOILAGE.get(),
                new SpoilageComponent(startTicks, maxTicks, 1.0f)
        );
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public NonNullList<ItemStack> getItems()    { return items; }
    public int getCookProgress()                { return cookProgress; }
    public int getCookTotalTime()               { return cookTotalTime; }
    public int getWaterLevel()                  { return waterLevel; }
    public int getBowlsRemaining()              { return bowlsRemaining; }
    public ItemStack getOutputItem()            { return outputItem; }
    public ItemStack getItem(int slot)          { return items.get(slot); }

    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        setChanged();
    }

    public void startCooking(int totalTime, ItemStack output) {
        this.cookTotalTime = totalTime;
        this.cookProgress  = 0;
        this.outputItem    = output;
        setChanged();
    }

    // ── Save / Load ───────────────────────────────────────────────────────────

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        ContainerHelper.saveAllItems(tag, items, provider);
        tag.putInt("CookProgress",    cookProgress);
        tag.putInt("CookTotalTime",   cookTotalTime);
        tag.putInt("WaterLevel",      waterLevel);
        tag.putInt("BowlsRemaining",  bowlsRemaining);
        tag.putFloat("SpoilReduction", currentRecipeSpoilReduction);
        tag.putBoolean("RequiresWater", currentRecipeRequiresWater);
        if (!outputItem.isEmpty()) {
            tag.put("OutputItem", outputItem.save(provider));
        }
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        ContainerHelper.loadAllItems(tag, items, provider);
        cookProgress                = tag.getInt("CookProgress");
        cookTotalTime               = tag.getInt("CookTotalTime");
        waterLevel                  = tag.getInt("WaterLevel");
        bowlsRemaining              = tag.getInt("BowlsRemaining");
        currentRecipeSpoilReduction = tag.getFloat("SpoilReduction");
        currentRecipeRequiresWater  = tag.getBoolean("RequiresWater");
        if (tag.contains("OutputItem")) {
            outputItem = ItemStack.parse(provider, tag.getCompound("OutputItem"))
                    .orElse(ItemStack.EMPTY);
        }
    }
}