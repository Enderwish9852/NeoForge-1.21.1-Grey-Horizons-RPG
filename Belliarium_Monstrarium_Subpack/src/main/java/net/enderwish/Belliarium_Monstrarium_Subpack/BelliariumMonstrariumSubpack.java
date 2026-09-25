package net.enderwish.Belliarium_Monstrarium_Subpack;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/**
 * BelliariumMonstrariumSubpack
 *
 * Combat and hostile-creature systems for Grey Horizons RPG. Vanilla
 * hostile spawns are already globally cancelled by TerraFormaSubpack's
 * own onMobSpawn listener (see TerraFormaSubpack.java) -- this subpack
 * is meant to become the ONLY source of hostile creatures once it has
 * any.
 *
 * Deliberately bare right now: no registries, no cross-subpack
 * dependencies. Wire those up here as real features land (entity
 * types, items, sounds, data components, event handlers), following
 * the same one-class-per-concern DeferredRegister pattern already
 * established in Farming_Overhaul_Subpack (ModBlocks / ModItems /
 * ModBlockEntities / ModMenuTypes, each registered from this
 * constructor).
 */
@Mod(BelliariumMonstrariumSubpack.MODID)
public class BelliariumMonstrariumSubpack {

    public static final String MODID = "gh_belliarium_monstrarium";

    public BelliariumMonstrariumSubpack(IEventBus modEventBus, ModContainer container) {
        // Registries wire up here as they're added.
    }
}
