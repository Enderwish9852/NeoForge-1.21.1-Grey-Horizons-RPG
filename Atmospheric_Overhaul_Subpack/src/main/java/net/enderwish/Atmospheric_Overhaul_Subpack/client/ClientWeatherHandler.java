package net.enderwish.Atmospheric_Overhaul_Subpack.client;

import net.enderwish.Atmospheric_Overhaul_Subpack.AtmosphericOverhaulSubpack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * ClientWeatherHandler
 *
 * Tracks the player's current biome category every 2 seconds.
 * Used by WeatherParticleMixin to suppress rain particles in hot biomes.
 *
 * Does NOT touch setRainLevel or any vanilla weather state —
 * vanilla handles clouds, darkness, and sound naturally.
 */
@EventBusSubscriber(modid = AtmosphericOverhaulSubpack.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ClientWeatherHandler {

    // Check every 40 ticks = 2 seconds
    private static final int CHECK_INTERVAL = 40;
    private static int tickCounter = 0;

    private static BiomeCategory currentCategory = BiomeCategory.TEMPERATE;

    // ── Tick ──────────────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        tickCounter++;
        if (tickCounter < CHECK_INTERVAL) return;
        tickCounter = 0;

        // Just update the category — never touch weather state
        currentCategory = getBiomeCategory(mc.level, mc.player);
    }

    // ── Biome categorisation ──────────────────────────────────────────────────

    public enum BiomeCategory {
        HOT,        // desert, savanna, badlands — no precipitation particles
        TEMPERATE,  // plains, forest — rain particles normal
        COLD        // taiga, snowy plains — snow particles
    }

    private static BiomeCategory getBiomeCategory(ClientLevel level, Player player) {
        BlockPos pos = player.blockPosition();
        float temp = level.getBiome(pos).value().getBaseTemperature();

        if (temp >= 1.0f) return BiomeCategory.HOT;
        if (temp <= 0.15f) return BiomeCategory.COLD;
        return BiomeCategory.TEMPERATE;
    }

    // ── Public getters ────────────────────────────────────────────────────────

    /** Returns the current player's biome category. */
    public static BiomeCategory getCurrentCategory() {
        return currentCategory;
    }

    /** True if precipitation particles should be visible. */
    public static boolean isPrecipitationVisible() {
        return currentCategory != BiomeCategory.HOT
                && ClientSeasonState.isPrecipitating();
    }
}