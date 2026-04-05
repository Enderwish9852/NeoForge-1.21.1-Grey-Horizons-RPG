package net.enderwish.HUD_Visuals_Subpack.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.enderwish.HUD_Visuals_Subpack.api.ClimateData;
import net.enderwish.HUD_Visuals_Subpack.core.ModAttachments;
import net.enderwish.HUD_Visuals_Subpack.core.Season;
import net.enderwish.HUD_Visuals_Subpack.network.ClimateSyncPacket;
import net.enderwish.HUD_Visuals_Subpack.network.ModMessages;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

public class SeasonCommand {

    // Constant for the day limit
    private static final int MAX_DAYS = 20;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> seasonCommand = Commands.literal("season")
                .requires(source -> source.hasPermission(2));

        // --- Subcommand: /season set <season_name> ---
        LiteralArgumentBuilder<CommandSourceStack> setNode = Commands.literal("set");

        for (Season season : Season.values()) {
            setNode.then(Commands.literal(season.name().toLowerCase())
                    .executes(context -> {
                        ServerLevel level = context.getSource().getLevel();

                        // 1. Fetch current attachment data
                        ClimateData oldData = level.getData(ModAttachments.CLIMATE);

                        // 2. Create new record with the updated season
                        ClimateData newData = new ClimateData(
                                season,
                                oldData.day(),
                                oldData.weather(),
                                oldData.tempOffset(),
                                oldData.intensity()
                        );

                        // 3. Save to Level and Sync to Clients
                        level.setData(ModAttachments.CLIMATE, newData);
                        ModMessages.sendToAllPlayers(new ClimateSyncPacket(newData));

                        context.getSource().sendSuccess(() ->
                                Component.literal("§6[GH]§r Season set to: §b" + season.name()), true);
                        return 1;
                    })
            );
        }

        // --- Subcommand: /season setday <number> ---
        seasonCommand.then(Commands.literal("setday")
                .then(Commands.argument("day", IntegerArgumentType.integer(1, MAX_DAYS))
                        .executes(context -> {
                            int day = IntegerArgumentType.getInteger(context, "day");
                            ServerLevel level = context.getSource().getLevel();

                            // 1. Fetch current attachment data
                            ClimateData oldData = level.getData(ModAttachments.CLIMATE);

                            // 2. Create new record with the updated day
                            ClimateData newData = new ClimateData(
                                    oldData.season(),
                                    day,
                                    oldData.weather(),
                                    oldData.tempOffset(),
                                    oldData.intensity()
                            );

                            // 3. Save to Level and Sync to Clients
                            level.setData(ModAttachments.CLIMATE, newData);
                            ModMessages.sendToAllPlayers(new ClimateSyncPacket(newData));

                            context.getSource().sendSuccess(() ->
                                    Component.literal("§6[GH]§r Season day set to: §e" + day), true);
                            return 1;
                        })
                )
        );

        seasonCommand.then(setNode);
        dispatcher.register(seasonCommand);
    }
}