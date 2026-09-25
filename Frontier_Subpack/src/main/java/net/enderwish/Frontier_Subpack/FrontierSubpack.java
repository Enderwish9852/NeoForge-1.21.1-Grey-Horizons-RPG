package net.enderwish.Frontier_Subpack;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

/**
 * FrontierSubpack
 *
 * UPDATED this round: registers FrontierConfig's new COMMON_SPEC
 * alongside the existing CLIENT SPEC -- see FrontierConfig's own doc
 * comment for why EXOTIC_START moved there.
 *
 * Early boot loading window intentionally NOT implemented -- see chat
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
        container.registerConfig(ModConfig.Type.CLIENT, FrontierConfig.SPEC);
        container.registerConfig(ModConfig.Type.COMMON, FrontierConfig.COMMON_SPEC);
    }
}
