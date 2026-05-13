package net.enderwish.Atmospheric_Overhaul_Subpack.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.enderwish.Atmospheric_Overhaul_Subpack.core.season.SeasonCalendar;
import net.enderwish.Atmospheric_Overhaul_Subpack.core.season.SeasonData;
import net.enderwish.Atmospheric_Overhaul_Subpack.network.ModMessages;
import net.enderwish.Atmospheric_Overhaul_Subpack.network.SeasonSyncPacket;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

/**
 * SeasonCommand
 *
 * /season info                    — shows current season, phase, day, year
 * /season set <season>            — jumps to the start of a season
 * /season setday <0-79>           — sets the exact year day
 * /season skipphase               — skips to the next phase
 */
public class SeasonCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("season")
                .requires(source -> source.hasPermission(2));

        // /season info
        root.then(Commands.literal("info")
                .executes(context -> {
                    ServerLevel level = context.getSource().getLevel();
                    SeasonData data = SeasonData.get(level);
                    context.getSource().sendSuccess(() -> Component.literal(
                            "§6[GH Seasons]§r " + data.getDisplayLabel()
                                    + "\n§6Season day:§r " + data.getSeasonDay() + "/19"
                                    + "\n§6Year day:§r " + data.getYearDay() + "/79"
                                    + "\n§6Total days:§r " + data.getTotalDays()
                                    + "\n§6Weather:§r " + data.getActiveWeatherId()
                                    + " (intensity: " + String.format("%.2f", data.getActiveIntensity()) + ")"
                                    + "\n§6Weather ticks left:§r " + data.getWeatherTicksRemaining()
                    ), false);
                    return 1;
                })
        );

        // /season set <season> — jumps to day 0 of that season
        LiteralArgumentBuilder<CommandSourceStack> setNode = Commands.literal("set");
        for (SeasonCalendar.Season season : SeasonCalendar.Season.values()) {
            setNode.then(Commands.literal(season.name().toLowerCase())
                    .executes(context -> {
                        ServerLevel level = context.getSource().getLevel();
                        SeasonData data = SeasonData.get(level);

                        // Calculate totalDays to land on day 0 of the target season
                        int yearOffset = data.getYear() * SeasonCalendar.DAYS_PER_YEAR;
                        int seasonStart = season.ordinal() * SeasonCalendar.DAYS_PER_SEASON;
                        data.setTotalDays(yearOffset + seasonStart);

                        syncToClients(level, data);

                        context.getSource().sendSuccess(() -> Component.literal(
                                "§6[GH]§r Season set to §b" + season.displayName()
                        ), true);
                        return 1;
                    })
            );
        }
        root.then(setNode);

        // /season setday <0-79> — sets exact year day
        root.then(Commands.literal("setday")
                .then(Commands.argument("day", IntegerArgumentType.integer(0, 79))
                        .executes(context -> {
                            int day = IntegerArgumentType.getInteger(context, "day");
                            ServerLevel level = context.getSource().getLevel();
                            SeasonData data = SeasonData.get(level);

                            int yearOffset = data.getYear() * SeasonCalendar.DAYS_PER_YEAR;
                            data.setTotalDays(yearOffset + day);

                            syncToClients(level, data);

                            context.getSource().sendSuccess(() -> Component.literal(
                                    "§6[GH]§r Year day set to §e" + day
                                            + " §7(" + data.getDisplayLabel() + ")"
                            ), true);
                            return 1;
                        })
                )
        );

        // /season skipphase — skips to the start of the next phase
        root.then(Commands.literal("skipphase")
                .executes(context -> {
                    ServerLevel level = context.getSource().getLevel();
                    SeasonData data = SeasonData.get(level);

                    SeasonCalendar.Phase currentPhase = data.getPhase();
                    int currentYearDay = data.getYearDay();
                    int yearOffset = data.getYear() * SeasonCalendar.DAYS_PER_YEAR;

                    // Find start of next phase
                    int nextPhaseDay;
                    int seasonBase = data.getSeason().ordinal() * SeasonCalendar.DAYS_PER_SEASON;
                    nextPhaseDay = switch (currentPhase) {
                        case EARLY -> seasonBase + SeasonCalendar.Phase.MID.startDay;
                        case MID   -> seasonBase + SeasonCalendar.Phase.LATE.startDay;
                        case LATE  -> (data.getSeason().ordinal() + 1) % 4
                                * SeasonCalendar.DAYS_PER_SEASON; // start of next season
                    };

                    data.setTotalDays(yearOffset + nextPhaseDay);
                    syncToClients(level, data);

                    context.getSource().sendSuccess(() -> Component.literal(
                            "§6[GH]§r Skipped to §b" + data.getDisplayLabel()
                    ), true);
                    return 1;
                })
        );

        dispatcher.register(root);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private static void syncToClients(ServerLevel level, SeasonData data) {
        ModMessages.sendToAllPlayers(new SeasonSyncPacket(
                data.getTotalDays(),
                data.getYearDay(),
                data.getSeason(),
                data.getPhase(),
                data.getActiveWeatherId(),
                data.getActiveIntensity(),
                data.getYear()
        ));
    }
}