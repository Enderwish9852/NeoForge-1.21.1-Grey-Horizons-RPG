package net.enderwish.Frontier_Subpack;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * FrontierConfig
 *
 * World Creation category: HARDCORE_MODE and DEV_MODE are mutually
 * exclusive in practice — enforced in FrontierSettingsScreen's click
 * handlers, since ModConfigSpec has no native concept of linked booleans.
 * Default: Hardcore ON, Dev Mode OFF, per the project's hardcore-realism
 * design philosophy.
 */
public class FrontierConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ADVENTURE_MODE_ENABLED = BUILDER
            .comment("Whether the custom Frontier title screen and Adventure flow replace vanilla's menu.")
            .define("adventureModeEnabled", true);

    public static final ModConfigSpec.BooleanValue HARDCORE_MODE;
    public static final ModConfigSpec.BooleanValue DEV_MODE;

    public static final ModConfigSpec SPEC;

    static {
        BUILDER.push("world_creation");

        HARDCORE_MODE = BUILDER
                .comment("Whether a fresh Adventure world is created in Hardcore (permadeath) mode.",
                        "Mutually exclusive with Dev Mode.")
                .define("hardcoreMode", true);

        DEV_MODE = BUILDER
                .comment("Whether a fresh Adventure world is created in Dev Mode (Creative, cheats allowed, non-hardcore).",
                        "Mutually exclusive with Hardcore Mode.")
                .define("devMode", false);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}