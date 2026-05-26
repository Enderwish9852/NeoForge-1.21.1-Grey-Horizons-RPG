package net.enderwish.Farming_Overhaul_Subpack.block.clay_pot;

import com.mojang.serialization.MapCodec;
import net.enderwish.Farming_Overhaul_Subpack.block.ModBlocks;
import net.enderwish.Farming_Overhaul_Subpack.gui.ClayPotMenu;
import net.enderwish.Farming_Overhaul_Subpack.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class ClayPotBlock extends BaseEntityBlock {

    public static final MapCodec<ClayPotBlock> CODEC = simpleCodec(ClayPotBlock::new);

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    public ClayPotBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ClayPotBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,
                                                                  BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, ModBlockEntities.CLAY_POT.get(),
                ClayPotBlockEntity::tick);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos,
                           BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);

        if (oldState.is(ModBlocks.WET_CLAY_POT.get())) return;

        BlockState below = level.getBlockState(pos.below());
        if (!below.is(Blocks.CAMPFIRE) && !below.is(Blocks.SOUL_CAMPFIRE)) {
            level.destroyBlock(pos, true);
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state,
                                              Level level, BlockPos pos,
                                              Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        if (level.isClientSide()) return ItemInteractionResult.SUCCESS;

        ClayPotBlockEntity entity = (ClayPotBlockEntity) level.getBlockEntity(pos);
        if (entity == null) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        // Right click with empty bowl — collect output
        if (stack.is(Items.BOWL) && entity.getBowlsRemaining() > 0) {
            ItemStack result = entity.collectBowl();
            if (!result.isEmpty()) {
                stack.shrink(1);
                if (stack.isEmpty()) {
                    player.setItemInHand(hand, result);
                } else {
                    player.getInventory().add(result);
                }
                return ItemInteractionResult.SUCCESS;
            }
        }

        // Open GUI
        player.openMenu(new net.minecraft.world.MenuProvider() {
            @Override
            public net.minecraft.network.chat.Component getDisplayName() {
                return net.minecraft.network.chat.Component.translatable(
                        "container.gh_farming_overhaul.clay_pot");
            }

            @Override
            public net.minecraft.world.inventory.AbstractContainerMenu createMenu(
                    int windowId,
                    net.minecraft.world.entity.player.Inventory inv,
                    net.minecraft.world.entity.player.Player p) {
                return new ClayPotMenu(windowId, inv, entity,
                        new SimpleContainerData(ClayPotMenu.DATA_COUNT));
            }
        }, buf -> buf.writeBlockPos(pos));

        return ItemInteractionResult.SUCCESS;
    }
}
