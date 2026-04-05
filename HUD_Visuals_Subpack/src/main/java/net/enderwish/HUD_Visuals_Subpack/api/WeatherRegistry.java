package net.enderwish.HUD_Visuals_Subpack.api;

import net.enderwish.HUD_Visuals_Subpack.core.Season;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import java.util.*;

public class WeatherRegistry {
    public static final String MOD_ID = "gh_hud_visuals";

    public static final ResourceKey<Registry<WeatherType>> WEATHER_REGISTRY_KEY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(MOD_ID, "weathers"));

    // --- TAG DEFINITIONS ---
    public static final TagKey<WeatherType> IS_STORM = TagKey.create(WEATHER_REGISTRY_KEY,
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "is_storm"));

    public static final TagKey<WeatherType> HAS_WIND = TagKey.create(WEATHER_REGISTRY_KEY,
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "has_wind"));

    public static final TagKey<WeatherType> IS_THUNDER = TagKey.create(WEATHER_REGISTRY_KEY,
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "is_thunder"));

    public static final TagKey<WeatherType> IS_FREEZING = TagKey.create(WEATHER_REGISTRY_KEY,
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "is_freezing"));

    private static final Map<String, WeatherType> REGISTERED_WEATHERS = new HashMap<>();

    // --- REGISTRATION ---
    public static final WeatherType CLEAR = register("clear", 0.0f, 0.0f, WeatherType.WeatherRarity.COMMON, false, 0xC0D8FF, 0.0f);
    public static final WeatherType RAIN = register("rain", -1.0f, 0.4f, WeatherType.WeatherRarity.COMMON, true, 0x667788, 0.6f);
    public static final WeatherType THUNDER = register("thunder", -2.0f, 0.6f, WeatherType.WeatherRarity.UNCOMMON, true, 0x181820, 1.0f);
    public static final WeatherType WIND = register("wind", -3.0f, 0.0f, WeatherType.WeatherRarity.COMMON, false, 0xC0D8FF, 0.1f);
    public static final WeatherType FOG = register("fog", -3.0f, 0.2f, WeatherType.WeatherRarity.COMMON, false, 0x909090, 0.3f);
    public static final WeatherType CLOUDY = register("cloudy", -2.0f, 0.0f, WeatherType.WeatherRarity.COMMON, false, 0x707580, 0.4f);

    public static final WeatherType SNOW = register("snow", -4.0f, 0.2f, WeatherType.WeatherRarity.COMMON, true, 0xFFFFFF, 0.5f);
    public static final WeatherType SNOW_STORM = register("snow_storm", -6.0f, 0.4f, WeatherType.WeatherRarity.COMMON, true, 0xE0E0E0, 0.7f);
    public static final WeatherType BLIZZARD = register("blizzard", -10.0f, 0.5f, WeatherType.WeatherRarity.COMMON, true, 0xD0D0D0, 0.9f);
    public static final WeatherType HAIL = register("hail", -8.0f, 0.3f, WeatherType.WeatherRarity.COMMON, true, 0x8899AA, 0.8f);

    public static final WeatherType HEATWAVE = register("heatwave", 10.0f, -0.4f, WeatherType.WeatherRarity.COMMON, false, 0xFFD700, 0.2f);
    public static final WeatherType DRAUGHT = register("draught", 10.0f, -0.8f, WeatherType.WeatherRarity.COMMON, false, 0xD2B48C, 0.1f);

    public static final WeatherType POLLEN_HAZE = register("pollen_haze", 1.0f, 0.1f, WeatherType.WeatherRarity.RARE, false, 0xE5E1B5, 0.2f);
    public static final WeatherType DIAMOND_DUST = register("diamond_dust", -20.0f, 0.0f, WeatherType.WeatherRarity.RARE, true, 0xF0F8FF, 0.4f);
    public static final WeatherType THAW = register("thaw", 10.0f, 0.8f, WeatherType.WeatherRarity.RARE, true, 0xA0A0A0, 0.4f);

    private static WeatherType register(String name, float temp, float wet, WeatherType.WeatherRarity rarity, boolean precip, int fog, float sky) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(MOD_ID, name);
        WeatherType type = new WeatherType(id, temp, wet, rarity == WeatherType.WeatherRarity.RARE, rarity, precip, fog, sky);
        REGISTERED_WEATHERS.put(name, type);
        REGISTERED_WEATHERS.put(id.toString(), type);
        return type;
    }

    // --- UTILITIES ---

    /**
     * Professional "Manual Bridge" for tags.
     * Handles logic based on WeatherType properties if the registry isn't fully initialized.
     */
    public static boolean is(WeatherType type, TagKey<WeatherType> tag) {
        if (type == null) return false;

        String path = type.id().getPath();

        // 1. Storm Check: Anything that triggers vanilla rain/snow rendering
        if (tag.equals(IS_STORM)) {
            return type.hasPrecipitation();
        }

        // 2. Thunder Check: High-intensity storms with lightning potential
        if (tag.equals(IS_THUNDER)) {
            return path.equals("thunder") || path.equals("hail");
        }

        // 3. Wind Check: Triggers sideways particle movement in ClimateHooks
        if (tag.equals(HAS_WIND)) {
            return type.hasPrecipitation() || path.equals("wind");
        }

        // 4. Freezing Check: Temperatures below zero that sustain snow/ice
        if (tag.equals(IS_FREEZING)) {
            return type.tempModifier() <= -4.0f;
        }

        return false;
    }

    public static WeatherType getById(String id) {
        if (id == null) return CLEAR;
        String key = id.contains(":") ? id.split(":")[1] : id;
        return REGISTERED_WEATHERS.getOrDefault(key, CLEAR);
    }

    public static WeatherType getRandomFromList(String... names) {
        List<WeatherType> pool = new ArrayList<>();
        for (String name : names) {
            WeatherType t = REGISTERED_WEATHERS.get(name.toLowerCase());
            if (t != null) pool.add(t);
        }
        return pool.isEmpty() ? CLEAR : pool.get(new Random().nextInt(pool.size()));
    }

    public static WeatherType getRandomWeatherForSeason(Season season, WeatherType.WeatherRarity rarity) {
        List<WeatherType> matches = REGISTERED_WEATHERS.values().stream()
                .distinct()
                .filter(w -> w.rarity() == rarity)
                .toList();

        // Add specific fallback logic for rare seasons if needed, otherwise return random
        return matches.isEmpty() ? CLEAR : matches.get(new Random().nextInt(matches.size()));
    }

    public static Collection<WeatherType> getAll() {
        return new HashSet<>(REGISTERED_WEATHERS.values());
    }
}

