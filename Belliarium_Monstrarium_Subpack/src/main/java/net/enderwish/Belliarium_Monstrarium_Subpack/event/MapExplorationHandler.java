package net.enderwish.Belliarium_Monstrarium_Subpack.event;

import net.enderwish.Belliarium_Monstrarium_Subpack.core.map.MapWorldData;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * MapExplorationHandler
 *
 * Marks the chunk a player is standing in as explored, so it shows up on
 * the map. Runs on the server tick so exploration is tracked server-side
 * and shared between all players in the world.
 *
 * Registered on PlayerTickEvent.Post (a concrete subclass) because
 * PlayerTickEvent itself is abstract and NeoForge rejects listeners on it.
 */
@EventBusSubscriber(modid = "gh_belliarium_monstrarium")
public class MapExplorationHandler {

    private static long lastTick = -1;

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
        int surfaceHeight = (int) player.getY();

        MapWorldData.get(level).markExplored(cx, cz, surfaceHeight);
    }
}
