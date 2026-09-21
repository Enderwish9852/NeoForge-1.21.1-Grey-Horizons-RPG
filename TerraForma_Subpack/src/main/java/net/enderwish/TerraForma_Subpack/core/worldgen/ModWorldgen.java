package net.enderwish.TerraForma_Subpack.core.worldgen;

import com.mojang.serialization.MapCodec;
import net.enderwish.TerraForma_Subpack.TerraFormaSubpack;
import net.enderwish.TerraForma_Subpack.core.biome.GHBiomeSource;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * ModWorldgen
 *
 * BUGFIX: the previous version called Registry.register() directly on
 * BuiltInRegistries.CHUNK_GENERATOR/BIOME_SOURCE from inside
 * TerraFormaSubpack's own constructor — but vanilla's bootstrap
 * process freezes these registries before FML constructs mods.
 * Confirmed by the actual crash: "Registry is already frozen" at
 * exactly that call site — exactly the risk this file's previous
 * version flagged in its own comment.
 *
 * FIX: DeferredRegister, same pattern as ModBlocks/ModItems/
 * ModParticles elsewhere in this project — NeoForge fires this
 * through its own RegisterEvent machinery, timed correctly for these
 * registries, unlike a raw eager call in a constructor body.
 *
 * VERIFY IF COMPILE FAILS — Registries.CHUNK_GENERATOR and
 * Registries.BIOME_SOURCE (the ResourceKey constants, distinct from
 * BuiltInRegistries.CHUNK_GENERATOR/BIOME_SOURCE which are the
 * resolved Registry instances I've already confirmed from
 * ChunkGenerator.java/BiomeSource.java) are based on Registries
 * normally mirroring BuiltInRegistries 1:1 — not directly confirmed
 * against Registries.java itself this round.
 */
public class ModWorldgen {

    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, TerraFormaSubpack.MODID);

    public static final DeferredRegister<MapCodec<? extends BiomeSource>> BIOME_SOURCES =
            DeferredRegister.create(Registries.BIOME_SOURCE, TerraFormaSubpack.MODID);

    public static final DeferredHolder<MapCodec<? extends ChunkGenerator>, MapCodec<GHChunkGenerator>> GH_CHUNK_GENERATOR =
            CHUNK_GENERATORS.register("gh_chunk_generator", () -> GHChunkGenerator.CODEC);

    public static final DeferredHolder<MapCodec<? extends BiomeSource>, MapCodec<GHBiomeSource>> GH_BIOME_SOURCE =
            BIOME_SOURCES.register("gh_biome_source", () -> GHBiomeSource.CODEC);

    public static void register(IEventBus modEventBus) {
        CHUNK_GENERATORS.register(modEventBus);
        BIOME_SOURCES.register(modEventBus);
    }
}
