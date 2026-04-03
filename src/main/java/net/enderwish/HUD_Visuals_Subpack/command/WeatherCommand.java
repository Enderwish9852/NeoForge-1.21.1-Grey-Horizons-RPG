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

/**
 * Command to manually trigger GH Weather effects.
 * Usage: /weather <type> [duration] [intensity]
 */
public class WeatherCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("weather")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("type", StringArgumentType.word())
                        // Suggests weather IDs from our custom registry
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                WeatherRegistry.getAll().stream().map(WeatherType::getIdString),
                                builder
                        ))
                        .executes(context -> setWeather(context.getSource(),
                                StringArgumentType.getString(context, "type"), 6000, 0.5f))

                        .then(Commands.argument("duration", IntegerArgumentType.integer(0))
                                .executes(context -> setWeather(context.getSource(),
                                        StringArgumentType.getString(context, "type"),
                                        IntegerArgumentType.getInteger(context, "duration"), 0.5f))

                                .then(Commands.argument("intensity", FloatArgumentType.floatArg(0.0f, 1.0f))
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
        WeatherType type = WeatherRegistry.getById(typeId);

        // Validation: If it's not clear and we can't find it, it's invalid
        if (type == WeatherRegistry.CLEAR && !typeId.equalsIgnoreCase("clear")) {
            source.sendFailure(Component.literal("§cUnknown GH Weather type: " + typeId));
            return 0;
        }

        // IMPORTANT: We now pass the WeatherType object to match the updated WeatherManager
        WeatherManager.getInstance().setWeather(level, type, intensity);

        // Feedback for the admin
        String name = type.id().getPath().toUpperCase();
        source.sendSuccess(() -> Component.literal("§6[GH]§f Set weather to §b" + name +
                " §ffor §e" + (duration / 20) + "s §fat §a" + (int)(intensity * 100) + "%"), true);

        return 1;
    }
}