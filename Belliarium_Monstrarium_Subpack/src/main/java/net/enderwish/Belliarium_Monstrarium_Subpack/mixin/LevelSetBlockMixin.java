package net.enderwish.Belliarium_Monstrarium_Subpack.mixin;

import net.enderwish.Belliarium_Monstrarium_Subpack.core.diary.DayTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * LevelSetBlockMixin
 *
 * Hooks the 4-arg Level#setBlock -- the single method every block change in the
 * loaded world goes through, so it also catches random-tick growth and spread,
 * fire, fluids, pistons and the seasonal snow/ice. Injected at HEAD so the
 * original state is read BEFORE it is overwritten.
 *
 * Known limit: anything that bypasses Level#setBlock (worldgen through
 * WorldGenRegion, or code calling LevelChunk#setBlockState directly) isn't seen.
 *
 * VERIFY IF COMPILE FAILS -- MinecraftServer#isSameThread().
 */
@Mixin(Level.class)
public abstract class LevelSetBlockMixin {

    @Inject(
            method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
            at = @At("HEAD")
    )
    private void ghBelliarium$logBlockChange(BlockPos pos, BlockState newState, int flags, int recursionLeft,
                                             CallbackInfoReturnable<Boolean> cir) {
        if (!DayTracker.isTracking()) return; // fast path: one volatile read when idle

        Level self = (Level) (Object) this;
        if (!(self instanceof ServerLevel serverLevel)) return;
        if (serverLevel.dimension() != Level.OVERWORLD) return;
        if (!serverLevel.getServer().isSameThread()) return; // the log isn't thread-safe
        if (serverLevel.isOutsideBuildHeight(pos)) return;

        DayTracker.record(serverLevel, pos, newState);
    }
}
