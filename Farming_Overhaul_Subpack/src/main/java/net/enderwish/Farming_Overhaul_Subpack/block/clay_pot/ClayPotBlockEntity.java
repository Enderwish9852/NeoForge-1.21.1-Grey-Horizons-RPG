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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

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

        if (entity.bowlsRemaining > 0) return;

        if (!isCampfireLit(level, pos)) return;

        if (entity.cookTotalTime <= 0) {
            List<ItemStack> grid = entity.items.subList(0, INGREDIENT_SLOTS);
            ItemStack waterSlot = entity.items.get(WATER_SLOT);
            ClayPotRecipeRegistry.INSTANCE.findMatch(grid, waterSlot)
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

        entity.cookProgress++;
        entity.setChanged();

        if (entity.cookProgress >= entity.cookTotalTime) {
            float worstSpoilProgress = getWorstSpoilProgress(entity);
            float finalProgress = Math.max(0.0f,
                    worstSpoilProgress - entity.currentRecipeSpoilReduction);

            attachSpoilage(entity.outputItem, finalProgress);

            entity.cookProgress   = 0;
            entity.cookTotalTime  = 0;
            entity.bowlsRemaining = BOWLS_PER_BATCH;

            // Convert water bucket to empty bucket
            if (entity.currentRecipeRequiresWater) {
                entity.items.set(WATER_SLOT, new ItemStack(Items.BUCKET));
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
        bowlsRemaining              = tag.getInt("BowlsRemaining");
        currentRecipeSpoilReduction = tag.getFloat("SpoilReduction");
        currentRecipeRequiresWater  = tag.getBoolean("RequiresWater");
        if (tag.contains("OutputItem")) {
            outputItem = ItemStack.parse(provider, tag.getCompound("OutputItem"))
                    .orElse(ItemStack.EMPTY);
        }
    }
}