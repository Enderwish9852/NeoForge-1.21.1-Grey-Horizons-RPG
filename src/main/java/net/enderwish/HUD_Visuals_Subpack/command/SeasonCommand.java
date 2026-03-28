package net.enderwish.HUD_Visuals_Subpack.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.enderwish.HUD_Visuals_Subpack.core.Season;
import net.enderwish.HUD_Visuals_Subpack.event.SeasonManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class SeasonCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Create the base command: /season
        LiteralArgumentBuilder<CommandSourceStack> seasonCommand = Commands.literal("season")
                .requires(source -> source.hasPermission(2));

        // Subcommand: /season set <season_name>
        LiteralArgumentBuilder<CommandSourceStack> setNode = Commands.literal("set");

        for (Season season : Season.values()) {
            // Using toLowerCase() to ensure the command is easy to type
            setNode.then(Commands.literal(season.name().toLowerCase())
                    .executes(context -> {
                        SeasonManager.setSeason(context.getSource().getLevel(), season);
                        context.getSource().sendSuccess(() ->
                                Component.literal("Season set to: " + season.name()), true);
                        return 1;
                    })
            );
        }

        // Subcommand: /season setday <number>
        // Note: I consolidated this to /season setday to match your logic
        seasonCommand.then(Commands.literal("setday")
                .then(Commands.argument("day", IntegerArgumentType.integer(1, 30))
                        .executes(context -> {
                            int day = IntegerArgumentType.getInteger(context, "day");
                            SeasonManager.setDay(context.getSource().getLevel(), day);
                            context.getSource().sendSuccess(() ->
                                    Component.literal("Season day set to: " + day), true);
                            return 1;
                        })
                )
        );

        // Attach the 'set' node (/season set ...)
        seasonCommand.then(setNode);

        // Register everything
        dispatcher.register(seasonCommand);
    }
}