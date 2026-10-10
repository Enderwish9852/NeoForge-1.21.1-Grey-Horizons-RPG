package net.enderwish.Belliarium_Monstrarium_Subpack.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.diary.DayTracker;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.diary.DayTrackerData;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.diary.PendingFixData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * DiaryDebugCommand -- TEMPORARY test harness, removed once the real diary item exists.
 *   /diarydebug start | status | rollback | stop | pending
 */
public class DiaryDebugCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("diarydebug")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("start").executes(DiaryDebugCommand::start))
                .then(Commands.literal("status").executes(DiaryDebugCommand::status))
                .then(Commands.literal("rollback").executes(DiaryDebugCommand::rollback))
                .then(Commands.literal("stop").executes(DiaryDebugCommand::stop))
                .then(Commands.literal("pending").executes(DiaryDebugCommand::pending)));
    }

    private static ServerLevel overworld(CommandContext<CommandSourceStack> ctx) {
        return ctx.getSource().getServer().overworld();
    }

    private static int start(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        DayTracker.ActivationResult result = DayTracker.activate(overworld(ctx), player);
        String message = switch (result) {
            case STARTED -> "§6[Diary]§r Tracking started. Window: 24000 ticks. Your state is saved as the rewind point.";
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
            message = "§6[Diary]§r Tracking. Elapsed: " + elapsed + "/" + DayTracker.TRACKING_WINDOW_TICKS + " ticks"
                    + "\n§6Player snapshot:§r " + (data.getPlayerSnapshot().isEmpty() ? "none" : "saved")
                    + " | §6Environment snapshot:§r " + (data.getEnvironment().isEmpty() ? "none" : "saved")
                    + "\n§6Blocks logged:§r " + data.getBlockLog().size()
                    + "\n§6Containers snapshotted:§r " + data.getContainers().size()
                    + "\n§6Entities:§r baseline " + data.getEntities().baselineCount()
                    + ", spawned " + data.getEntities().spawnedCount()
                    + ", destroyed " + data.getEntities().destroyedCount();
        }
        ctx.getSource().sendSuccess(() -> Component.literal(message), false);
        return 1;
    }

    private static int rollback(CommandContext<CommandSourceStack> ctx) {
        DayTracker.RollbackReport report = DayTracker.rollback(overworld(ctx));
        if (report == null) {
            ctx.getSource().sendFailure(Component.literal("§c[Diary] Not tracking, nothing to roll back."));
            return 0;
        }
        String message = "§6[Diary]§r Rolled back."
                + "\n§6Blocks restored:§r " + report.blocks()
                + "\n§6Containers restored:§r " + report.containers()
                + " (deferred until chunk loads: " + report.containersDeferred() + ")"
                + "\n§6Entities:§r restored " + report.entitiesRestored()
                + ", recreated " + report.entitiesRecreated()
                + ", removed " + report.entitiesDiscarded()
                + ", deferred until chunk loads " + report.entitiesDeferred()
                + "\n§6Environment restored:§r " + report.environmentRestored()
                + " | §6Player restored:§r " + report.playerRestored();
        ctx.getSource().sendSuccess(() -> Component.literal(message), false);
        return 1;
    }

    private static int stop(CommandContext<CommandSourceStack> ctx) {
        DayTracker.deactivate(overworld(ctx));
        ctx.getSource().sendSuccess(() -> Component.literal("§6[Diary]§r Tracking stopped, logs discarded."), false);
        return 1;
    }

    private static int pending(CommandContext<CommandSourceStack> ctx) {
        PendingFixData pending = PendingFixData.get(overworld(ctx));
        String message = "§6[Diary]§r Pending fixes (apply when their chunk loads):"
                + "\n§6Containers:§r " + pending.containerCount()
                + "\n§6Entities to reset:§r " + pending.entityRestoreCount()
                + " | §6to remove:§r " + pending.entityDiscardCount()
                + " | §6to recreate:§r " + pending.entityRecreateCount();
        ctx.getSource().sendSuccess(() -> Component.literal(message), false);
        return 1;
    }
}
