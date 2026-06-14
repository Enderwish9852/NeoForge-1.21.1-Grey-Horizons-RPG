package net.enderwish.Farming_Overhaul_Subpack.block.cutting_board;

import com.mojang.serialization.MapCodec;
import net.enderwish.Farming_Overhaul_Subpack.gui.CuttingBoardMenu;
import net.enderwish.Farming_Overhaul_Subpack.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class CuttingBoardBlock extends BaseEntityBlock {

    public static final MapCodec<CuttingBoardBlock> CODEC =
            simpleCodec(CuttingBoardBlock::new);

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    public CuttingBoardBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CuttingBoardBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type,
                ModBlockEntities.CUTTING_BOARD.get(),
                CuttingBoardBlockEntity::tick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state,
                                              Level level, BlockPos pos,
                                              Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        if (level.isClientSide()) return ItemInteractionResult.SUCCESS;

        CuttingBoardBlockEntity entity =
                (CuttingBoardBlockEntity) level.getBlockEntity(pos);
        if (entity == null)
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        player.openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable(
                        "container.gh_farming_overhaul.cutting_board");
            }

            @Override
            public AbstractContainerMenu createMenu(int windowId,
                                                    Inventory inv,
                                                    Player p) {
                return new CuttingBoardMenu(windowId, inv, entity,
                        new SimpleContainerData(
                                CuttingBoardMenu.DATA_COUNT));
            }
        }, buf -> buf.writeBlockPos(pos));

        return ItemInteractionResult.SUCCESS;
    }
}
