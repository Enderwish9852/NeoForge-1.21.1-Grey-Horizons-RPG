package net.enderwish.Farming_Overhaul_Subpack.block.composter;

import net.enderwish.Atmospheric_Overhaul_Subpack.api.SeasonsAPI;
import net.enderwish.Atmospheric_Overhaul_Subpack.core.season.SeasonCalendar;
import net.enderwish.Farming_Overhaul_Subpack.core.spoilage.ModDataComponents;
import net.enderwish.Farming_Overhaul_Subpack.core.spoilage.SpoilageComponent;
import net.enderwish.Farming_Overhaul_Subpack.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * GHComposterBlockEntity
 *
 * Stores the composting state for GHComposterBlock.
 *
 * Fields:
 *   fillLevel       - visual fill level 0-8 (mirrors LEVELS blockstate)
 *   compostTimer    - ticks remaining until ready (counts down)
 *   compostTotalTime- total ticks when timer was last set (for progress bar)
 *   ready           - true when fillLevel == 8 AND compostTimer <= 0
 *
 * Timer math:
 *   newBatchTimer = median(remaining spoil ticks of newly added items)
 *                  / (1 + boostFraction)
 *   compostTimer += newBatchTimer
 *   compostTotalTime += newBatchTimer
 *
 * Boost fraction:
 *   1.0 base (always, +100%)
 *   +0.10 if local chunk is hot
 *   +0.50 if season is Summer
 *   divisor = 1 + boostFraction (min 2.0, max 2.6)
 */
public class GHComposterBlockEntity extends BlockEntity {

    // ── Tag key for allowed items ─────────────────────────────────────────────
    private static final TagKey<Item> ORGANIC_COMPOSTABLES =
            TagKey.create(
                    net.minecraft.core.registries.Registries.ITEM,
                    ResourceLocation.fromNamespaceAndPath(
                            "gh_farming_overhaul", "organic_compostables"));

    // ── Constants ─────────────────────────────────────────────────────────────
    public static final int MAX_LEVEL = 8;

    // ── State ─────────────────────────────────────────────────────────────────
    private int fillLevel     = 0;
    private int compostTimer  = 0;
    private int compostTotalTime = 0;
    private boolean ready     = false;

    // Remaining spoil ticks of each item added — for cumulative median calc
    // Stored as a list of ints; one entry per item added (not per stack)
    private final List<Integer> addedSpoilTimes = new ArrayList<>();

    public GHComposterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GH_COMPOSTER.get(), pos, state);
    }

    // ── Tick ──────────────────────────────────────────────────────────────────

    public static void tick(Level level, BlockPos pos, BlockState state,
                            GHComposterBlockEntity entity) {
        if (level.isClientSide()) return;
        if (entity.ready) return; // waiting for extraction — pause timer
        if (entity.compostTimer <= 0) return;

        entity.compostTimer--;
        entity.setChanged();

        // Check if timer just finished
        if (entity.compostTimer <= 0 && entity.fillLevel >= MAX_LEVEL) {
            entity.ready = true;
            entity.setChanged();

            // Play ready sound
            level.playSound(null, pos, SoundEvents.COMPOSTER_READY,
                    SoundSource.BLOCKS, 1.0f, 1.0f);
        }
    }

    // ── Add single item ───────────────────────────────────────────────────────

    /**
     * Attempts to add one item from the held stack to the composter.
     * Uses vanilla compost chance map for fill level roll.
     * Adds the item's remaining spoil time to the cumulative timer.
     */
    public void addSingleItem(ItemStack stack, Level level, BlockPos pos) {
        if (ready || fillLevel >= MAX_LEVEL) return;
        if (!isCompostable(stack)) return;

        // Roll vanilla compost chance for fill level
        float chance = getCompostChance(stack);
        if (level.random.nextFloat() < chance) {
            fillLevel++;
            updateBlockState(level, pos);
            level.playSound(null, pos, SoundEvents.COMPOSTER_FILL,
                    SoundSource.BLOCKS, 1.0f, 1.0f);
        }

        // Add spoil time to cumulative timer
        int remainingTicks = getRemainingTicks(stack);
        if (remainingTicks > 0) {
            addedSpoilTimes.add(remainingTicks);
            int newBatchTimer = applyBoost(
                    calculateMedian(List.of(remainingTicks)),
                    level, pos);
            compostTimer    += newBatchTimer;
            compostTotalTime += newBatchTimer;
        }

        // Consume one item
        stack.shrink(1);
        setChanged();
    }

    // ── Add full stack (shift-click) ──────────────────────────────────────────

    /**
     * Quick-adds the entire held stack.
     * Median is calculated once for the whole batch then added once.
     */
    public void addStack(ItemStack stack, Level level, BlockPos pos) {
        if (ready || fillLevel >= MAX_LEVEL) return;
        if (!isCompostable(stack)) return;

        float chance = getCompostChance(stack);
        int count = stack.getCount();

        // Collect spoil times for the whole batch
        List<Integer> batchTimes = new ArrayList<>();
        int remaining = getRemainingTicks(stack);
        if (remaining > 0) {
            for (int i = 0; i < count; i++) {
                batchTimes.add(remaining);
            }
        }

        // Roll fill level for each item in the stack
        int filledBefore = fillLevel;
        for (int i = 0; i < count && fillLevel < MAX_LEVEL; i++) {
            if (level.random.nextFloat() < chance) {
                fillLevel++;
            }
        }

        if (fillLevel > filledBefore) {
            updateBlockState(level, pos);
            level.playSound(null, pos, SoundEvents.COMPOSTER_FILL,
                    SoundSource.BLOCKS, 1.0f, 1.0f);
        }

        // Calculate batch median once and add to timer
        if (!batchTimes.isEmpty()) {
            addedSpoilTimes.addAll(batchTimes);
            int batchMedian = calculateMedian(batchTimes);
            int newBatchTimer = applyBoost(batchMedian, level, pos);
            compostTimer     += newBatchTimer;
            compostTotalTime += newBatchTimer;
        }

        // Consume the whole stack
        stack.setCount(0);
        setChanged();
    }

    // ── Reset ─────────────────────────────────────────────────────────────────

    /**
     * Resets the composter to empty after fertilizer is extracted.
     */
    public void reset(Level level, BlockPos pos) {
        fillLevel        = 0;
        compostTimer     = 0;
        compostTotalTime = 0;
        ready            = false;
        addedSpoilTimes.clear();
        updateBlockState(level, pos);
        setChanged();
    }

    // ── Median calculation ────────────────────────────────────────────────────

    /**
     * Returns the median of a list of integers.
     * Sorted then picks middle value (or average of two middles for even size).
     */
    private static int calculateMedian(List<Integer> values) {
        if (values.isEmpty()) return 0;
        List<Integer> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int size = sorted.size();
        if (size % 2 == 1) {
            return sorted.get(size / 2);
        } else {
            return (sorted.get(size / 2 - 1) + sorted.get(size / 2)) / 2;
        }
    }

    // ── Boost calculation ─────────────────────────────────────────────────────

    /**
     * Divides baseTicks by the total boost divisor.
     * Base: always +100% → divisor 2.0
     * Hot chunk: +10%    → divisor 2.1
     * Summer:    +50%    → divisor 2.5 (or 2.6 if both)
     */
    private static int applyBoost(int baseTicks, Level level, BlockPos pos) {
        float boostFraction = 1.0f; // base +100%

        if (level instanceof ServerLevel sl) {
            if (SeasonsAPI.isLocalChunkHot(sl, pos)) {
                boostFraction += 0.10f;
            }
            if (SeasonsAPI.isSeason(sl, SeasonCalendar.Season.SUMMER)) {
                boostFraction += 0.50f;
            }
        }

        float divisor = 1.0f + boostFraction;
        return Math.max(1, (int) (baseTicks / divisor));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Returns true if the item is in the organic_compostables tag.
     */
    private static boolean isCompostable(ItemStack stack) {
        return stack.is(ORGANIC_COMPOSTABLES);
    }

    /**
     * Returns vanilla compost chance for the item (0.0-1.0).
     * Falls back to 0.65 if not in vanilla's compostables map.
     */
    private static float getCompostChance(ItemStack stack) {
        Float chance = ComposterBlock.COMPOSTABLES.getFloat(stack.getItem());
        return (chance != null && chance > 0) ? chance : 0.65f;
    }

    /**
     * Returns remaining spoil ticks from SpoilageComponent.
     * Returns 0 if item has no spoilage data.
     */
    private static int getRemainingTicks(ItemStack stack) {
        SpoilageComponent comp = stack.get(ModDataComponents.SPOILAGE.get());
        if (comp == null) return 0;
        return Math.max(0, comp.maxSpoilTicks() - comp.spoiledTicks());
    }

    /**
     * Syncs the LEVELS blockstate with the current fillLevel.
     */
    private void updateBlockState(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.hasProperty(GHComposterBlock.LEVELS)) {
            level.setBlockAndUpdate(pos,
                    state.setValue(GHComposterBlock.LEVELS, fillLevel));
        }
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public int getFillLevel()      { return fillLevel; }
    public int getCompostTimer()   { return compostTimer; }
    public int getCompostTotalTime(){ return compostTotalTime; }
    public boolean isReady()       { return ready; }

    // ── Save / Load ───────────────────────────────────────────────────────────

    @Override
    protected void saveAdditional(CompoundTag tag,
                                  HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt("FillLevel",        fillLevel);
        tag.putInt("CompostTimer",     compostTimer);
        tag.putInt("CompostTotalTime", compostTotalTime);
        tag.putBoolean("Ready",        ready);

        // Save added spoil times as int array
        int[] times = addedSpoilTimes.stream()
                .mapToInt(Integer::intValue).toArray();
        tag.putIntArray("AddedSpoilTimes", times);
    }

    @Override
    public void loadAdditional(CompoundTag tag,
                               HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        fillLevel        = tag.getInt("FillLevel");
        compostTimer     = tag.getInt("CompostTimer");
        compostTotalTime = tag.getInt("CompostTotalTime");
        ready            = tag.getBoolean("Ready");

        addedSpoilTimes.clear();
        for (int t : tag.getIntArray("AddedSpoilTimes")) {
            addedSpoilTimes.add(t);
        }
    }
}