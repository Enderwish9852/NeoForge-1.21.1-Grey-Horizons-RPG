package net.enderwish.Belliarium_Monstrarium_Subpack.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.LearnedSkillsCapability;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.ModAttachments;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

/**
 * SkillCommand
 *
 * /skill list | /skill <name> keybind | /skill remove | /skill remove <name>
 *
 * NOT admin-gated like SeasonCommand/WeatherCommand -- those are debug
 * tools, this is a player managing their own skills.
 */
public class SkillCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("skill")
                .then(Commands.literal("list").executes(SkillCommand::listSkills))
                .then(Commands.literal("remove")
                        .executes(SkillCommand::removeAll)
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(SkillCommand::removeOne)))
                .then(Commands.argument("name", StringArgumentType.word())
                        .then(Commands.literal("keybind").executes(SkillCommand::showKeybind)))
        );
    }

    private static int listSkills(CommandContext<CommandSourceStack> ctx)  throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        LearnedSkillsCapability skills = player.getData(ModAttachments.LEARNED_SKILLS);

        if (skills.getSkillKeyBinds().isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.literal("§6[Skills]§r You haven't learned any skills yet."), false);
            return 1;
        }

        StringBuilder sb = new StringBuilder("§6[Skills]§r ");
        for (Map.Entry<String, Integer> entry : skills.getSkillKeyBinds().entrySet()) {
            sb.append(entry.getKey()).append(", ");
        }
        sb.setLength(sb.length() - 2);
        ctx.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }

    private static int showKeybind(CommandContext<CommandSourceStack> ctx)  throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(ctx, "name");
        Integer keyCode = player.getData(ModAttachments.LEARNED_SKILLS).getKeyFor(name);

        if (keyCode == null) {
            ctx.getSource().sendFailure(Component.literal("§cYou haven't learned '" + name + "'."));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.literal("§6[Skills]§r '" + name + "' is bound to key code " + keyCode), false);
        return 1;
    }

    private static int removeAll(CommandContext<CommandSourceStack> ctx)  throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        player.getData(ModAttachments.LEARNED_SKILLS).removeAll();
        ctx.getSource().sendSuccess(() -> Component.literal("§6[Skills]§r All learned skills removed."), false);
        return 1;
    }

    private static int removeOne(CommandContext<CommandSourceStack> ctx)  throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(ctx, "name");

        if (player.getData(ModAttachments.LEARNED_SKILLS).removeOne(name)) {
            ctx.getSource().sendSuccess(() -> Component.literal("§6[Skills]§r Removed '" + name + "'."), false);
            return 1;
        }
        ctx.getSource().sendFailure(Component.literal("§cYou haven't learned '" + name + "'."));
        return 0;
    }
}
