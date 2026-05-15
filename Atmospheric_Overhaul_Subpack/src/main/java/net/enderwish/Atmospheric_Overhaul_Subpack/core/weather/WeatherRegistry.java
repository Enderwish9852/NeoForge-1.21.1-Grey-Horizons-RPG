package net.enderwish.Atmospheric_Overhaul_Subpack.core.weather;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.enderwish.Atmospheric_Overhaul_Subpack.core.season.SeasonCalendar;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.GsonHelper;

import java.io.IOException;
import java.io.Reader;
import java.util.*;

public class WeatherRegistry implements ResourceManagerReloadListener {
    // Singleton
    public static final WeatherRegistry INSTANCE = new WeatherRegistry();
    // Storage
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String FOLDER = "tags/weathers";
    private final Map<String, WeatherDefinition> definitions = new HashMap<>();
    // Constructor
    private WeatherRegistry() {}
    // Load
    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        definitions.clear();
        Map<ResourceLocation, Resource> resources = manager.listResources(
                FOLDER,
                path -> path.getNamespace().equals("gh_atmospheric")
                        && path.getPath().endsWith(".json")
        );

        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            ResourceLocation id = entry.getKey();

            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement json = GsonHelper.fromJson(GSON, reader, JsonElement.class);
                WeatherDefinition.CODEC.parse(JsonOps.INSTANCE, json)
                        .resultOrPartial(error ->
                                System.err.println("[GreyHorizons] Failed to parse weather JSON "
                                        + id + ": " + error)
                        )
                        .ifPresent(def -> {
                            // Key is the filename without .json e.g. "light_rain"
                            String name = id.getPath()
                                    .replace("tags/weathers/", "")
                                    .replace(".json", "");
                            definitions.put(name, def);
                            System.out.println("[GreyHorizons] Loaded weather: " + name);
                        });

            } catch (IOException e) {
                System.err.println("[GreyHorizons] Could not read weather JSON " + id + ": " + e.getMessage());
            }
        }

        System.out.println("[GreyHorizons] WeatherRegistry loaded " + definitions.size() + " weather definitions.");
    }
    //Queries
    public List<WeatherDefinition> getPool(SeasonCalendar.Season season, SeasonCalendar.Phase phase) {
        List<WeatherDefinition> pool = new ArrayList<>();
        for (WeatherDefinition def : definitions.values()) {
            if (def.isInPool(season, phase)) {
                pool.add(def);
            }
        }
        return pool;
    }
    public WeatherDefinition getByName(String name) {
        return definitions.getOrDefault(name, getFallback());
    }
    public boolean isLoaded() {
        return !definitions.isEmpty();
    }
    public Set<String> getAllNames() {
        return Collections.unmodifiableSet(definitions.keySet());
    }
    // Fallback
    public WeatherDefinition getFallback() {
        return new WeatherDefinition(
                false,
                false,
                false,
                new WeatherDefinition.DurationRange(6000, 6000),
                new WeatherDefinition.IntensityRange(0.0f, 0.0f),
                0.0f,
                false,
                Map.of()
        );
    }
}