package net.enderwish.TerraForma_Subpack.core.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.enderwish.TerraForma_Subpack.core.worldgen.noise.GHNoiseRouter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * GHChunkGenerator
 *
 * First real working version — turns GHNoiseRouter's terrain-shape
 * queries into actual placed blocks. Deliberately simple right now:
 * solid stone below the surface, 4 blocks of dirt near the top with
 * grass on the very top block, water filling anything below sea
 * level, and GHNoiseRouter's own cave noise carving out air pockets
 * as it goes (per your confirmation to use the custom cave system
 * instead of vanilla's carver pipeline — that's why applyCarvers()
 * below is a deliberate no-op).
 *
 * NOT YET DONE (next passes, once this compiles and generates land):
 *   - Biome-aware surface blocks (arctic should get snow/podzol etc.
 *     — everything is dirt/grass/stone regardless of biome for now)
 *   - Ore vein placement
 *   - River carving (GHNoiseRouter.isRiver() exists but isn't
 *     consumed here yet — flagged in the review as needing continuous
 *     path-following logic, not just a per-point check)
 *
 * VERIFY IF COMPILE FAILS:
 *   - ChunkAccess.setBlockState(BlockPos, BlockState, boolean) — this
 *     is extremely standard, stable vanilla API used in essentially
 *     every chunk generator ever written, but I don't have
 *     ChunkAccess's actual source confirmed this round the way I do
 *     for ChunkGenerator/BiomeSource.
 *   - NoiseColumn's constructor — I'm using
 *     `new NoiseColumn(minBuildHeight, BlockState[])` based on general
 *     recollection, not a confirmed signature.
 */
public class GHChunkGenerator extends ChunkGenerator {

    public static final MapCodec<GHChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(GHChunkGenerator::getBiomeSource)
            ).apply(instance, GHChunkGenerator::new)
    );

    private final GHNoiseRouter noiseRouter;

    public GHChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
        // Placeholder seed — GHNoiseRouter's noise layers need a real
        // seed threaded through eventually (same concern as
        // ClimateMap.setSeed() needing the real world seed, flagged
        // in last round's review). Not wired yet.
        this.noiseRouter = new GHNoiseRouter(RandomSource.create());
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public void applyCarvers(WorldGenRegion level, long seed, RandomState randomState,
                             BiomeManager biomeManager, StructureManager structureManager,
                             ChunkAccess chunk, GenerationStep.Carving step) {
        // No-op — GHNoiseRouter's own cave noise carves directly inside
        // fillFromNoise below instead of using vanilla's carver pipeline.
    }

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager,
                             RandomState randomState, ChunkAccess chunk) {
        // Surface blocks are placed directly in fillFromNoise for now —
        // revisit once biome-aware surface blocks are added.
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion level) {
        // TerraFormaSubpack already globally cancels vanilla hostile
        // spawns; nothing to do here yet.
    }

    @Override
    public int getGenDepth() {
        return GHNoiseRouter.MAX_HEIGHT - GHNoiseRouter.MIN_HEIGHT;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState,
                                                        StructureManager structureManager,
                                                        ChunkAccess chunk) {
        ChunkPos chunkPos = chunk.getPos();
        int minX = chunkPos.getMinBlockX();
        int minZ = chunkPos.getMinBlockZ();

        BlockState stoneState = Blocks.STONE.defaultBlockState();
        BlockState dirtState  = Blocks.DIRT.defaultBlockState();
        BlockState grassState = Blocks.GRASS_BLOCK.defaultBlockState();
        BlockState waterState = Blocks.WATER.defaultBlockState();

        for (int x = minX; x < minX + 16; x++) {
            for (int z = minZ; z < minZ + 16; z++) {
                int surfaceY = noiseRouter.getSurfaceHeight(x, z);

                for (int y = GHNoiseRouter.MIN_HEIGHT; y <= Math.max(surfaceY, GHNoiseRouter.SEA_LEVEL); y++) {
                    BlockPos pos = new BlockPos(x, y, z);

                    if (y > surfaceY) {
                        if (y <= GHNoiseRouter.SEA_LEVEL) {
                            chunk.setBlockState(pos, waterState, false);
                        }
                        continue;
                    }

                    if (noiseRouter.isCave(x, y, z)) {
                        continue; // leave as air — carves out the cave
                    }

                    if (y == surfaceY) {
                        chunk.setBlockState(pos, grassState, false);
                    } else if (y > surfaceY - 4) {
                        chunk.setBlockState(pos, dirtState, false);
                    } else {
                        chunk.setBlockState(pos, stoneState, false);
                    }
                }
            }
        }

        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public int getSeaLevel() {
        return GHNoiseRouter.SEA_LEVEL;
    }

    @Override
    public int getMinY() {
        return GHNoiseRouter.MIN_HEIGHT;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState randomState) {
        return noiseRouter.getSurfaceHeight(x, z) + 1;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState randomState) {
        int surfaceY = noiseRouter.getSurfaceHeight(x, z);
        BlockState[] states = new BlockState[level.getHeight()];

        for (int i = 0; i < states.length; i++) {
            int y = level.getMinBuildHeight() + i;
            if (y > surfaceY) {
                states[i] = y <= GHNoiseRouter.SEA_LEVEL ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
            } else if (noiseRouter.isCave(x, y, z)) {
                states[i] = Blocks.AIR.defaultBlockState();
            } else if (y == surfaceY) {
                states[i] = Blocks.GRASS_BLOCK.defaultBlockState();
            } else if (y > surfaceY - 4) {
                states[i] = Blocks.DIRT.defaultBlockState();
            } else {
                states[i] = Blocks.STONE.defaultBlockState();
            }
        }

        return new NoiseColumn(level.getMinBuildHeight(), states);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState randomState, BlockPos pos) {
        info.add("GH TerraForma — surface: " + noiseRouter.getSurfaceHeight(pos.getX(), pos.getZ()));
    }
}
