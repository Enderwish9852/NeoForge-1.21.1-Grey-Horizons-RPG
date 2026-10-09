package net.enderwish.Belliarium_Monstrarium_Subpack.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.diary.DayTracker;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.diary.DayTrackerData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

/**
 * DiaryDebugCommand -- TEMPORARY test harness, removed once the real diary item exists.
 *   /diarydebug start | status | rollback | stop
 */
public class DiaryDebugCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("diarydebug")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("start").executes(DiaryDebugCommand::start))
                .then(Commands.literal("status").executes(DiaryDebugCommand::status))
                .then(Commands.literal("rollback").executes(DiaryDebugCommand::rollback))
                .then(Commands.literal("stop").executes(DiaryDebugCommand::stop)));
    }

    private static ServerLevel overworld(CommandContext<CommandSourceStack> ctx) {
        return ctx.getSource().getServer().overworld();
    }

    private static int start(CommandContext<CommandSourceStack> ctx) {
        DayTracker.ActivationResult result = DayTracker.activate(overworld(ctx));
        String message = switch (result) {
            case STARTED -> "§6[Diary]§r Tracking started. Window: 24000 ticks.";
            case ALREADY_ACTIVE -> "§6[Diary]§r Already tracking.";
            case DISABLED_BY_CONFIG -> "§c[Diary] worldTrackingEnabled is false in the config.";
        };
        ctx.getSource().sendSuccess(() -> Component.literal(message), false);
        return 1;
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = overworld(ctx);
        DayTrackerData data = DayTracker.getActiveData();
        String message;
        if (data == null) {
            message = "§6[Diary]§r Not tracking.";
        } else {
            long elapsed = level.getGameTime() - data.getStartGameTime();
            message = "§6[Diary]§r Tracking. Elapsed: " + elapsed + "/" + DayTracker.TRACKING_WINDOW_TICKS
                    + " ticks | Blocks logged: " + data.getBlockLog().size();
        }
        ctx.getSource().sendSuccess(() -> Component.literal(message), false);
        return 1;
    }

    private static int rollback(CommandContext<CommandSourceStack> ctx) {
        int restored = DayTracker.rollbackBlocks(overworld(ctx));
        String message = restored < 0
                ? "§c[Diary] Not tracking, nothing to roll back."
                : "§6[Diary]§r Rolled back. Blocks restored: " + restored;
        ctx.getSource().sendSuccess(() -> Component.literal(message), false);
        return 1;
    }

    private static int stop(CommandContext<CommandSourceStack> ctx) {
        DayTracker.deactivate(overworld(ctx));
        ctx.getSource().sendSuccess(() -> Component.literal("§6[Diary]§r Tracking stopped, log discarded."), false);
        return 1;
    }
}
