package net.enderwish.Frontier_Subpack;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

/**
 * FrontierSubpack
 *
 * Owns the entire pre-gameplay experience for Grey Horizons RPG.
 * Early boot loading window intentionally NOT implemented — see chat
 * discussion (ImmediateWindowProvider requires either reflection-based
 * hacks into non-public NeoForge internals, or a full from-scratch OpenGL
 * pipeline with zero Minecraft API access; both real proven implementations
 * warn against exactly this). Vanilla's boot screen stays as a brief flash;
 * our custom experience starts at the title screen.
 */
@Mod(FrontierSubpack.MODID)
public class FrontierSubpack {

    public static final String MODID = "gh_frontier";

    public FrontierSubpack(IEventBus modEventBus, ModContainer container) {
        // NOTE: this registerConfig call follows the standard NeoForge
        // pattern, but I haven't seen it verified working anywhere in your
        // codebase yet — Atmospheric's own existing Config.java doesn't
        // appear to actually be registered in AtmosphericOverhaulSubpack's
        // constructor either. Flag this line if it errors.
        container.registerConfig(ModConfig.Type.CLIENT, FrontierConfig.SPEC);
    }
}
