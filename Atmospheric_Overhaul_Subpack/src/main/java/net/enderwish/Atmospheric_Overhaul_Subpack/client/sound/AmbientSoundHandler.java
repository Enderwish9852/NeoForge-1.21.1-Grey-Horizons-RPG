package net.enderwish.Atmospheric_Overhaul_Subpack.client.sound;

import net.enderwish.Atmospheric_Overhaul_Subpack.AtmosphericOverhaulSubpack;
import net.enderwish.Atmospheric_Overhaul_Subpack.client.ClientSeasonState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * BUGFIX: wind threshold (0.4) sat above where typical non-storm wind
 * speeds usually roll — meaning wind ambience almost only ever
 * triggered during active weather, exactly when rain ambience was
 * also playing and masking it underneath its own louder volume curve.
 * Lowered the threshold and matched the volume curve's range to rain's
 * so wind is audible on its own in normal (non-storm) conditions too.
 */
@EventBusSubscriber(modid = AtmosphericOverhaulSubpack.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class AmbientSoundHandler {

    private static final int CLIP_LENGTH_TICKS = 400;
    private static int rainTimer = 0;
    private static int windTimer = 0;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused()) return;

        if (ClientSeasonState.isPrecipitating()) {
            if (rainTimer <= 0) {
                float volume = 0.3f + ClientSeasonState.getIntensity() * 0.5f;
                mc.getSoundManager().play(SimpleSoundInstance.forLocalAmbience(ModSounds.RAIN_AMBIENCE.get(), volume, 1.0f));
                rainTimer = CLIP_LENGTH_TICKS;
            } else rainTimer--;
        } else rainTimer = 0;

        float windSpeed = ClientSeasonState.getWindSpeed();
        if (windSpeed > 0.15f) {
            if (windTimer <= 0) {
                float volume = 0.25f + windSpeed * 0.55f;
                mc.getSoundManager().play(SimpleSoundInstance.forLocalAmbience(ModSounds.WIND_HOWL.get(), volume, 1.0f));
                windTimer = CLIP_LENGTH_TICKS;
            } else windTimer--;
        } else windTimer = 0;
    }
}
