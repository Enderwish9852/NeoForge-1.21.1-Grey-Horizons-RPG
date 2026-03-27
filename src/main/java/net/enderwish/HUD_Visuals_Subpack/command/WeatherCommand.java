package net.enderwish.HUD_Visuals_Subpack.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerLevel;
import net.enderwish.HUD_Visuals_Subpack.core.WeatherType;
import net.enderwish.HUD_Visuals_Subpack.core.WeatherManager;
import java.util.Arrays;

/**
 * Command class to manually trigger weather effects.
 * Usage: /weather <type> <duration> <intensity>
 */
public class WeatherCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("weather")
                .requires(source -> source.hasPermission(2)) // Level 2 permission required (OP)
                .then(Commands.argument("type", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                Arrays.stream(WeatherType.values()).map(e -> e.name().toLowerCase()),
                                builder
                        ))
                        .then(Commands.argument("duration", IntegerArgumentType.integer(0))
                                .then(Commands.argument("intensity", FloatArgumentType.floatArg(0.0f, 1.0f))
                                        .executes(context -> {
                                            String typeStr = StringArgumentType.getString(context, "type");

                                            // Get the server level from the command source
                                            ServerLevel level = context.getSource().getLevel();

                                            try {
                                                WeatherType type = WeatherType.valueOf(typeStr.toUpperCase());
                                                int duration = IntegerArgumentType.getInteger(context, "duration");
                                                float intensity = FloatArgumentType.getFloat(context, "intensity");

                                                // UPDATED: Passing the level as the first argument to match the 4-arg signature
                                                WeatherManager.getInstance().setWeather(level, type, duration, intensity);

                                                return 1; // Success
                                            } catch (IllegalArgumentException e) {
                                                // Handle cases where the typed string doesn't match an Enum value
                                                return 0; // Failure
                                            }
                                        })
                                )
                        )
                )
        );
    }
}