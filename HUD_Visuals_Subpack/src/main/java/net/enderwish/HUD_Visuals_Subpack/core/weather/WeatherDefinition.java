package net.enderwish.HUD_Visuals_Subpack.core.weather;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.enderwish.HUD_Visuals_Subpack.core.season.SeasonCalendar;

import java.util.Map;
import java.util.Random;

public record WeatherDefinition(
        boolean hasRain,
        boolean hasThunder,
        boolean isSpecial,
        DurationRange duration,
        IntensityRange intensity,
        Map<String, Map<String, Integer>> seasonWeights

){
   // Codec
   public static final Codec<WeatherDefinition> CODEC = RecordCodecBuilder.create(instance ->
           instance.group(
                   Codec.BOOL.fieldOf("has_rain").forGetter(WeatherDefinition::hasRain),
                   Codec.BOOL.fieldOf("has_thunder").forGetter(WeatherDefinition::hasThunder),
                   Codec.BOOL.fieldOf("is_special").forGetter(WeatherDefinition::isSpecial),
                   DurationRange.CODEC.fieldOf("duration").forGetter(WeatherDefinition::duration),
                   IntensityRange.CODEC.fieldOf("intensity").forGetter(WeatherDefinition::intensity),
                   Codec.unboundedMap(
                           Codec.STRING,
                           Codec.unboundedMap(Codec.STRING, Codec.INT)
                   ).fieldOf("season_weights").forGetter(WeatherDefinition::seasonWeights)

           ).apply(instance, WeatherDefinition::new)
   );
   // Rolling
    public int rollDuration(Random rand) {
        if (isSpecial) return duration.min();
        return duration.min() + rand.nextInt(duration.max() - duration.min() + 1);
    }
    public float rollIntensity(Random rand) {
        if (isSpecial) return intensity.min();
        float rolled = intensity.min() + rand.nextFloat() * (intensity.max() -intensity.min());
        return Math.min(rolled, 0.99f);
    }
    // Weight lookup
    public int getWeight(SeasonCalendar.Season season, SeasonCalendar.Phase phase) {
        Map<String, Integer> phaseMap = seasonWeights.get(season.name());
        if (phaseMap ==null) return 0;
        return phaseMap.getOrDefault(phase.name(), 0);
    }
    public boolean isInPool(SeasonCalendar.Season season, SeasonCalendar.Phase phase) {
        return getWeight(season, phase) > 0;
    }
    // Nested records
    public record DurationRange(int min,int max) {
        public static final Codec<DurationRange> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.INT.fieldOf("min").forGetter(DurationRange::min),
                        Codec.INT.fieldOf("max").forGetter(DurationRange::max)
                ).apply(instance, DurationRange::new)
        );
    }
    public record IntensityRange(float min, float max) {
        public static final Codec<IntensityRange> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.FLOAT.fieldOf("min").forGetter(IntensityRange::min),
                        Codec.FLOAT.fieldOf("max").forGetter(IntensityRange::max)
                ).apply(instance, IntensityRange::new)
        );
    }
}
