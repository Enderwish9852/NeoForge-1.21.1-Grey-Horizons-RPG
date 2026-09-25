package net.enderwish.TerraForma_Subpack.core.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.enderwish.TerraForma_Subpack.core.biome.GHBiomeSource;
import net.enderwish.TerraForma_Subpack.core.climate.ClimateMap;
import net.enderwish.TerraForma_Subpack.core.worldgen.noise.BiomeTerrainProfile;
import net.enderwish.TerraForma_Subpack.core.worldgen.noise.GHNoiseRouter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.WorldGenRegion;
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
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.StructureSet;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * GHChunkGenerator
 *
 * UPDATED this round: every isCave(...) call now passes the column's
 * real BiomeTerrainProfile (already computed via profileAt for the
 * height itself) instead of the old neutral-defaulting overload --
 * see GHNoiseRouter's own doc comment for the underground-mess bug
 * this closes.
 */
public class GHChunkGenerator extends ChunkGenerator {

    public static final MapCodec<GHChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(GHChunkGenerator::getBiomeSource)
            ).apply(instance, GHChunkGenerator::new)
    );

    private final GHNoiseRouter noiseRouter = GHNoiseRouter.INSTANCE;

    public GHChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public ChunkGeneratorStructureState createState(HolderLookup<StructureSet> structureSetLookup,
                                                    RandomState randomState, long seed) {
        ClimateMap.INSTANCE.setSeed(seed);
        GHNoiseRouter.INSTANCE.setSeed(seed);
        return super.createState(structureSetLookup, randomState, seed);
    }

    private BiomeTerrainProfile profileAt(int x, int z) {
        int roughHeight = noiseRouter.getSurfaceHeight(x, z, BiomeTerrainProfile.NEUTRAL);
        return BiomeTerrainRegistry.get(GHBiomeSource.getBiomeKeyAt(x, roughHeight, z));
    }

    @Override
    public void applyCarvers(WorldGenRegion level, long seed, RandomState randomState,
                             BiomeManager biomeManager, StructureManager structureManager,
                             ChunkAccess chunk, GenerationStep.Carving step) {
        // No-op -- GHNoiseRouter's own cave noise carves inside fillFromNoise.
    }

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager,
                             RandomState randomState, ChunkAccess chunk) {
        // Surface blocks placed directly in fillFromNoise for now.
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion level) {
        // TerraFormaSubpack already globally cancels vanilla hostile spawns.
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
                BiomeTerrainProfile profile = profileAt(x, z);
                int surfaceY = noiseRouter.getSurfaceHeight(x, z, profile);

                for (int y = GHNoiseRouter.MIN_HEIGHT; y <= Math.max(surfaceY, GHNoiseRouter.SEA_LEVEL); y++) {
                    BlockPos pos = new BlockPos(x, y, z);

                    if (y > surfaceY) {
                        if (y <= GHNoiseRouter.SEA_LEVEL) {
                            chunk.setBlockState(pos, waterState, false);
                        }
                        continue;
                    }

                    if (noiseRouter.isCave(x, y, z, profile)) {
                        continue;
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
        return noiseRouter.getSurfaceHeight(x, z, profileAt(x, z)) + 1;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState randomState) {
        BiomeTerrainProfile profile = profileAt(x, z);
        int surfaceY = noiseRouter.getSurfaceHeight(x, z, profile);
        BlockState[] states = new BlockState[level.getHeight()];

        for (int i = 0; i < states.length; i++) {
            int y = level.getMinBuildHeight() + i;
            if (y > surfaceY) {
                states[i] = y <= GHNoiseRouter.SEA_LEVEL ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
            } else if (noiseRouter.isCave(x, y, z, profile)) {
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
        info.add("GH TerraForma -- surface: "
                + noiseRouter.getSurfaceHeight(pos.getX(), pos.getZ(), profileAt(pos.getX(), pos.getZ())));
    }
}
