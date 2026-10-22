package net.enderwish.TerraForma_Subpack.core.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.enderwish.TerraForma_Subpack.core.biome.GHBiomeSource;
import net.enderwish.TerraForma_Subpack.core.climate.ClimateMap;
import net.enderwish.TerraForma_Subpack.core.worldgen.noise.BiomeTerrainProfile;
import net.enderwish.TerraForma_Subpack.core.worldgen.noise.GHNoiseRouter;
import net.enderwish.TerraForma_Subpack.core.worldgen.river.RiverNetwork;
import net.enderwish.TerraForma_Subpack.core.worldgen.noise.SurfacePalette;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
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

    private ResourceKey<Biome> biomeKeyAt(int x, int z) {
        int roughHeight = noiseRouter.getSurfaceHeight(x, z, BiomeTerrainProfile.NEUTRAL);
        return GHBiomeSource.getBiomeKeyAt(x, roughHeight, z);
    }

    private BiomeTerrainProfile profileAt(int x, int z) {
        return BiomeTerrainRegistry.get(biomeKeyAt(x, z));
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

        BlockState waterState = Blocks.WATER.defaultBlockState();

        for (int x = minX; x < minX + 16; x++) {
            for (int z = minZ; z < minZ + 16; z++) {
                ResourceKey<Biome> biomeKey = biomeKeyAt(x, z);
                BiomeTerrainProfile profile = BiomeTerrainRegistry.get(biomeKey);
                SurfacePalette palette = SurfacePaletteRegistry.get(biomeKey);
                int surfaceY = noiseRouter.getSurfaceHeight(x, z, profile);

                // Is there a river here? If so the channel is cut into
                // the ground and the water stands in it, below the
                // banks so that it stays in its bed.
                RiverNetwork.RiverColumn river = RiverNetwork.columnAt(x, z);
                int waterLevel = GHNoiseRouter.SEA_LEVEL;
                if (river != null) {
                    surfaceY = river.bedY();
                    waterLevel = river.waterY();
                }

                for (int y = GHNoiseRouter.MIN_HEIGHT; y <= Math.max(surfaceY, waterLevel); y++) {
                    BlockPos pos = new BlockPos(x, y, z);

                    if (y > surfaceY) {
                        if (y <= waterLevel) {
                            chunk.setBlockState(pos, waterState, false);
                        }
                        continue;
                    }

                    if (noiseRouter.isCave(x, y, z, profile)) {
                        continue;
                    }

                    if (y == surfaceY) {
                        chunk.setBlockState(pos, palette.top(), false);
                    } else if (y > surfaceY - palette.subDepth()) {
                        chunk.setBlockState(pos, palette.sub(), false);
                    } else {
                        chunk.setBlockState(pos, palette.deep(), false);
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
        ResourceKey<Biome> biomeKey = biomeKeyAt(x, z);
        BiomeTerrainProfile profile = BiomeTerrainRegistry.get(biomeKey);
        SurfacePalette palette = SurfacePaletteRegistry.get(biomeKey);
        int surfaceY = noiseRouter.getSurfaceHeight(x, z, profile);
        BlockState[] states = new BlockState[level.getHeight()];

        for (int i = 0; i < states.length; i++) {
            int y = level.getMinBuildHeight() + i;
            if (y > surfaceY) {
                states[i] = y <= GHNoiseRouter.SEA_LEVEL ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
            } else if (noiseRouter.isCave(x, y, z, profile)) {
                states[i] = Blocks.AIR.defaultBlockState();
            } else if (y == surfaceY) {
                states[i] = palette.top();
            } else if (y > surfaceY - palette.subDepth()) {
                states[i] = palette.sub();
            } else {
                states[i] = palette.deep();
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
