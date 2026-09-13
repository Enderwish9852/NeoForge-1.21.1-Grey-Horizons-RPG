package net.enderwish.Frontier_Subpack;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/**
 * FrontierSubpack
 *
 * Owns the entire pre-gameplay experience for Grey Horizons RPG:
 *   - Custom early boot loading window (ImmediateWindowProvider) — NOT YET
 *     BUILT. Needs the real early-window provider interface verified
 *     against actual NeoForge sources before implementation, same
 *     discipline used for the render mixin work in Atmospheric.
 *   - Custom title screen replacing vanilla's main menu (Start Adventure /
 *     Continue Adventure, Settings, Quit — no Singleplayer/Multiplayer)
 *   - Adventure Mode on/off toggle (disabling reverts fully to vanilla screens)
 *   - Hardware detection + tiering, exposed via a public API for other
 *     subpacks (Combat & Monsters will need this later)
 *
 * Client-only systems (title screen swap etc.) self-register via their own
 * @EventBusSubscriber(value = Dist.CLIENT) classes — same pattern as
 * Atmospheric's ClientParticleFactories/ClientDebugHandler — nothing needs
 * to be called from this constructor for those.
 */
@Mod(FrontierSubpack.MODID)
public class FrontierSubpack {

    public static final String MODID = "gh_frontier";

    public FrontierSubpack(IEventBus modEventBus, ModContainer container) {
        // Systems registered here as we build them.
    }
}
