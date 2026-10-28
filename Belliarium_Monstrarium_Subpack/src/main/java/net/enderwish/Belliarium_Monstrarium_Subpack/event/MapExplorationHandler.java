package net.enderwish.Belliarium_Monstrarium_Subpack.event;

import net.enderwish.Belliarium_Monstrarium_Subpack.core.map.MapWorldData;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * MapExplorationHandler
 *
 * Marks the chunk a player is standing in as explored, so it shows up on
 * the map. Runs on the server tick so exploration is tracked server-side
 * and shared between all players in the world.
 *
 * Also loads/saves map data to disk on level load/save.
 *
 * Registered on PlayerTickEvent.Post (a concrete subclass) because
 * PlayerTickEvent itself is abstract and NeoForge rejects listeners on it.
 */
@EventBusSubscriber(modid = "gh_belliarium_monstrarium")
public class MapExplorationHandler {

    private static long lastTick = -1;

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof Level level) {
            // Only run on server side - client level has no server for file I/O
            if (level.isClientSide()) {
                return;
            }
            MapWorldData.loadFor(level);
        }
    }

    @SubscribeEvent
    public static void onLevelSave(LevelEvent.Save event) {
        if (event.getLevel() instanceof Level level) {
            // Only run on server side - client level has no server for file I/O
            if (level.isClientSide()) {
                return;
            }
            MapWorldData.saveAll(level);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        Level level = player.level();
        if (level.isClientSide) return;

        long tick = level.getGameTime();
        if (tick == lastTick) return;
        lastTick = tick;

        int cx = SectionPos.blockToSectionCoord(player.getX());
        int cz = SectionPos.blockToSectionCoord(player.getZ());

        // Mark a 3x3 chunk area around the player as explored, so walking
        // through an area fills in the map faster than one chunk per tick.
        MapWorldData data = MapWorldData.get(level);
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                int wx = cx + dx;
                int wz = cz + dz;
                int surfaceHeight = (int) player.getY();
                data.markExplored(wx, wz, surfaceHeight);
            }
        }
    }
}
