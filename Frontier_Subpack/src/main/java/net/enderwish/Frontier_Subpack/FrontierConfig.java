package net.enderwish.Frontier_Subpack;

import net.neoforged.neoforge.common.ModConfigSpec;

public class FrontierConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ADVENTURE_MODE_ENABLED = BUILDER
            .comment("Whether the custom Frontier title screen and Adventure flow replace vanilla's menu.")
            .define("adventureModeEnabled", true);

    public static final ModConfigSpec.BooleanValue HARDCORE_MODE;
    public static final ModConfigSpec.BooleanValue DEV_MODE;
    public static final ModConfigSpec.BooleanValue EXOTIC_START;

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

        EXOTIC_START = BUILDER
                .comment("Whether a fresh Adventure world forces the player's spawn point into a Volcanic Lowlands biome.",
                        "Independent of Hardcore/Dev Mode -- can be combined with either.",
                        "Players can otherwise NEVER spawn in Volcanic Lowlands; this is the one exception.")
                .define("exoticStart", false);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}