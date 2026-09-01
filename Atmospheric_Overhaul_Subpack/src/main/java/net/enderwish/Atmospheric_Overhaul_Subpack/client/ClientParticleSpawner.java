package net.enderwish.Atmospheric_Overhaul_Subpack.client;

import net.enderwish.Atmospheric_Overhaul_Subpack.AtmosphericOverhaulSubpack;
import net.enderwish.Atmospheric_Overhaul_Subpack.client.particle.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
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
 * Spawns custom weather particles around the player each client tick.
 * Density scales directly with the server-rolled weather intensity
 * (synced via SeasonSyncPacket -> ClientSeasonState) — higher intensity
 * = more particles per tick = visually "thicker" rain.
 *
 * Skips spawning entirely while the game is paused (singleplayer pause
 * menu). ClientTickEvent still fires while paused, but ParticleEngine
 * stops ticking existing particles — without this guard, new particles
 * keep queuing up frozen in the sky and all fall at once on resume.
 *
 * TODO: currently spawns for ANY precipitating weather (rain or snow).
 * Once snow gets its own particle class, branch on biome temperature
 * (same check BiomeMixin uses) to pick rain vs snow here.
 */
@EventBusSubscriber(modid = AtmosphericOverhaulSubpack.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ClientParticleSpawner {

    private static final int BASE_RAIN_COUNT   = 20; // particles/tick at intensity 1.0
    private static final int SPAWN_RADIUS      = 12;
    private static final int SPAWN_HEIGHT_ABOVE = 15;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if (mc.isPaused()) return; // don't queue particles while the sim is frozen

        ClientLevel level = mc.level;
        Player player = mc.player;

        if (!ClientSeasonState.isPrecipitating()) return;

        float intensity = ClientSeasonState.getIntensity();
        if (intensity <= 0f) return;

        int count = Math.round(BASE_RAIN_COUNT * intensity);
        if (count <= 0) return;

        RandomSource random = level.getRandom();
        BlockPos playerPos = player.blockPosition();

        for (int i = 0; i < count; i++) {
            int dx = random.nextInt(SPAWN_RADIUS * 2) - SPAWN_RADIUS;
            int dz = random.nextInt(SPAWN_RADIUS * 2) - SPAWN_RADIUS;

            int colX = playerPos.getX() + dx;
            int colZ = playerPos.getZ() + dz;
            int topY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, colX, colZ);

            // Skip columns with no sky access — no indoor rain
            if (!level.canSeeSky(new BlockPos(colX, topY, colZ))) continue;

            double x = colX + random.nextDouble();
            double z = colZ + random.nextDouble();
            double y = player.getY() + SPAWN_HEIGHT_ABOVE + random.nextDouble() * 5.0;

            level.addParticle(ModParticles.RAIN_DROP.get(), x, y, z, 0.0, 0.0, 0.0);
        }
    }
}
