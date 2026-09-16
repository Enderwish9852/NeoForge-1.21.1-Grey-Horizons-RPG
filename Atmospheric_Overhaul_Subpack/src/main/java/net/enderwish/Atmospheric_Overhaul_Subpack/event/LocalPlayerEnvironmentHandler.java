package net.enderwish.Atmospheric_Overhaul_Subpack.event;

import net.enderwish.Atmospheric_Overhaul_Subpack.core.season.LocalChunkTemperature;
import net.enderwish.Atmospheric_Overhaul_Subpack.core.weather.LocalWindCalculator;
import net.enderwish.Atmospheric_Overhaul_Subpack.core.weather.WindDirection;
import net.enderwish.Atmospheric_Overhaul_Subpack.core.weather.WindManager;
import net.enderwish.Atmospheric_Overhaul_Subpack.network.LocalPlayerEnvironmentSyncPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * LocalPlayerEnvironmentHandler
 *
 * Periodically (every 10 ticks / 0.5s — NOT every tick, matching
 * SeasonEnvironmentHandler's own interval-gating for the same cost
 * reason) computes each online player's felt wind and feels-like
 * temperature, then syncs both to that specific player.
 */
public class LocalPlayerEnvironmentHandler {

    private static final int BASE_INTERVAL = 10;
    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (level.dimension() != ServerLevel.OVERWORLD) return;

        tickCounter++;
        if (tickCounter % BASE_INTERVAL != 0) return;

        WindDirection globalDirection = WindManager.INSTANCE.getDirection(level);
        float globalSpeed = WindManager.INSTANCE.getSpeed(level);

        for (ServerPlayer player : level.players()) {
            BlockPos pos = player.blockPosition();

            float feltSpeed = LocalWindCalculator.calculateFeltSpeed(
                    level, pos, globalDirection, globalSpeed);

            float feelsLike = LocalChunkTemperature.calculate(level, pos);

            LocalPlayerEnvironmentSyncPacket.sendToPlayer(
                    player, globalDirection, feltSpeed, feelsLike);
        }
    }
}
