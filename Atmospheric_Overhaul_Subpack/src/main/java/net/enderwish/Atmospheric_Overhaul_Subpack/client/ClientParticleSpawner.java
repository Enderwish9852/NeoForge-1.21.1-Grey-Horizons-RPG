package net.enderwish.Atmospheric_Overhaul_Subpack.client;

import net.enderwish.Atmospheric_Overhaul_Subpack.AtmosphericOverhaulSubpack;
import net.enderwish.Atmospheric_Overhaul_Subpack.client.particle.ModParticles;
import net.enderwish.Atmospheric_Overhaul_Subpack.core.season.SeasonCalendar;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * ClientParticleSpawner
 *
 * Spawns ALL weather/season-driven ambient particles near the player.
 * Rain unchanged from before. Three new blocks added this round:
 *
 *   - Swirling leaves: autumn season only, sparse, open-sky columns.
 *   - Dust motes: hot + non-precipitating biomes (reuses
 *     ClientWeatherHandler's existing HOT/TEMPERATE/COLD category —
 *     no dependency on TerraForma, stays self-contained in Atmospheric).
 *   - Fog wisps: only when active weather ID is literally "fog".
 *
 * All three are deliberately simple v1 triggers (season/biome-category/
 * weather-id gates + a random chance per tick) rather than anything
 * spatially precise (e.g. leaves don't yet check for actual nearby tree
 * blocks) — good enough to confirm they're visible, refinable later.
 */
@EventBusSubscriber(modid = AtmosphericOverhaulSubpack.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ClientParticleSpawner {

    private static final int BASE_RAIN_COUNT    = 20;
    private static final int SPAWN_RADIUS       = 40;
    private static final int SPAWN_HEIGHT_ABOVE = 15;
    private static final float UPWIND_BIAS_STRENGTH = 0.3f;

    private static final int LEAVES_SPAWN_RADIUS = 20;
    private static final int DUST_SPAWN_RADIUS   = 24;
    private static final int FOG_SPAWN_RADIUS    = 18;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if (mc.isPaused()) return;

        spawnRain(mc.level, mc.player);
        spawnSwirlingLeaves(mc.level, mc.player);
        spawnDustMotes(mc.level, mc.player);
        spawnFogWisps(mc.level, mc.player);
    }

    // ── Rain (unchanged) ──────────────────────────────────────────────────────

    private static void spawnRain(ClientLevel level, Player player) {
        if (!ClientSeasonState.isPrecipitating()) return;

        float intensity = ClientSeasonState.getIntensity();
        if (intensity <= 0f) return;

        int count = Math.round(BASE_RAIN_COUNT * intensity);
        if (count <= 0) return;

        RandomSource random = level.getRandom();
        BlockPos playerPos = player.blockPosition();

        float windDx = ClientSeasonState.getWindDx();
        float windDz = ClientSeasonState.getWindDz();
        float windMag = (float) Math.sqrt(windDx * windDx + windDz * windDz);
        float upwindX = windMag > 1.0E-4f ? -windDx / windMag : 0f;
        float upwindZ = windMag > 1.0E-4f ? -windDz / windMag : 0f;

        for (int i = 0; i < count; i++) {
            int rawDx = random.nextInt(SPAWN_RADIUS * 2) - SPAWN_RADIUS;
            int rawDz = random.nextInt(SPAWN_RADIUS * 2) - SPAWN_RADIUS;

            int dx = (int) Mth.lerp(UPWIND_BIAS_STRENGTH, rawDx, upwindX * SPAWN_RADIUS);
            int dz = (int) Mth.lerp(UPWIND_BIAS_STRENGTH, rawDz, upwindZ * SPAWN_RADIUS);

            int colX = playerPos.getX() + dx;
            int colZ = playerPos.getZ() + dz;
            int topY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, colX, colZ);

            if (!level.canSeeSky(new BlockPos(colX, topY, colZ))) continue;

            double x = colX + random.nextDouble();
            double z = colZ + random.nextDouble();
            double y = player.getY() + SPAWN_HEIGHT_ABOVE + random.nextDouble() * 5.0;

            level.addParticle(ModParticles.RAIN_DROP.get(), x, y, z, 0.0, intensity, 0.0);
        }
    }

    // ── Swirling leaves — autumn only ─────────────────────────────────────────

    private static void spawnSwirlingLeaves(ClientLevel level, Player player) {
        if (ClientSeasonState.getSeason() != SeasonCalendar.Season.AUTUMN) return;

        RandomSource random = level.getRandom();
        if (random.nextFloat() > 0.3f) return; // sparse — not every tick

        BlockPos playerPos = player.blockPosition();
        int dx = random.nextInt(LEAVES_SPAWN_RADIUS * 2) - LEAVES_SPAWN_RADIUS;
        int dz = random.nextInt(LEAVES_SPAWN_RADIUS * 2) - LEAVES_SPAWN_RADIUS;
        int colX = playerPos.getX() + dx;
        int colZ = playerPos.getZ() + dz;
        int topY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, colX, colZ);

        if (!level.canSeeSky(new BlockPos(colX, topY, colZ))) return;

        double x = colX + random.nextDouble();
        double z = colZ + random.nextDouble();
        double y = player.getY() + 4 + random.nextDouble() * 8.0;

        level.addParticle(ModParticles.SWIRLING_LEAVES.get(), x, y, z, 0.0, 0.0, 0.0);
    }

    // ── Dust motes — hot, non-precipitating biomes ────────────────────────────

    private static void spawnDustMotes(ClientLevel level, Player player) {
        if (ClientWeatherHandler.getCurrentCategory() != ClientWeatherHandler.BiomeCategory.HOT) return;
        if (ClientSeasonState.isPrecipitating()) return;

        RandomSource random = level.getRandom();
        if (random.nextFloat() > 0.5f) return;

        BlockPos playerPos = player.blockPosition();
        int dx = random.nextInt(DUST_SPAWN_RADIUS * 2) - DUST_SPAWN_RADIUS;
        int dz = random.nextInt(DUST_SPAWN_RADIUS * 2) - DUST_SPAWN_RADIUS;
        int colX = playerPos.getX() + dx;
        int colZ = playerPos.getZ() + dz;
        int topY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, colX, colZ);

        double x = colX + random.nextDouble();
        double z = colZ + random.nextDouble();
        double y = topY + 1 + random.nextDouble() * 3.0;

        level.addParticle(ModParticles.DUST_MOTE.get(), x, y, z, 0.0, 0.0, 0.0);
    }

    // ── Fog wisps — fog weather only ──────────────────────────────────────────

    private static void spawnFogWisps(ClientLevel level, Player player) {
        if (!ClientSeasonState.getWeatherId().equals("fog")) return;

        RandomSource random = level.getRandom();
        if (random.nextFloat() > 0.4f) return;

        BlockPos playerPos = player.blockPosition();
        int dx = random.nextInt(FOG_SPAWN_RADIUS * 2) - FOG_SPAWN_RADIUS;
        int dz = random.nextInt(FOG_SPAWN_RADIUS * 2) - FOG_SPAWN_RADIUS;
        int colX = playerPos.getX() + dx;
        int colZ = playerPos.getZ() + dz;
        int topY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, colX, colZ);

        double x = colX + random.nextDouble();
        double z = colZ + random.nextDouble();
        double y = topY + 1 + random.nextDouble() * 2.0;

        level.addParticle(ModParticles.FOG_WISP.get(), x, y, z, 0.0, 0.0, 0.0);
    }
}
