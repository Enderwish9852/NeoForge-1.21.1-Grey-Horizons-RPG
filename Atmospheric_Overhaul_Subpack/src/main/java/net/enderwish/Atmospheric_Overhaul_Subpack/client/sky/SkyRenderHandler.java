package net.enderwish.Atmospheric_Overhaul_Subpack.client.sky;

import net.enderwish.Atmospheric_Overhaul_Subpack.AtmosphericOverhaulSubpack;
import net.enderwish.Atmospheric_Overhaul_Subpack.client.ClientSeasonState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
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
 *
 * Water ripples used to be spawned from here as an AMBIENT effect: one
 * ring every 4 ticks at a random spot within 14 blocks of the player,
 * with nothing having touched the water. That was the "ripples just
 * random spawn" the user saw, and it is gone. Ripples are now spawned
 * by whatever actually hits the surface:
 *   - WaterImpactHandler: an item or an entity crossing the surface;
 *   - RainDropParticle: a raindrop reaching the water.
 */
@EventBusSubscriber(modid = AtmosphericOverhaulSubpack.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class SkyRenderHandler {

    // ── Underwater murk ──────────────────────────────────────────────────

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level instanceof ClientLevel level)) return;
        if (!mc.options.getCameraType().isFirstPerson() || mc.player == null) return;

        if (isEyeInWater(mc.player)) {
            // dense, short fog that thickens with depth below the
            // surface. It was thick enough to hide the shafts and the
            // bed: the far plane now reaches a good deal further, and
            // the tint is a little less heavy, so the water still reads
            // as water without being a wall.
            event.setNearPlaneDistance(0.0f);
            event.setFarPlaneDistance(22.0f + 16.0f * mc.player.getAirSupply() / 300f);
            event.setCanceled(true);
            return;
        }

        // THE RAIN'S OWN HAZE. A downpour pulls the world in: the far
        // plane comes in as the rain gets heavier, so you can see the
        // rain rather than the far side of the map. It is a soft
        // shortening of the distance, not a wall.
        float rain = rainIntensity(level);
        if (rain > 0.01f) {
            float base = mc.options.getEffectiveRenderDistance() * 16.0f;
            event.setNearPlaneDistance(0.0f);
            event.setFarPlaneDistance(Mth.lerp(rain, base, base * 0.45f));
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        if (isEyeInWater(mc.player)) {
            // deep blue-green; darker the deeper the camera sits
            event.setRed(event.getRed() * 0.46f + 0.02f);
            event.setGreen(event.getGreen() * 0.66f + 0.06f);
            event.setBlue(event.getBlue() * 0.72f + 0.10f);
            return;
        }

        // the rain's haze is a cold grey, and it thickens with the rain
        float rain = rainIntensity(mc.level);
        if (rain > 0.01f) {
            event.setRed(Mth.lerp(rain, event.getRed(), 0.60f));
            event.setGreen(Mth.lerp(rain, event.getGreen(), 0.63f));
            event.setBlue(Mth.lerp(rain, event.getBlue(), 0.68f));
        }
    }

    /** How hard it is raining, 0..1, from the weather system or vanilla. */
    private static float rainIntensity(ClientLevel level) {
        float rain = level.getRainLevel(1.0f);
        if (ClientSeasonState.isPrecipitating()) {
            rain = Math.max(rain, ClientSeasonState.getIntensity());
        }
        return Mth.clamp(rain, 0.0f, 1.0f);
    }

    private static boolean isEyeInWater(LocalPlayer player) {
        return player.isEyeInFluidType(NeoForgeMod.WATER_TYPE.value());
    }
}
