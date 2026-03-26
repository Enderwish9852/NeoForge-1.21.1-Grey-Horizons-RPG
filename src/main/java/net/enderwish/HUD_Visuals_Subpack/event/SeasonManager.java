package net.enderwish.HUD_Visuals_Subpack.event;

import net.enderwish.HUD_Visuals_Subpack.core.SeasonData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Handles the logic for updating seasons over time in NeoForge 1.21.1.
 * This runs on the Server side to keep the "World Clock" synchronized.
 */
@EventBusSubscriber(modid = "hud_visuals_subpack")
public class SeasonManager {

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        Level level = event.getLevel();

        // 1. Only run on the server side (isClientSide == false)
        // 2. Only run for the Overworld
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            if (level.dimension() == Level.OVERWORLD) {

                // Get our data and increment the tick
                SeasonData data = SeasonData.get(serverLevel);
                data.tick(serverLevel);

                // Sync to clients every 20 ticks (1 second)
                if (level.getGameTime() % 20 == 0) {
                    // Packet logic will go here once we fix the Packet class
                }
            }
        }
    }
}