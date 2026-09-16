package net.enderwish.Frontier_Subpack;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * FrontierConfig
 *
 * Adventure Mode on/off toggle. Disabling reverts fully to vanilla's
 * title screen (Singleplayer/Multiplayer/etc restored).
 */
public class FrontierConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ADVENTURE_MODE_ENABLED = BUILDER
            .comment("Whether the custom Grey Horizons title screen and Adventure Mode flow are active.",
                    "Disabling this reverts to vanilla's normal title screen.")
            .define("adventureModeEnabled", true);

    public static final ModConfigSpec SPEC = BUILDER.build();
}