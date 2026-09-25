package net.enderwish.Frontier_Subpack.event;

import net.enderwish.Frontier_Subpack.FrontierConfig;
import net.enderwish.Frontier_Subpack.FrontierSubpack;
import net.enderwish.TerraForma_Subpack.api.WorldAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ExoticSpawnHandler
 *
 * DIAGNOSTIC LOGGING added this round. Most likely cause of "does
 * nothing": EXOTIC_START was reading from a CLIENT-type config inside
 * server-side event code -- fixed in FrontierConfig this round. These
 * log lines exist so that if it's STILL wrong after that fix, the next
 * report comes with real evidence instead of another guess: did the
 * event even fire, what did the config actually read as, and what
 * position/biome did the search land on. Check latest.log for lines
 * tagged GHFrontier/ExoticSpawn after testing.
 */
@EventBusSubscriber(modid = FrontierSubpack.MODID, bus = EventBusSubscriber.Bus.GAME)
public class ExoticSpawnHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("GHFrontier/ExoticSpawn");

    @SubscribeEvent
    public static void onCreateSpawnPosition(LevelEvent.CreateSpawnPosition event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (level.dimension() != ServerLevel.OVERWORLD) return;

        boolean enabled = FrontierConfig.EXOTIC_START.get();
        LOGGER.info("CreateSpawnPosition fired for overworld -- Exotic Start enabled: {}", enabled);

        if (!enabled) return;

        BlockPos exoticSpawn = WorldAPI.findExoticSpawnPosition(level.getRandom());
        LOGGER.info("Exotic Start -- forcing spawn to {}", exoticSpawn);

        event.getSettings().setSpawn(exoticSpawn, 0.0f);
        event.setCanceled(true);
    }
}
