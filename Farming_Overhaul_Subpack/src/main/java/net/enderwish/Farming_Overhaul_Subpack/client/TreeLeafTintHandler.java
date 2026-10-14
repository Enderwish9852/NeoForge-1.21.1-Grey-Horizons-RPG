package net.enderwish.Farming_Overhaul_Subpack.client;

import net.enderwish.Farming_Overhaul_Subpack.FarmingOverhaulSubpack;
import net.enderwish.Farming_Overhaul_Subpack.block.ModBlocks;
import net.enderwish.Farming_Overhaul_Subpack.block.tree.FruitTreeLeavesBlock;
import net.enderwish.Farming_Overhaul_Subpack.item.ModItems;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/**
 * TreeLeafTintHandler — client tint registration for fruit-tree leaves.
 *
 * WHY THIS EXISTS: the leaf models use vanilla's greyscale leaf texture
 * (minecraft:block/oak_leaves). Without a BlockColors registration the
 * texture renders raw grey. Vanilla tints leaves with the biome's
 * foliage colour; we do the same, then bend it per SEASON_STAGE so
 * seasons read on the canopy:
 *
 *   BARE     → muted brown-green (sparser, dying)
 *   BUDDING  → pale yellow-green (new growth)
 *   BLOSSOM  → washed toward blossom pink
 *   FULL     → pure biome foliage colour
 *   COLOURED → washed toward autumn orange
 *
 * Item form (creative/pick-block) uses a fixed pleasant green.
 */
@EventBusSubscriber(
        modid = FarmingOverhaulSubpack.MODID,
        value = Dist.CLIENT,
        bus = EventBusSubscriber.Bus.MOD)
public class TreeLeafTintHandler {

    private static final int FALLBACK_FOLIAGE = 0x48B518;

    // DeferredHolders are resolved LAZILY inside the event handlers —
    // calling .get() at class-init time crashes with
    // "Trying to access unbound value" because registration hasn't run yet.

    @SubscribeEvent
    public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
        BlockColor resolver = (state, level, pos, tintIndex) ->
                tint(state, level, pos);
        event.register(resolver,
                ModBlocks.OAK_LEAVES.get(),
                ModBlocks.BIRCH_LEAVES.get(),
                ModBlocks.SPRUCE_LEAVES.get(),
                ModBlocks.ACACIA_LEAVES.get(),
                ModBlocks.DARK_OAK_LEAVES.get(),
                ModBlocks.JUNGLE_LEAVES.get(),
                ModBlocks.CHERRY_LEAVES.get(),
                ModBlocks.MANGROVE_LEAVES.get());
    }

    @SubscribeEvent
    public static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        // NOTE: only oak has a registered leaf item so far — extend here
        // as more species leaf items get added to ModItems.
        ItemColor itemResolver = (stack, tintIndex) -> FALLBACK_FOLIAGE;
        event.register(itemResolver, ModItems.OAK_LEAVES.get());
    }

    private static int tint(BlockState state,
                            net.minecraft.world.level.BlockAndTintGetter level,
                            BlockPos pos) {
        int foliage = FALLBACK_FOLIAGE;
        if (level != null && pos != null) {
            // vanilla's own pre-seeded foliage resolver (ClientLevel seeds
            // exactly GRASS/FOLIAGE/WATER resolvers — any other ColorResolver
            // instance resolves to a null cache and NPEs during tesselation)
            foliage = level.getBlockTint(pos, BiomeColors.FOLIAGE_COLOR_RESOLVER);
        }
        if (!(state.getBlock() instanceof FruitTreeLeavesBlock)) return foliage;

        return switch (state.getValue(FruitTreeLeavesBlock.SEASON_STAGE)) {
            case BARE     -> mix(foliage, 0x7A5A34, 0.75f);
            case BUDDING  -> mix(foliage, 0xC9D96A, 0.40f);
            case BLOSSOM  -> mix(foliage, 0xF4C6D0, 0.60f);
            case FULL     -> foliage;
            case COLOURED -> mix(foliage, 0xC1642A, 0.70f);
        };
    }

    private static int mix(int a, int b, float bWeight) {
        int r = (int) (((a >> 16) & 0xFF) * (1f - bWeight)
                + ((b >> 16) & 0xFF) * bWeight);
        int g = (int) (((a >> 8) & 0xFF) * (1f - bWeight)
                + ((b >> 8) & 0xFF) * bWeight);
        int bl = (int) ((a & 0xFF) * (1f - bWeight) + (b & 0xFF) * bWeight);
        return (r << 16) | (g << 8) | bl;
    }
}
