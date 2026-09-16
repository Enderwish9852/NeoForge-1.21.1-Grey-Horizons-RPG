package net.enderwish.Atmospheric_Overhaul_Subpack.core.temperature;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

/**
 * HeatBlockRegistry
 *
 * Loads all heat-emitting/absorbing block definitions from JSON at
 * resource reload — same pattern as CropRegistry/WeatherRegistry.
 *
 * JSON files live at: data/gh_atmospheric/heat_blocks/*.json
 * One block per file (matching cutting_board_recipes' one-file-per-entry
 * style) since heat blocks are simple enough not to need grouping.
 */
public class HeatBlockRegistry implements ResourceManagerReloadListener {

    public static final HeatBlockRegistry INSTANCE = new HeatBlockRegistry();
    private HeatBlockRegistry() {}

    private static final Logger LOGGER = LoggerFactory.getLogger("GHAtmospheric/HeatBlockRegistry");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String FOLDER = "heat_blocks";
    private static final String NAMESPACE = "gh_atmospheric";

    private final Map<String, HeatBlockDefinition> definitions = new HashMap<>();

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        definitions.clear();

        Map<ResourceLocation, Resource> resources = manager.listResources(
                FOLDER,
                path -> path.getNamespace().equals(NAMESPACE) && path.getPath().endsWith(".json")
        );

        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            ResourceLocation fileId = entry.getKey();

            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement json = GsonHelper.fromJson(GSON, reader, JsonElement.class);
                HeatBlockDefinition.CODEC.parse(JsonOps.INSTANCE, json)
                        .resultOrPartial(error ->
                                LOGGER.error("Failed to parse heat block {}: {}", fileId, error))
                        .ifPresent(def -> {
                            definitions.put(def.block(), def);
                            LOGGER.info("Loaded heat block: {}", def.block());
                        });
            } catch (IOException e) {
                LOGGER.error("Could not read heat block JSON {}: {}", fileId, e.getMessage());
            }
        }

        LOGGER.info("HeatBlockRegistry loaded {} heat block definitions.", definitions.size());
    }

    /**
     * Returns the temperature contribution for a given block state, or
     * 0.0 if it has no thermal effect or (for lit-only blocks) is unlit.
     */
    public float getTemperature(BlockState state) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        HeatBlockDefinition def = definitions.get(id.toString());
        if (def == null) return 0.0f;

        if (def.requiresLit()) {
            return isLit(state) ? def.temperature() : 0.0f;
        }
        return def.temperature();
    }

    private static boolean isLit(BlockState state) {
        try {
            return state.getValue(BlockStateProperties.LIT);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isLoaded() {
        return !definitions.isEmpty();
    }
}