package net.enderwish.HUD_Visuals_Subpack.event;

import net.enderwish.HUD_Visuals_Subpack.core.Season;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = "hud_visuals_subpack")
public class GlobalTemperatureModifier {

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            Season currentSeason = SeasonManager.getSeason(level);

            // If it's winter, we want to make the world "cold" so rain becomes snow
            if (currentSeason == Season.WINTER) {
                // This doesn't change the biome files, just the current level's rendering logic
                if (level.isRaining() && level.getGameTime() % 100 == 0) {
                    // Force the level to recognize it's cold enough for snow
                    // Note: This is a simplified logic. In 1.21.1, the best way
                    // is to use the 'getTemperature' hook below.
                }
            }
        }
    }
}