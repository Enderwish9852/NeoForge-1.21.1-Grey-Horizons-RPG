package net.enderwish.TerraForma_Subpack.core.worldgen.noise;

import net.minecraft.world.level.block.state.BlockState;

/**
 * SurfacePalette
 *
 * Per-biome surface blocks: {@code top} is the single surface block,
 * {@code sub} fills {@code subDepth} blocks beneath it, and {@code deep}
 * fills everything below that down to the cave/stone layer.
 */
public record SurfacePalette(BlockState top, BlockState sub, int subDepth, BlockState deep) {
    public static SurfacePalette of(BlockState top, BlockState sub, int subDepth, BlockState deep) {
        return new SurfacePalette(top, sub, subDepth, deep);
    }
}
