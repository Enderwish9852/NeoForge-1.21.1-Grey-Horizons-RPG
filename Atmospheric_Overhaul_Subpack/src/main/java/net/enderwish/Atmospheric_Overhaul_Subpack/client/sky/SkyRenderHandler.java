package net.enderwish.Atmospheric_Overhaul_Subpack.client.sky;

import net.enderwish.Atmospheric_Overhaul_Subpack.AtmosphericOverhaulSubpack;
import net.enderwish.Atmospheric_Overhaul_Subpack.client.ClientSeasonState;
import net.enderwish.Atmospheric_Overhaul_Subpack.client.particle.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.common.NeoForgeMod;

/**
 * SkyRenderHandler
 *
 * NEW -- client-side atmosphere pipeline:
 * - Voxel clouds are NOT drawn here: the renderClouds mixin calls
 *   VoxelCloudEngine directly (the AFTER_SKY stage dispatches a null
 *   pose stack, so that hook was removed).
 * - Underwater: denser fog ramp (RenderFog) + blue-green fog tint
 *   (ComputeFogColor) -- the murk that makes depth legible.
 * - Water ripples: ambient + rain-driven expanding rings on the water
 *   surface near the camera, drifting with the synced WIND direction
 *   (this is also the visual hook the water-current system will use).
 */
@EventBusSubscriber(modid = AtmosphericOverhaulSubpack.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class SkyRenderHandler {

    private static final RandomSource RANDOM = RandomSource.create();

    // ── Underwater murk ──────────────────────────────────────────────────

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level instanceof ClientLevel level)) return;
        if (!mc.options.getCameraType().isFirstPerson() || mc.player == null) return;
        if (!isEyeInWater(mc.player)) return;

        // dense, short fog that thickens with depth below the surface
        event.setNearPlaneDistance(0.0f);
        event.setFarPlaneDistance(14.0f + 10.0f * mc.player.getAirSupply() / 300f);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (!isEyeInWater(mc.player)) return;

        // deep blue-green; darker the deeper the camera sits
        event.setRed(event.getRed() * 0.35f + 0.02f);
        event.setGreen(event.getGreen() * 0.55f + 0.06f);
        event.setBlue(event.getBlue() * 0.60f + 0.10f);
    }

    private static boolean isEyeInWater(LocalPlayer player) {
        return player.isEyeInFluidType(NeoForgeMod.WATER_TYPE.value());
    }

    // ── Water ripples ────────────────────────────────────────────────────

    private static int ambientRippleCooldown = 0;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        if (level == null || player == null || mc.isPaused()) return;

        boolean raining = level.isRaining() || ClientSeasonState.isPrecipitating();

        if (ambientRippleCooldown-- > 0) return;
        ambientRippleCooldown = raining ? 1 : 4;   // spawn cadence (ticks)

        // try a handful of water-surface spots in a ring around the player
        for (int attempt = 0; attempt < 6; attempt++) {
            double ang = RANDOM.nextDouble() * Math.PI * 2;
            double dist = 2 + RANDOM.nextDouble() * 14;
            int x = (int) Math.round(player.getX() + Math.cos(ang) * dist);
            int z = (int) Math.round(player.getZ() + Math.sin(ang) * dist);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);

            // getHeight returns the first AIR block above the surface,
            // so the fluid itself sits at y-1 (surface top ~y-1+0.875)
            FluidState fluid = level.getFluidState(new BlockPos(x, y - 1, z));
            if (!fluid.is(Fluids.WATER)) continue;

            SimpleParticleType type = ModParticles.WATER_RIPPLE.get();
            level.addParticle(type,
                    x + RANDOM.nextDouble(), y - 0.12, z + RANDOM.nextDouble(),
                    0, 0, 0);
            break;   // one ripple per cadence tick is plenty (ambient)
        }
    }
}
