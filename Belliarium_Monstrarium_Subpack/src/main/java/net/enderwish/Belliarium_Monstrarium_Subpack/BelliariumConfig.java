package net.enderwish.Belliarium_Monstrarium_Subpack;

import net.neoforged.neoforge.common.ModConfigSpec;

public class BelliariumConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue WORLD_TRACKING_ENABLED;
    public static final ModConfigSpec.DoubleValue BOOK_LOSS_CHANCE_START;
    public static final ModConfigSpec.DoubleValue BOOK_LOSS_CHANCE_STEP;
    public static final ModConfigSpec.DoubleValue SANITY_DRAIN_PER_MINUTE;
    public static final ModConfigSpec SPEC;

    static {
        BUILDER.push("sanity");

        SANITY_DRAIN_PER_MINUTE = BUILDER
                .comment("Sanity drained per real-time minute while Hive Mind Influence is active. 1.0 = ~1h40m from full to death.")
                .defineInRange("drainPerMinute", 1.0, 0.0, 100.0);

        BUILDER.pop();

        BUILDER.push("survivors_diary");

        WORLD_TRACKING_ENABLED = BUILDER
                .comment("Whether the Survivor's Diary records world changes so it can rewind the day on death.",
                        "This is the heaviest system in the mod. When false, the diary behaves as a plain notebook.")
                .define("worldTrackingEnabled", true);

        BOOK_LOSS_CHANCE_START = BUILDER
                .comment("Chance (0.0-1.0) the diary disappears on the player's FIRST wake-up.")
                .defineInRange("bookLossChanceStart", 0.0, 0.0, 1.0);

        BOOK_LOSS_CHANCE_STEP = BUILDER
                .comment("How much the book-loss chance increases per wake-up after the first.")
                .defineInRange("bookLossChanceStep", 0.05, 0.0, 1.0);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    /** True when the diary should actively track the world (vs. behaving as a plain notebook). */
    public static boolean trackingEnabled() {
        return WORLD_TRACKING_ENABLED.get();
    }

    /** The chance the diary disappears after a wake-up that brings its wake-up count to {@code wakeUpsAfter}. */
    public static double bookLossChance(int wakeUpsAfter) {
        double chance = BOOK_LOSS_CHANCE_START.get() + BOOK_LOSS_CHANCE_STEP.get() * (wakeUpsAfter - 1);
        return Math.min(1.0, Math.max(0.0, chance));
    }
}
