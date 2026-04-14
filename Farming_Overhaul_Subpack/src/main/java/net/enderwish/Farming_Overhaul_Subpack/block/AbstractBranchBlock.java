package net.enderwish.Farming_Overhaul_Subpack.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class AbstractBranchBlock extends Block {
    // Thickness 1 (thin twig) to 7 (heavy limb/trunk)
    public static final IntegerProperty THICKNESS = IntegerProperty.create("thickness", 1, 7);
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public AbstractBranchBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(THICKNESS, 1)
                .setValue(FACING, Direction.UP));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int t = state.getValue(THICKNESS);
        Direction dir = state.getValue(FACING);

        // Math to center the branch:
        // A thickness of 1 = 2 pixels wide. 7 = 14 pixels wide.
        double width = t * 2.0;
        double min = (16.0 - width) / 2.0;
        double max = 16.0 - min;

        // Create the box based on the direction the branch is growing
        return switch (dir.getAxis()) {
            case X -> Shapes.box(0.0, min / 16.0, min / 16.0, 1.0, max / 16.0, max / 16.0);
            case Z -> Shapes.box(min / 16.0, min / 16.0, 0.0, max / 16.0, max / 16.0, 1.0);
            default -> Shapes.box(min / 16.0, 0.0, min / 16.0, max / 16.0, 1.0, max / 16.0);
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(THICKNESS, FACING);
    }
}