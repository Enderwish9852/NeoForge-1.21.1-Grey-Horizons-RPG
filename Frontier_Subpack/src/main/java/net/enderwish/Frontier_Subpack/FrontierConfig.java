package net.enderwish.Frontier_Subpack;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * FrontierConfig
 *
 * SPLIT this round: EXOTIC_START moved out of the CLIENT spec into a
 * new COMMON spec. Reasoning: it's the one setting here read from
 * genuinely server-side/game-logic code (ExoticSpawnHandler, reacting
 * to LevelEvent.CreateSpawnPosition) rather than client Screen code --
 * a CLIENT-registered config is the wrong type for that, which is the
 * most likely explanation for "Exotic Start does nothing" even after
 * confirming it was toggled on before a genuinely fresh world. This is
 * correct either way, so it's not a shot in the dark -- and
 * ExoticSpawnHandler now logs enough to confirm or rule this out for
 * certain if it's still wrong.
 *
 * World Creation category: HARDCORE_MODE and DEV_MODE remain mutually
 * exclusive in practice -- enforced in FrontierSettingsScreen's click
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

    // ── Common config -- settings read from server-side/game-logic code ────────

    private static final ModConfigSpec.Builder COMMON_BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue EXOTIC_START;
    public static final ModConfigSpec COMMON_SPEC;

    static {
        COMMON_BUILDER.push("world_creation");

        EXOTIC_START = COMMON_BUILDER
                .comment("Whether a fresh Adventure world forces the player's spawn point into a Volcanic Lowlands or Glacial Peaks biome.",
                        "Independent of Hardcore/Dev Mode -- can be combined with either.",
                        "Players can otherwise NEVER spawn in Volcanic Lowlands; this is the one exception.")
                .define("exoticStart", false);

        COMMON_BUILDER.pop();
        COMMON_SPEC = COMMON_BUILDER.build();
    }
}