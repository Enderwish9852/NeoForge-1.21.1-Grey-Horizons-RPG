package net.enderwish.Farming_Overhaul_Subpack.block.tree;

import net.enderwish.Atmospheric_Overhaul_Subpack.api.SeasonsAPI;
import net.enderwish.Atmospheric_Overhaul_Subpack.core.season.SeasonCalendar;
import net.enderwish.Farming_Overhaul_Subpack.core.tree.TreeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * FruitBlock
 *
 * Grows on FruitTreeLeavesBlock and FruitTreeLogBlock faces.
 * Species identified by which block is registered (orange_fruit etc.)
 *
 * Block states:
 *   AGE     — 0 (budding) → 1 (growing) → 2 (ripe) → 3 (overripe)
 *   FACING  — which direction the fruit hangs from
 *             (away from the log/leaf it is attached to)
 *
 * Behaviour:
 *   AGE 0-1 → growing, not harvestable, small visual
 *   AGE 2   → ripe, right-click to harvest, resets to AGE 0
 *   AGE 3   → overripe, right-click OR random tick chance to drop
 *             as item entity on ground
 *
 * Growth:
 *   Only advances age during the species' fruiting seasons
 *   (from TreeDefinition.fruitingSeasons)
 *   Growth chance per random tick from TreeDefinition.fruitGrowthChance
 *
 * Placement:
 *   Placed by TreeGenerator on leaf/log faces during tree generation
 *   FACING = direction away from attached block
 *
 * Survival:
 *   Must be attached to a FruitTreeLeavesBlock or FruitTreeLogBlock
 *   If support block is removed → drops as item
 */
public class FruitBlock extends Block {

    // ── Block states ──────────────────────────────────────────────────────────

    public static final IntegerProperty AGE =
            IntegerProperty.create("age", 0, 3);

    public static final DirectionProperty FACING =
            DirectionProperty.create("facing",
                    Direction.NORTH, Direction.SOUTH,
                    Direction.EAST, Direction.WEST,
                    Direction.DOWN); // DOWN = fruit hanging below leaf

    // ── Shapes per age per facing ─────────────────────────────────────────────
    // Fruit grows from small (age 0) to full size (age 2-3)
    // Positioned on the face indicated by FACING

    // DOWN facing (hanging fruit) — most common
    private static final VoxelShape[] SHAPES_DOWN = {
            Block.box(6, 10, 6, 10, 16, 10),  // age 0 — tiny bud
            Block.box(5,  8, 5, 11, 16, 11),  // age 1 — growing
            Block.box(4,  4, 4, 12, 16, 12),  // age 2 — ripe
            Block.box(4,  4, 4, 12, 16, 12),  // age 3 — overripe (same size)
    };

    // NORTH facing (on north face of log)
    private static final VoxelShape[] SHAPES_NORTH = {
            Block.box(6,  6, 12, 10, 10, 16),
            Block.box(5,  5, 10, 11, 11, 16),
            Block.box(4,  4,  5, 12, 12, 16),
            Block.box(4,  4,  5, 12, 12, 16),
    };

    // SOUTH facing
    private static final VoxelShape[] SHAPES_SOUTH = {
            Block.box(6,  6, 0, 10, 10,  4),
            Block.box(5,  5, 0, 11, 11,  6),
            Block.box(4,  4, 0, 12, 12, 11),
            Block.box(4,  4, 0, 12, 12, 11),
    };

    // EAST facing
    private static final VoxelShape[] SHAPES_EAST = {
            Block.box(0, 6,  6,  4, 10, 10),
            Block.box(0, 5,  5,  6, 11, 11),
            Block.box(0, 4,  4, 11, 12, 12),
            Block.box(0, 4,  4, 11, 12, 12),
    };

    // WEST facing
    private static final VoxelShape[] SHAPES_WEST = {
            Block.box(12, 6,  6, 16, 10, 10),
            Block.box(10, 5,  5, 16, 11, 11),
            Block.box( 5, 4,  4, 16, 12, 12),
            Block.box( 5, 4,  4, 16, 12, 12),
    };

    // ── Species ID ────────────────────────────────────────────────────────────
    private final String species;

    // ── Constructor ───────────────────────────────────────────────────────────

    public FruitBlock(String species, BlockBehaviour.Properties properties) {
        super(properties);
        this.species = species;
        this.registerDefaultState(
                this.stateDefinition.any()
                        .setValue(AGE, 0)
                        .setValue(FACING, Direction.DOWN));
    }

    // ── Block state ───────────────────────────────────────────────────────────

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE, FACING);
    }

    // ── Shape ─────────────────────────────────────────────────────────────────

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level,
                               BlockPos pos, CollisionContext context) {
        int age = state.getValue(AGE);
        return switch (state.getValue(FACING)) {
            case DOWN  -> SHAPES_DOWN[age];
            case NORTH -> SHAPES_NORTH[age];
            case SOUTH -> SHAPES_SOUTH[age];
            case EAST  -> SHAPES_EAST[age];
            case WEST  -> SHAPES_WEST[age];
            default    -> SHAPES_DOWN[age];
        };
    }

    // ── No collision — fruit is passable ──────────────────────────────────────

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level,
                                        BlockPos pos,
                                        CollisionContext context) {
        return net.minecraft.world.phys.shapes.Shapes.empty();
    }

    // ── Random tick — growth and overripe drop ────────────────────────────────

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level,
                           BlockPos pos, RandomSource random) {
        int age = state.getValue(AGE);

        // Check if in fruiting season
        if (!isInFruitingSeason(level)) return;

        if (age < 3) {
            // Advance age based on growth chance from TreeDefinition
            float growthChance = getGrowthChance();
            if (random.nextFloat() < growthChance) {
                level.setBlockAndUpdate(pos,
                        state.setValue(AGE, age + 1));
            }
        } else {
            // Age 3 (overripe) — chance to fall as item
            float dropChance = getDropChance();
            if (random.nextFloat() < dropChance) {
                dropFruit(level, pos, state);
                level.removeBlock(pos, false);
            }
        }
    }

    // ── Right click — harvest when ripe ───────────────────────────────────────

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack,
                                              BlockState state,
                                              Level level, BlockPos pos,
                                              Player player,
                                              InteractionHand hand,
                                              BlockHitResult hit) {
        int age = state.getValue(AGE);

        // Only harvestable at age 2 (ripe) or 3 (overripe)
        if (age < 2) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!level.isClientSide()) {
            // Drop the fruit item
            dropFruit(level, pos, state);

            // Reset to age 0 — regrows next fruiting season
            level.setBlockAndUpdate(pos, state.setValue(AGE, 0));

            // Play harvest sound
            level.playSound(null, pos,
                    net.minecraft.sounds.SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES,
                    net.minecraft.sounds.SoundSource.BLOCKS,
                    1.0f, 0.8f + level.random.nextFloat() * 0.4f);
        }

        return ItemInteractionResult.SUCCESS;
    }

    // ── Survival — must be attached to tree block ─────────────────────────────

    @Override
    public boolean canSurvive(BlockState state, LevelReader level,
                              BlockPos pos) {
        // The block this fruit is attached to is OPPOSITE of FACING
        Direction facing   = state.getValue(FACING);
        BlockPos attachPos = pos.relative(facing.getOpposite());
        BlockState attach  = level.getBlockState(attachPos);

        return attach.getBlock() instanceof FruitTreeLeavesBlock
                || attach.getBlock() instanceof FruitTreeLogBlock;
    }

    @Override
    public void neighborChanged(BlockState state, Level level,
                                BlockPos pos, Block block,
                                BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (!state.canSurvive(level, pos)) {
            // Drop as item then remove
            dropFruit(level, pos, state);
            level.destroyBlock(pos, false);
        }
    }

    // ── Fruit drop ────────────────────────────────────────────────────────────

    /**
     * Drops the fruit item at this position.
     * Item comes from TreeDefinition.fruitItem for this species.
     */
    private void dropFruit(Level level, BlockPos pos, BlockState state) {
        TreeRegistry.INSTANCE.getBySpecies(species).ifPresent(def -> {
            if (def.getFruitItem() == null) return;

            net.minecraft.world.item.Item item =
                    net.minecraft.core.registries.BuiltInRegistries.ITEM
                            .get(net.minecraft.resources.ResourceLocation
                                    .parse(def.getFruitItem()));

            if (item == Items.AIR) return;

            ItemStack drop = new ItemStack(item);
            net.minecraft.world.entity.item.ItemEntity entity =
                    new net.minecraft.world.entity.item.ItemEntity(
                            level,
                            pos.getX() + 0.5,
                            pos.getY() + 0.2,
                            pos.getZ() + 0.5,
                            drop);
            level.addFreshEntity(entity);
        });
    }

    // ── Season + growth helpers ───────────────────────────────────────────────

    private boolean isInFruitingSeason(ServerLevel level) {
        return TreeRegistry.INSTANCE.getBySpecies(species)
                .map(def -> {
                    SeasonCalendar.Season season =
                            SeasonsAPI.getSeason(level);
                    return def.fruitsInSeason(season.name());
                })
                .orElse(false);
    }

    private float getGrowthChance() {
        return TreeRegistry.INSTANCE.getBySpecies(species)
                .map(def -> def.getFruitGrowthChance())
                .orElse(0.05f);
    }

    private float getDropChance() {
        return TreeRegistry.INSTANCE.getBySpecies(species)
                .map(def -> def.getFruitDropChance())
                .orElse(0.02f);
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public String getSpecies() { return species; }

    public static boolean isRipe(BlockState state) {
        return state.getBlock() instanceof FruitBlock
                && state.getValue(AGE) >= 2;
    }
}
