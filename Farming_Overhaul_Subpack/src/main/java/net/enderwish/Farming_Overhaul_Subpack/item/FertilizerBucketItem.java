package net.enderwish.Farming_Overhaul_Subpack.item;

import net.enderwish.Farming_Overhaul_Subpack.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

/**
 * FertilizerBucketItem
 *
 * Right click on normal farmland → converts to fertilized_farmland.
 * Starts at moisture 0 — the block's own randomTick will detect nearby
 * water and jump to moisture 7 naturally, same as vanilla farmland
 * does after tilling.
 *
 * Consumes the bucket → returns the empty bucket variant defined by
 * emptyBucketSupplier (wooden_bucket for the wooden version,
 * minecraft:bucket for the iron version).
 */
public class FertilizerBucketItem extends Item {

    private final Supplier<Item> emptyBucketSupplier;

    public FertilizerBucketItem(Properties properties, Supplier<Item> emptyBucketSupplier) {
        super(properties);
        this.emptyBucketSupplier = emptyBucketSupplier;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        BlockState state = level.getBlockState(pos);

        if (level.isClientSide()) return InteractionResult.SUCCESS;

        if (!state.is(Blocks.FARMLAND)) {
            return InteractionResult.PASS;
        }

        level.setBlockAndUpdate(pos,
                ModBlocks.FERTILIZED_FARMLAND.get().defaultBlockState());

        level.playSound(null, pos,
                SoundEvents.SAND_PLACE,
                SoundSource.BLOCKS, 1.0f, 1.0f);

        if (player != null && !player.isCreative()) {
            ItemStack held = context.getItemInHand();
            held.shrink(1);
            ItemStack emptyBucket = new ItemStack(emptyBucketSupplier.get());
            if (held.isEmpty()) {
                player.setItemInHand(context.getHand(), emptyBucket);
            } else {
                player.getInventory().add(emptyBucket);
            }
        }

        return InteractionResult.SUCCESS;
    }
}
