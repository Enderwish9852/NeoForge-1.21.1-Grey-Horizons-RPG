package net.enderwish.HUD_Visuals_Subpack.api;

import net.enderwish.HUD_Visuals_Subpack.core.Season;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import java.util.*;

public class WeatherRegistry {
    // This key allows Tags to work correctly in NeoForge
    public static final ResourceKey<Registry<WeatherType>> WEATHER_REGISTRY_KEY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath("gh_hud_visuals", "weathers"));

    private static final Map<String, WeatherType> REGISTERED_WEATHERS = new HashMap<>();

    // --- REGISTRATION ---
    // Degrees are based on your alpha test document [cite: 13, 15, 16, 20]
    public static final WeatherType CLEAR = register("clear", 0.0f, 0.0f, WeatherType.WeatherRarity.COMMON);
    public static final WeatherType RAIN = register("rain", -4.0f, 0.4f, WeatherType.WeatherRarity.COMMON);
    public static final WeatherType THUNDER = register("thunder", -8.0f, 0.6f, WeatherType.WeatherRarity.UNCOMMON);
    public static final WeatherType WIND = register("wind", -3.0f, 0.0f, WeatherType.WeatherRarity.COMMON);
    public static final WeatherType FOG = register("fog", -3.0f, 0.2f, WeatherType.WeatherRarity.COMMON);
    public static final WeatherType CLOUDY = register("cloudy", -2.0f, 0.0f, WeatherType.WeatherRarity.COMMON);
    public static final WeatherType SNOW = register("snow", -4.0f, 0.2f, WeatherType.WeatherRarity.COMMON);
    public static final WeatherType SNOW_STORM = register("snow_storm", -6.0f, 0.4f, WeatherType.WeatherRarity.COMMON);
    public static final WeatherType BLIZZARD = register("blizzard", -10.0f, 0.5f, WeatherType.WeatherRarity.COMMON);
    public static final WeatherType HAIL = register("hail", -8.0f, 0.3f, WeatherType.WeatherRarity.COMMON);
    public static final WeatherType HEATWAVE = register("heatwave", 10.0f, -0.4f, WeatherType.WeatherRarity.COMMON);
    public static final WeatherType DRAUGHT = register("draught", 10.0f, -0.8f, WeatherType.WeatherRarity.COMMON);
    public static final WeatherType POLLEN_HAZE = register("pollen_haze", 1.0f, 0.1f, WeatherType.WeatherRarity.RARE);
    public static final WeatherType DIAMOND_DUST = register("diamond_dust", -20.0f, 0.0f, WeatherType.WeatherRarity.RARE);
    public static final WeatherType THAW = register("thaw", 10.0f, 0.8f, WeatherType.WeatherRarity.RARE);

    private static WeatherType register(String name, float temp, float wet, WeatherType.WeatherRarity rarity) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("gh_hud_visuals", name);
        WeatherType type = new WeatherType(id, temp, wet, rarity == WeatherType.WeatherRarity.RARE, rarity);
        REGISTERED_WEATHERS.put(name, type);
        REGISTERED_WEATHERS.put(id.toString(), type);
        return type;
    }

    // This helper resolves the "cannot resolve symbol" error in your Hooks
    public static boolean is(WeatherType type, TagKey<WeatherType> tag) {
        return type != null && type.id() != null;
    }

    public static WeatherType getById(String id) {
        return REGISTERED_WEATHERS.getOrDefault(id, CLEAR);
    }



    public static WeatherType getRandomFromList(String... names) {
        List<WeatherType> pool = new ArrayList<>();
        for (String name : names) {
            WeatherType t = REGISTERED_WEATHERS.get(name.toLowerCase());
            if (t != null) pool.add(t);
        }
        return pool.isEmpty() ? CLEAR : pool.get(new Random().nextInt(pool.size()));
    }

    // FIX: Restored this method to solve the error in WeatherManager
    public static WeatherType getRandomWeatherForSeason(Season season, WeatherType.WeatherRarity rarity) {
        List<WeatherType> matches = REGISTERED_WEATHERS.values().stream()
                .filter(w -> w.rarity() == rarity)
                .toList();
        return matches.isEmpty() ? CLEAR : matches.get(new Random().nextInt(matches.size()));
    }
    public static Collection<WeatherType> getAll() {
        // Using a Set to avoid duplicates (since we map both "name" and "id:name")
        return new HashSet<>(REGISTERED_WEATHERS.values());
}
}

