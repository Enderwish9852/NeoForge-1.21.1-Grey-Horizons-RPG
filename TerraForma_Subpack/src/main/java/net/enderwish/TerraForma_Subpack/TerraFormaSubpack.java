package net.enderwish.TerraForma_Subpack;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.monster.Monster;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;

/**
 * TerraFormaSubpack
 *
 * Main mod class for GH TerraForma — world generation, biomes, climate.
 *
 * Globally cancels ALL vanilla hostile mob spawns.
 * GH Combat subpack registers its own monsters (gh_combat namespace)
 * which are exempt from this cancellation.
 */
@Mod(TerraFormaSubpack.MODID)
@EventBusSubscriber(modid = TerraFormaSubpack.MODID,
        bus = EventBusSubscriber.Bus.GAME)
public class TerraFormaSubpack {

    public static final String MODID = "gh_terraforma";

    public TerraFormaSubpack(IEventBus modEventBus, ModContainer container) {
        // Systems registered here as we build them
    }

    /**
     * Cancels ALL vanilla hostile mob spawns globally.
     *
     * Fires before the entity is instantiated — checks EntityType directly.
     * Only blocks entities from the "minecraft" namespace.
     * GH monsters (gh_combat namespace) pass through unaffected.
     *
     * Uses SpawnPlacementCheck — the correct NeoForge 1.21.1 hook for
     * pre-spawn cancellation. DENY result fully prevents the spawn.
     */
    @SubscribeEvent
    public static void onMobSpawn(MobSpawnEvent.SpawnPlacementCheck event) {
        String modid = BuiltInRegistries.ENTITY_TYPE
                .getKey(event.getEntityType())
                .getNamespace();

        if (modid.equals("minecraft")) {
            if (Monster.class.isAssignableFrom(
                    event.getEntityType().getBaseClass())) {
                event.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.FAIL);
            }
        }
    }
}
