package net.enderwish.Belliarium_Monstrarium_Subpack.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.status.StatusManager;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.status.StatusType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * StatusCommand
 *
 * /status             -- lists this player's current statuses (temporary text list; icons come later).
 * /status clear        -- removes every CONDITIONAL status (e.g. Deja Vu). Permanent body statuses are untouched.
 * /status remove <name> -- removes one conditional status by name; fails on a permanent one or a miss.
 */
public class StatusCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("status")
                .executes(StatusCommand::list)
                .then(Commands.literal("clear").executes(StatusCommand::clear))
                .then(Commands.literal("remove")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(StatusCommand::remove))));
    }

    private static int list(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Set<StatusType> active = EnumSet.noneOf(StatusType.class);
        active.addAll(StatusManager.INSTANCE.activeBodyStatuses(player));
        active.addAll(StatusManager.INSTANCE.activeConditional(player.getUUID()));

        if (active.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.literal("§6[Status]§r No active statuses."), false);
            return 1;
        }
        StringBuilder sb = new StringBuilder("§6[Status]§r ");
        for (StatusType type : active) sb.append(type.displayName()).append(", ");
        sb.setLength(sb.length() - 2);
        ctx.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }

    private static int clear(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        StatusManager.INSTANCE.clearConditional(player.getUUID());
        ctx.getSource().sendSuccess(() -> Component.literal("§6[Status]§r Cleared all conditional statuses."), false);
        return 1;
    }

    private static int remove(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(ctx, "name").toUpperCase(Locale.ROOT);
        StatusType type;
        try {
            type = StatusType.valueOf(name);
        } catch (IllegalArgumentException e) {
            ctx.getSource().sendFailure(Component.literal("§cUnknown status '" + name + "'."));
            return 0;
        }
        if (StatusManager.INSTANCE.remove(player.getUUID(), type)) {
            ctx.getSource().sendSuccess(() -> Component.literal("§6[Status]§r Removed " + type.displayName() + "."), false);
            return 1;
        }
        ctx.getSource().sendFailure(Component.literal("§cThat status isn't active or can't be removed."));
        return 0;
    }
}
