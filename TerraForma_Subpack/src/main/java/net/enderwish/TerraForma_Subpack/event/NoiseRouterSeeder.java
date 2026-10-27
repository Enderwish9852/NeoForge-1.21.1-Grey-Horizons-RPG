package net.enderwish.TerraForma_Subpack.event;

import net.enderwish.TerraForma_Subpack.core.worldgen.noise.GHNoiseRouter;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.WorldData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;

/**
 * NoiseRouterSeeder
 *
 * GHNoiseRouter never had setSeed() called anywhere, so it always fell
 * back to the hard-coded 12345L default. Client and server happened to
 * agree (both used 12345L), but the water current didn't vary by world
 * seed. Hook the level load event and seed the router with the real
 * level seed so every world gets its own current pattern.
 */
@EventBusSubscriber(modid = "gh_terraforma", bus = EventBusSubscriber.Bus.GAME)
public class NoiseRouterSeeder {

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        LevelData data = event.getLevel().getLevelData();
        if (data instanceof WorldData worldData) {
            GHNoiseRouter.INSTANCE.setSeed(worldData.worldGenOptions().seed());
        }
    }
}
