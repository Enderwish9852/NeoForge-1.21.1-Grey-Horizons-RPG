package net.enderwish.Atmospheric_Overhaul_Subpack.core.weather;

import net.enderwish.Atmospheric_Overhaul_Subpack.core.season.SeasonCalendar;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Random;

/**
 * WeatherRoller
 *
 * Picks the next weather using weighted random selection.
 *
 * How it works:
 *   1. Gets the pool of valid weathers for the current season + phase
 *   2. Filters out weathers that can't occur in the current biome context
 *   3. Sums all weights
 *   4. Picks a random number between 0 and total weight
 *   5. Walks the list until it finds the winner
 *   6. Rolls duration + intensity for the chosen weather
 *   7. Returns a RollResult with everything ClimateEventHandler needs
 */
public class WeatherRoller {
    // Singleton
    public static final WeatherRoller INSTANCE = new WeatherRoller();
    private static final Random RAND = new Random();
    private WeatherRoller() {}
    // Roll
    public RollResult roll(SeasonCalendar.Season season,
                           SeasonCalendar.Phase phase,
                           ServerLevel level) {
        // Guard
        if (!WeatherRegistry.INSTANCE.isLoaded()) {
            return RollResult.fallback(WeatherRegistry.INSTANCE.getFallback());
        }

        List<WeatherDefinition> pool = WeatherRegistry.INSTANCE.getPool(season, phase);

        float avgTemp = getAverageBiomeTemp(level);
        pool = filterByBiomeTemp(pool, avgTemp);

        if (pool.isEmpty()) {
            System.out.println("[GreyHorizons] WeatherRoller: empty pool for "
                    + season + " " + phase + ", defaulting to clear");
            return RollResult.fallback(WeatherRegistry.INSTANCE.getFallback());
        }

        int totalWeight = 0;
        for (WeatherDefinition def : pool) {
            totalWeight += def.getWeight(season, phase);
        }

        int roll = RAND.nextInt(totalWeight);
        int cursor = 0;
        WeatherDefinition chosen = pool.get(pool.size() - 1);

        for (WeatherDefinition def : pool) {
            cursor += def.getWeight(season, phase);
            if (roll < cursor) {
                chosen = def;
                break;
            }
        }

        int   duration  = chosen.rollDuration(RAND);
        float intensity = chosen.rollIntensity(RAND);
        final WeatherDefinition finalChosen = chosen;
        String name = WeatherRegistry.INSTANCE.getAllNames()
                .stream()
                .filter(n -> WeatherRegistry.INSTANCE.getByName(n) == finalChosen)
                .findFirst()
                .orElse("clear");

        System.out.println("[GreyHorizons] Rolled weather: " + name
                + " (duration=" + duration + ", intensity=" + intensity + ")");

        return new RollResult(chosen, name, duration, intensity);
    }
    // Biome filtering
    private List<WeatherDefinition> filterByBiomeTemp(List<WeatherDefinition> pool,
                                                      float avgTemp) {
        return pool.stream().filter(def -> {
            if (avgTemp >= 1.0f) {
                if (def.hasRain() && !def.hasThunder()
                        && def.intensity().max() <= 0.5f) {
                    return false;
                }
            }
            if (avgTemp <= 0.15f) {
                if (!def.hasRain() && !def.hasThunder() && def.isSpecial()) {
                    return false;
                }
            }
            return true;
        }).toList();
    }
    private float getAverageBiomeTemp(ServerLevel level) {
        try {
            BlockPos spawn = level.getSharedSpawnPos();
            float temp = level.getBiome(spawn).value().getBaseTemperature();
            float t1 = level.getBiome(spawn.offset(100, 0, 0)).value().getBaseTemperature();
            float t2 = level.getBiome(spawn.offset(-100, 0, 0)).value().getBaseTemperature();
            float t3 = level.getBiome(spawn.offset(0, 0, 100)).value().getBaseTemperature();
            float t4 = level.getBiome(spawn.offset(0, 0, -100)).value().getBaseTemperature();
            return (temp + t1 + t2 + t3 + t4) / 5.0f;
        } catch (Exception e) {
            return 0.5f;
        }
    }
    // Result
    public record RollResult(
            WeatherDefinition definition,
            String name,
            int durationTicks,
            float intensity
    ) {
        public static RollResult fallback(WeatherDefinition clear) {
            return new RollResult(clear, "clear", 6000, 0.0f);
        }
    }
}