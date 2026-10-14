package net.enderwish.Farming_Overhaul_Subpack.block.tree;

import net.minecraft.util.StringRepresentable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
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
 * NOTE: joints are welded Dynamic-Trees-style — each log's model extends
 * 8px flush into its parent voxel (PARENT side; every branch starts
 * strictly thinner than its parent). Known tradeoff: two extensions
 * entering the same parent voxel can z-fight inside the joint. End caps
 * (CAPPED) render on tips, bends and shoulders; straight equal-thickness
 * runs stay open.
 */
public class FruitTreeLogBlock extends Block {

    public static final IntegerProperty THICKNESS =
            IntegerProperty.create("thickness", 1, 6);

    public static final EnumProperty<Direction.Axis> AXIS =
            BlockStateProperties.AXIS;

    /**
     * Which side (along AXIS) the parent voxel lies on. The block model
     * extends ~8px into the parent voxel so joints between different
     * thicknesses stay visually welded (Dynamic-Trees-style connections).
     */
    public static final EnumProperty<ParentSide> PARENT =
            EnumProperty.create("parent", ParentSide.class);

    /**
     * True when this voxel ends in its +AXIS direction (branch tip or a
     * shoulder where the next voxel is thinner): the model renders the
     * ring-textured end cap. Interior/bending voxels are open on that side
     * so no pale cap quad shows through joints.
     */
    public static final BooleanProperty CAPPED =
            BooleanProperty.create("capped");

    public enum ParentSide implements StringRepresentable {
        NEG("neg"), POS("pos");
        private final String name;
        ParentSide(String name) { this.name = name; }
        @Override public String getSerializedName() { return name; }
    }

    public static ParentSide sideOf(Direction parentDir, Direction.Axis axis) {
        boolean positive = switch (axis) {
            case X -> parentDir == Direction.EAST;
            case Y -> parentDir == Direction.UP;
            case Z -> parentDir == Direction.SOUTH;
        };
        return positive ? ParentSide.POS : ParentSide.NEG;
    }

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
                        .setValue(AXIS, Direction.Axis.Y)
                        .setValue(PARENT, ParentSide.NEG)
                        .setValue(CAPPED, true));
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(THICKNESS, AXIS, PARENT, CAPPED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clicked = context.getClickedFace();
        return this.defaultBlockState()
                .setValue(AXIS, clicked.getAxis())
                .setValue(PARENT, sideOf(clicked.getOpposite(), clicked.getAxis()));
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