package net.enderwish.Belliarium_Monstrarium_Subpack;

import net.neoforged.neoforge.common.ModConfigSpec;

public class BelliariumConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue WORLD_TRACKING_ENABLED;
    public static final ModConfigSpec SPEC;

    static {
        BUILDER.push("survivors_diary");

        WORLD_TRACKING_ENABLED = BUILDER
                .comment("Whether the Survivor's Diary records world changes so it can rewind the day on death.",
                        "This is the heaviest system in the mod. When false, the diary behaves as a plain notebook.")
                .define("worldTrackingEnabled", true);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
