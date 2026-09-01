package net.enderwish.Farming_Overhaul_Subpack.block.tree;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * FruitTreeLogBlock
 *
 * Universal log block for all tree species. Species is identified by
 * which block is registered (oak_log, mango_log etc.).
 *
 * THICKNESS — 1 (sprig/2px) to 6 (trunk/16px)
 * AXIS      — X, Y, Z
 *
 * Universal pixel palette (level → size):
 *   6=16px(trunk)  5=12px(limb)  4=8px(branch)
 *   3=6px(branchlet)  2=4px(twig)  1=2px(sprig)
 *
 * Species choose which levels their trunk/primary/secondary/twig
 * generations use via TreeDefinition JSON — not every species needs
 * every size.
 *
 * NOTE: the CONNECTOR transition system is deferred — junctions
 * currently show a small natural seam. Revisit later with a
 * neighbor-detection post-pass.
 */
public class FruitTreeLogBlock extends Block {

    public static final IntegerProperty THICKNESS =
            IntegerProperty.create("thickness", 1, 6);

    public static final EnumProperty<Direction.Axis> AXIS =
            BlockStateProperties.AXIS;

    private static final int[] PIXEL_SIZE = {0, 2, 4, 6, 8, 12, 16};

    private static final VoxelShape[] SHAPE_Y = new VoxelShape[7];
    private static final VoxelShape[] SHAPE_X = new VoxelShape[7];
    private static final VoxelShape[] SHAPE_Z = new VoxelShape[7];

    static {
        for (int level = 1; level <= 6; level++) {
            int size = PIXEL_SIZE[level];
            int off  = (16 - size) / 2;
            SHAPE_Y[level] = Block.box(off, 0, off, off + size, 16, off + size);
            SHAPE_X[level] = Block.box(0, off, off, 16, off + size, off + size);
            SHAPE_Z[level] = Block.box(off, off, 0, off + size, off + size, 16);
        }
    }

    private final String species;

    public FruitTreeLogBlock(String species, BlockBehaviour.Properties properties) {
        super(properties);
        this.species = species;
        this.registerDefaultState(
                this.stateDefinition.any()
                        .setValue(THICKNESS, 6)
                        .setValue(AXIS, Direction.Axis.Y));
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(THICKNESS, AXIS);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(AXIS, context.getClickedFace().getAxis());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level,
                               BlockPos pos, CollisionContext context) {
        int thickness = state.getValue(THICKNESS);
        return switch (state.getValue(AXIS)) {
            case Y -> SHAPE_Y[thickness];
            case X -> SHAPE_X[thickness];
            case Z -> SHAPE_Z[thickness];
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level,
                                        BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    public String getSpecies() { return species; }

    public static boolean isTrunk(BlockState state) {
        return state.getBlock() instanceof FruitTreeLogBlock
                && state.getValue(THICKNESS) == 6;
    }

    public static boolean isStructural(BlockState state) {
        return state.getBlock() instanceof FruitTreeLogBlock
                && state.getValue(THICKNESS) >= 4;
    }
}