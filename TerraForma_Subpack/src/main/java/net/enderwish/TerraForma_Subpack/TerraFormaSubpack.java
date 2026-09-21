package net.enderwish.TerraForma_Subpack;

import net.enderwish.TerraForma_Subpack.core.worldgen.ModWorldgen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.monster.Monster;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;

@Mod(TerraFormaSubpack.MODID)
@EventBusSubscriber(modid = TerraFormaSubpack.MODID,
        bus = EventBusSubscriber.Bus.GAME)
public class TerraFormaSubpack {

    public static final String MODID = "gh_terraforma";

    public TerraFormaSubpack(IEventBus modEventBus, ModContainer container) {
        ModWorldgen.register(modEventBus);
    }

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
