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
            setNode.then(Commands.literal(season.getSerializedName())
                    .executes(context -> {
                        // Make sure SeasonManager has a static setSeason(ServerLevel, Season) method
                        SeasonManager.setSeason(context.getSource().getLevel(), season);
                        context.getSource().sendSuccess(() ->
                                Component.literal("Season set to: " + season.getSerializedName()), true);
                        return 1;
                    })
            );
        }

        // Subcommand: /season setday <number>
        LiteralArgumentBuilder<CommandSourceStack> setDayNode = Commands.literal("setday")
                .then(Commands.argument("day", IntegerArgumentType.integer(1, 30))
                        .executes(context -> {
                            int day = IntegerArgumentType.getInteger(context, "day");
                            // Make sure SeasonManager has a static setDay(ServerLevel, int) method
                            SeasonManager.setDay(context.getSource().getLevel(), day);
                            context.getSource().sendSuccess(() ->
                                    Component.literal("Season day set to: " + day), true);
                            return 1;
                        })
                );

        // Attach subcommands to the main builder
        seasonCommand.then(setNode);
        seasonCommand.then(setDayNode);

        // Register everything
        dispatcher.register(seasonCommand);
    }
}