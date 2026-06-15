package net.enderwish.Farming_Overhaul_Subpack.item;

import net.enderwish.Farming_Overhaul_Subpack.block.farmland.FertilizedFarmlandBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

/**
 * SelfPlantingCropItem
 *
 * Used by root crops (garlic, onion, sweet_potato) where
 * the crop item itself is planted — no separate seed item.
 *
 * Right click on fertilized farmland → places the crop block at age 0.
 * Consumes one item from the stack.
 * Only works on fertilized farmland (dry or moist).
 */
public class SelfPlantingCropItem extends Item {

    private final Supplier<Block> cropBlockSupplier;

    public SelfPlantingCropItem(Supplier<Block> cropBlockSupplier,
                                Properties properties) {
        super(properties);
        this.cropBlockSupplier = cropBlockSupplier;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();

        // Must click the TOP face of the farmland
        if (context.getClickedFace() != Direction.UP) {
            return InteractionResult.PASS;
        }

        BlockState below = level.getBlockState(pos);

        // Only plant on fertilized farmland
        if (!FertilizedFarmlandBlock.isFertilizedFarmland(below)) {
            return InteractionResult.PASS;
        }

        // Position above the farmland
        BlockPos plantPos = pos.above();
        BlockState plantState = level.getBlockState(plantPos);

        // Must be empty/replaceable above
        if (!plantState.isAir() && !plantState.canBeReplaced()) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide()) {
            // Place crop block at age 0
            Block cropBlock = cropBlockSupplier.get();
            level.setBlockAndUpdate(plantPos,
                    cropBlock.defaultBlockState());

            level.playSound(null, plantPos,
                    SoundEvents.CROP_PLANTED,
                    SoundSource.BLOCKS, 1.0f, 1.0f);

            // Consume one item
            if (player != null && !player.isCreative()) {
                context.getItemInHand().shrink(1);
            }
        }

        return InteractionResult.SUCCESS;
    }
}
