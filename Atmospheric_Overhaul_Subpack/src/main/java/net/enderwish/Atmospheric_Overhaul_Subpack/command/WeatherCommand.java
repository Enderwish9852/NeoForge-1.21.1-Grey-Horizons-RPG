package net.enderwish.Atmospheric_Overhaul_Subpack.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.enderwish.Atmospheric_Overhaul_Subpack.core.season.SeasonCalendar;
import net.enderwish.Atmospheric_Overhaul_Subpack.core.season.SeasonData;
import net.enderwish.Atmospheric_Overhaul_Subpack.core.weather.WeatherDefinition;
import net.enderwish.Atmospheric_Overhaul_Subpack.core.weather.WeatherRegistry;
import net.enderwish.Atmospheric_Overhaul_Subpack.core.weather.WindDirection;
import net.enderwish.Atmospheric_Overhaul_Subpack.core.weather.WindManager;
import net.enderwish.Atmospheric_Overhaul_Subpack.network.ModMessages;
import net.enderwish.Atmospheric_Overhaul_Subpack.network.SeasonSyncPacket;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

import java.util.Arrays;
import java.util.List;

/**
 * WeatherCommand
 *
 * /ghweather <type>                                                        — fully rolled
 * /ghweather <type> <intensity>                                            — custom intensity, rest rolled
 * /ghweather <type> <intensity> <duration>                                 — + custom duration
 * /ghweather <type> <intensity> <duration> <turbulence>                    — + custom turbulence
 * /ghweather <type> <intensity> <duration> <turbulence> <wind_speed>       — + custom wind speed (0-1)
 * /ghweather <type> <intensity> <duration> <turbulence> <wind_speed> <direction> — + custom direction
 * /ghweather list                                                          — lists all loaded weather types
 *
 * Validates the requested weather is actually in the current season/phase's
 * pool (nonzero weight) — forcing e.g. a spring blizzard is rejected with
 * an "Out season weather" error rather than silently applying it.
 *
 * All wind values (speed/turbulence/direction) apply INSTANTLY via
 * WindManager.applyManual — no lerp delay — since this command is a
 * testing/debug tool and waiting ~10s for natural interpolation isn't useful
 * when actively iterating.
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

                // /ghweather <type> [intensity] [duration] [turbulence] [wind_speed] [direction]
                .then(Commands.argument("type", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                WeatherRegistry.INSTANCE.getAllNames(), builder
                        ))

                        // /ghweather <type> — everything rolled
                        .executes(context -> setWeather(
                                context.getSource(),
                                StringArgumentType.getString(context, "type"),
                                null, null, null, null, null
                        ))

                        // /ghweather <type> <intensity>
                        .then(Commands.argument("intensity", FloatArgumentType.floatArg(0.0f, 1.0f))
                                .executes(context -> setWeather(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "type"),
                                        FloatArgumentType.getFloat(context, "intensity"),
                                        null, null, null, null
                                ))

                                // /ghweather <type> <intensity> <duration>
                                .then(Commands.argument("duration", IntegerArgumentType.integer(1))
                                        .executes(context -> setWeather(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "type"),
                                                FloatArgumentType.getFloat(context, "intensity"),
                                                IntegerArgumentType.getInteger(context, "duration"),
                                                null, null, null
                                        ))

                                        // /ghweather <type> <intensity> <duration> <turbulence>
                                        .then(Commands.argument("turbulence", FloatArgumentType.floatArg(0.0f, 1.0f))
                                                .executes(context -> setWeather(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "type"),
                                                        FloatArgumentType.getFloat(context, "intensity"),
                                                        IntegerArgumentType.getInteger(context, "duration"),
                                                        FloatArgumentType.getFloat(context, "turbulence"),
                                                        null, null
                                                ))

                                                // /ghweather <type> <intensity> <duration> <turbulence> <wind_speed>
                                                .then(Commands.argument("wind_speed", FloatArgumentType.floatArg(0.0f, 1.0f))
                                                        .executes(context -> setWeather(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "type"),
                                                                FloatArgumentType.getFloat(context, "intensity"),
                                                                IntegerArgumentType.getInteger(context, "duration"),
                                                                FloatArgumentType.getFloat(context, "turbulence"),
                                                                FloatArgumentType.getFloat(context, "wind_speed"),
                                                                null
                                                        ))

                                                        // /ghweather <type> <intensity> <duration> <turbulence> <wind_speed> <direction>
                                                        .then(Commands.argument("direction", StringArgumentType.word())
                                                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                                        Arrays.stream(WindDirection.values())
                                                                                .map(Enum::name), builder
                                                                ))
                                                                .executes(context -> setWeather(
                                                                        context.getSource(),
                                                                        StringArgumentType.getString(context, "type"),
                                                                        FloatArgumentType.getFloat(context, "intensity"),
                                                                        IntegerArgumentType.getInteger(context, "duration"),
                                                                        FloatArgumentType.getFloat(context, "turbulence"),
                                                                        FloatArgumentType.getFloat(context, "wind_speed"),
                                                                        StringArgumentType.getString(context, "direction")
                                                                ))
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );
    }

    // ── Handler ───────────────────────────────────────────────────────────────

    /**
     * Any customX param is null when the player didn't supply that argument
     * — in that case we roll/derive a value instead of using a hardcoded
     * default. directionName is a raw string (validated against
     * WindDirection.valueOf) since Brigadier has no enum argument type here.
     */
    private static int setWeather(CommandSourceStack source,
                                  String typeId,
                                  Float customIntensity,
                                  Integer customDuration,
                                  Float customTurbulence,
                                  Float customWindSpeed,
                                  String directionName) {
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

        // Validate direction, if supplied
        WindDirection direction = null;
        if (directionName != null) {
            try {
                direction = WindDirection.valueOf(directionName.toUpperCase());
            } catch (IllegalArgumentException e) {
                source.sendFailure(Component.literal(
                        "§c[GH] Unknown direction: §f" + directionName
                                + "\n§c Available: §f" + Arrays.toString(WindDirection.values())
                ));
                return 0;
            }
        }

        WeatherDefinition def = WeatherRegistry.INSTANCE.getByName(typeId);

        // ── Season/phase validation ─────────────────────────────────────────
        SeasonData data = SeasonData.get(level);
        SeasonCalendar.Season season = data.getSeason();
        SeasonCalendar.Phase phase = data.getPhase();

        if (!def.isInPool(season, phase)) {
            source.sendFailure(Component.literal(
                    "§c[GH] " + typeId + " has zero weight in "
                            + season.displayName() + " " + phase.displayName()
                            + ".\n§cOut season weather"
            ));
            return 0;
        }

        RandomSource rand = level.getRandom();

        // ── Resolve final values — roll from JSON range if not supplied ─────
        int finalDuration = def.isSpecial()
                ? def.duration().min()
                : (customDuration != null ? customDuration : def.rollDuration(rand));

        float finalIntensity = def.isSpecial()
                ? def.intensity().min()
                : (customIntensity != null ? Math.min(customIntensity, 0.99f) : def.rollIntensity(rand));

        float finalTurbulence = customTurbulence != null
                ? customTurbulence
                : def.windTurbulence();

        float finalWindSpeed = customWindSpeed != null
                ? customWindSpeed
                : def.rollWindSpeed(rand);

        // Apply to Minecraft level
        if (def.hasRain()) {
            level.setWeatherParameters(0, finalDuration, true, def.hasThunder());
        } else {
            level.setWeatherParameters(finalDuration, 0, false, false);
        }

        // Store in SeasonData so it persists + stays in sync
        data.setActiveWeather(typeId, finalDuration, finalIntensity);

        // ── Wind — command bypasses the natural onWeatherChanged pipeline,
        //     so drive WindManager directly. Applies instantly (no lerp).
        WindManager.INSTANCE.applyManual(finalWindSpeed, finalTurbulence, direction, level);

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
        WindDirection finalDir = direction; // for lambda capture
        source.sendSuccess(() -> Component.literal(
                "§6[GH]§r Weather set to §b" + displayName
                        + " §7(intensity: " + String.format("%.0f%%", finalIntensity * 100)
                        + ", duration: " + finalDuration + " ticks"
                        + ", turbulence: " + String.format("%.2f", finalTurbulence)
                        + ", wind speed: " + String.format("%.2f", finalWindSpeed)
                        + (finalDir != null ? ", direction: " + finalDir : "") + ")"
        ), true);

        return 1;
    }
}