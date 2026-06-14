package net.enderwish.Farming_Overhaul_Subpack.block.composter;

import com.mojang.serialization.MapCodec;
import net.enderwish.Farming_Overhaul_Subpack.init.ModBlockEntities;
import net.enderwish.Farming_Overhaul_Subpack.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * GHComposterBlock
 *
 * A custom composter that replaces vanilla composter behaviour.
 * Uses a BlockEntity to store composting state, timer, and fill level.
 *
 * Accepts only items tagged gh_farming_overhaul:organic_compostables
 * which must have a SpoilageComponent (remaining spoil time drives timer).
 *
 * Right click with compostable item  → add to composter
 * Shift + right click with stack     → quick-add full stack
 * Right click with wooden_bucket when ready → extract wooden_bucket_of_fertilizer
 * Right click with minecraft:bucket when ready → extract bucket_of_fertilizer
 *
 * Visual fill level: LEVELS property 0-8 (same as vanilla composter)
 */
public class GHComposterBlock extends BaseEntityBlock {

    // ── Codec — required by BaseEntityBlock in 1.21.1 ─────────────────────────
    private static final MapCodec<GHComposterBlock> CODEC =
            simpleCodec(GHComposterBlock::new);

    @Override
    public MapCodec<GHComposterBlock> codec() {
        return CODEC;
    }

    // ── Block state ───────────────────────────────────────────────────────────
    public static final IntegerProperty LEVELS =
            BlockStateProperties.LEVEL_COMPOSTER; // 0-8

    private static final VoxelShape OUTER =
            Block.box(0, 0, 0, 16, 16, 16);

    // ── Constructor ───────────────────────────────────────────────────────────
    public GHComposterBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(
                this.stateDefinition.any().setValue(LEVELS, 0));
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVELS);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level,
                               BlockPos pos, CollisionContext context) {
        return OUTER;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    // ── BlockEntity ───────────────────────────────────────────────────────────

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GHComposterBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type,
                ModBlockEntities.GH_COMPOSTER.get(),
                GHComposterBlockEntity::tick);
    }

    // ── Right click interaction ───────────────────────────────────────────────

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level,
                                               BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof GHComposterBlockEntity composter))
            return InteractionResult.PASS;

        ItemStack held = player.getMainHandItem();

        // ── Extract with bucket when ready ────────────────────────────────────
        if (composter.isReady()) {
            if (held.is(ModItems.WOODEN_BUCKET.get())) {
                extractFertilizer(level, pos, player, composter,
                        held, ModItems.WOODEN_BUCKET_OF_FERTILIZER.get());
                return InteractionResult.SUCCESS;
            }
            if (held.is(Items.BUCKET)) {
                extractFertilizer(level, pos, player, composter,
                        held, ModItems.BUCKET_OF_FERTILIZER.get());
                return InteractionResult.SUCCESS;
            }
            // Ready but wrong item — play hint sound
            level.playSound(null, pos, SoundEvents.COMPOSTER_READY,
                    SoundSource.BLOCKS, 1.0f, 1.0f);
            return InteractionResult.SUCCESS;
        }

        // ── Add item to composter ─────────────────────────────────────────────
        if (!held.isEmpty()) {
            if (player.isShiftKeyDown()) {
                composter.addStack(held, level, pos);
            } else {
                composter.addSingleItem(held, level, pos);
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    // ── Extract fertilizer ────────────────────────────────────────────────────

    private static void extractFertilizer(Level level, BlockPos pos,
                                          Player player,
                                          GHComposterBlockEntity composter,
                                          ItemStack heldBucket,
                                          net.minecraft.world.item.Item outputItem) {
        if (!player.isCreative()) {
            heldBucket.shrink(1);
        }

        ItemStack fertilizer = new ItemStack(outputItem);
        if (!player.getInventory().add(fertilizer)) {
            player.drop(fertilizer, false);
        }

        composter.reset(level, pos);

        level.playSound(null, pos, SoundEvents.COMPOSTER_EMPTY,
                SoundSource.BLOCKS, 1.0f, 1.0f);
    }

    // ── Break ─────────────────────────────────────────────────────────────────

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                         BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
}
