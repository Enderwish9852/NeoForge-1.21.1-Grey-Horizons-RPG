package net.enderwish.Atmospheric_Overhaul_Subpack.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.enderwish.Atmospheric_Overhaul_Subpack.core.season.SeasonData;
import net.enderwish.Atmospheric_Overhaul_Subpack.core.weather.WeatherDefinition;
import net.enderwish.Atmospheric_Overhaul_Subpack.core.weather.WeatherRegistry;
import net.enderwish.Atmospheric_Overhaul_Subpack.network.ModMessages;
import net.enderwish.Atmospheric_Overhaul_Subpack.network.SeasonSyncPacket;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

/**
 * WeatherCommand
 *
 * /ghweather <type>                        — set weather with default duration + intensity
 * /ghweather <type> <intensity>            — set weather with custom intensity
 * /ghweather <type> <intensity> <duration> — set weather with custom intensity + duration
 * /ghweather list                          — lists all loaded weather types
 *
 * Uses 'ghweather' to avoid collision with vanilla '/weather'.
 */
public class WeatherCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ghweather")
                .requires(source -> source.hasPermission(2))

                // /ghweather list
                .then(Commands.literal("list")
                        .executes(context -> {
                            if (!WeatherRegistry.INSTANCE.isLoaded()) {
                                context.getSource().sendFailure(Component.literal(
                                        "§c[GH] Weather registry not loaded yet."
                                ));
                                return 0;
                            }
                            String list = String.join(", ", WeatherRegistry.INSTANCE.getAllNames());
                            context.getSource().sendSuccess(() -> Component.literal(
                                    "§6[GH Weather Types]§r " + list
                            ), false);
                            return 1;
                        })
                )

                // /ghweather <type> [intensity] [duration]
                .then(Commands.argument("type", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                WeatherRegistry.INSTANCE.getAllNames(), builder
                        ))

                        // /ghweather <type>
                        .executes(context -> setWeather(
                                context.getSource(),
                                StringArgumentType.getString(context, "type"),
                                6000,
                                0.5f
                        ))

                        // /ghweather <type> <intensity>
                        .then(Commands.argument("intensity", FloatArgumentType.floatArg(0.0f, 1.0f))
                                .executes(context -> setWeather(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "type"),
                                        6000,
                                        FloatArgumentType.getFloat(context, "intensity")
                                ))

                                // /ghweather <type> <intensity> <duration>
                                .then(Commands.argument("duration", IntegerArgumentType.integer(1))
                                        .executes(context -> setWeather(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "type"),
                                                IntegerArgumentType.getInteger(context, "duration"),
                                                FloatArgumentType.getFloat(context, "intensity")
                                        ))
                                )
                        )
                )
        );
    }

    // ── Handler ───────────────────────────────────────────────────────────────

    private static int setWeather(CommandSourceStack source,
                                  String typeId,
                                  int duration,
                                  float intensity) {
        ServerLevel level = source.getLevel();

        // Validate registry loaded
        if (!WeatherRegistry.INSTANCE.isLoaded()) {
            source.sendFailure(Component.literal("§c[GH] Weather registry not loaded yet."));
            return 0;
        }

        // Validate weather type exists
        if (!WeatherRegistry.INSTANCE.getAllNames().contains(typeId)) {
            source.sendFailure(Component.literal(
                    "§c[GH] Unknown weather type: §f" + typeId
                            + "\n§c Available: §f" + String.join(", ", WeatherRegistry.INSTANCE.getAllNames())
            ));
            return 0;
        }

        WeatherDefinition def = WeatherRegistry.INSTANCE.getByName(typeId);

        // Special weathers always use fixed duration + intensity
        int finalDuration  = def.isSpecial() ? def.duration().min() : duration;
        float finalIntensity = def.isSpecial() ? def.intensity().min() : Math.min(intensity, 0.99f);

        // Apply to Minecraft level
        if (def.hasRain()) {
            level.setWeatherParameters(0, finalDuration, true, def.hasThunder());
        } else {
            level.setWeatherParameters(finalDuration, 0, false, false);
        }

        // Store in SeasonData so it persists + stays in sync
        SeasonData data = SeasonData.get(level);
        data.setActiveWeather(typeId, finalDuration, finalIntensity);

        // Sync to all clients
        ModMessages.sendToAllPlayers(new SeasonSyncPacket(
                data.getTotalDays(),
                data.getYearDay(),
                data.getSeason(),
                data.getPhase(),
                data.getActiveWeatherId(),
                data.getActiveIntensity(),
                data.getYear()
        ));

        String displayName = typeId.toUpperCase().replace("_", " ");
        source.sendSuccess(() -> Component.literal(
                "§6[GH]§r Weather set to §b" + displayName
                        + " §7(intensity: " + String.format("%.0f%%", finalIntensity * 100)
                        + ", duration: " + finalDuration + " ticks)"
        ), true);

        return 1;
    }
}