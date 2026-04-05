package net.enderwish.HUD_Visuals_Subpack.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.enderwish.HUD_Visuals_Subpack.api.WeatherRegistry;
import net.enderwish.HUD_Visuals_Subpack.api.WeatherType;
import net.enderwish.HUD_Visuals_Subpack.core.weather.WeatherManager;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import java.util.stream.Collectors;

/**
 * Command to manually trigger GH Weather effects.
 * Renamed to 'ghweather' to avoid collision with vanilla '/weather'.
 */
public class WeatherCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Changed "weather" to "ghweather" to prevent the "trailing data" error
        dispatcher.register(Commands.literal("ghweather")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("type", StringArgumentType.word())
                        // Suggests IDs from our registry (e.g., "blizzard", "heatwave")
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                WeatherRegistry.getAll().stream()
                                        .map(w -> w.id().getPath())
                                        .collect(Collectors.toList()),
                                builder
                        ))
                        .executes(context -> setWeather(context.getSource(),
                                StringArgumentType.getString(context, "type"), 6000, 1.0f))

                        .then(Commands.argument("intensity", FloatArgumentType.floatArg(0.0f, 1.0f))
                                .executes(context -> setWeather(context.getSource(),
                                        StringArgumentType.getString(context, "type"),
                                        6000,
                                        FloatArgumentType.getFloat(context, "intensity")))

                                .then(Commands.argument("duration", IntegerArgumentType.integer(0))
                                        .executes(context -> setWeather(context.getSource(),
                                                StringArgumentType.getString(context, "type"),
                                                IntegerArgumentType.getInteger(context, "duration"),
                                                FloatArgumentType.getFloat(context, "intensity")))
                                )
                        )
                )
        );
    }

    private static int setWeather(CommandSourceStack source, String typeId, int duration, float intensity) {
        ServerLevel level = source.getLevel();

        // Use our registry to find the weather
        WeatherType type = WeatherRegistry.getById(typeId);

        // Validation
        if (type == WeatherRegistry.CLEAR && !typeId.equalsIgnoreCase("clear")) {
            source.sendFailure(Component.literal("§c[GH] Unknown weather type: " + typeId));
            return 0;
        }

        // Force the WeatherManager to update the world and sync to clients
        WeatherManager.getInstance().setWeather(level, type, intensity);

        // Format feedback message
        String name = type.id().getPath().toUpperCase().replace("_", " ");
        source.sendSuccess(() -> Component.literal("§6[GH]§f Atmospheric shift: §b" + name +
                " §f(§a" + (int)(intensity * 100) + "% §fintensity)"), true);

        return 1;
    }
}