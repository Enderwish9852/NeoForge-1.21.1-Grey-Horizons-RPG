package net.enderwish.Belliarium_Monstrarium_Subpack.core.gear;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.GsonHelper;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * ToolProfileRegistry
 *
 * One JSON file per item -- same pattern as Atmospheric's HeatBlockRegistry
 * -- under data/gh_belliarium_monstrarium/tool_profiles/<name>.json:
 *   {
 *     "item": "gh_farming_overhaul:knife",
 *     "rarity": "COMMON",
 *     "traits": [
 *       { "name": "combat", "description": "Range 1" },
 *       { "name": "fast", "description": "Faster draw speed, faster attack sequence." }
 *     ]
 *   }
 *
 * An item with no file here is just a normal vanilla item -- no tooltip,
 * no rarity, no repair limit. Matches "tools for now, add more as we build
 * the foundation": start this folder empty except for actual tool items,
 * expand later.
 *
 * NOTE -- lives at data/gh_belliarium_monstrarium/tool_profiles/, NOT under
 * recipe/ -- the 1.21 plural->singular rename only affected a specific
 * fixed list of vanilla folder names (recipe, loot_table, advancement,
 * item_modifier, predicate, structure). Custom folders like this one were
 * never affected and can be named whatever you want.
 */
public class ToolProfileRegistry implements ResourceManagerReloadListener {

    public static final ToolProfileRegistry INSTANCE = new ToolProfileRegistry();
    private ToolProfileRegistry() {}

    private static final Gson GSON = new GsonBuilder().create();
    private static final String FOLDER = "tool_profiles";
    private static final String NAMESPACE = "gh_belliarium_monstrarium";

    private final Map<String, ToolProfileDefinition> profiles = new HashMap<>();

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        profiles.clear();

        Map<ResourceLocation, Resource> resources = manager.listResources(FOLDER,
                path -> path.getNamespace().equals(NAMESPACE) && path.getPath().endsWith(".json"));

        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonObject json = GsonHelper.fromJson(GSON, reader, JsonElement.class).getAsJsonObject();
                String itemId = GsonHelper.getAsString(json, "item");
                ItemRarity rarity = ItemRarity.valueOf(GsonHelper.getAsString(json, "rarity").toUpperCase());

                List<ToolProfileDefinition.ToolTrait> traits = new ArrayList<>();
                JsonArray traitArray = json.getAsJsonArray("traits");
                if (traitArray != null) {
                    for (JsonElement el : traitArray) {
                        JsonObject traitObj = el.getAsJsonObject();
                        traits.add(new ToolProfileDefinition.ToolTrait(
                                GsonHelper.getAsString(traitObj, "name"),
                                GsonHelper.getAsString(traitObj, "description")));
                    }
                }

                profiles.put(itemId, new ToolProfileDefinition(rarity, traits));
            } catch (IOException | RuntimeException e) {
                System.err.println("[GHBelliarium] Failed to parse tool profile " + entry.getKey() + ": " + e.getMessage());
            }
        }

        System.out.println("[GHBelliarium] ToolProfileRegistry loaded " + profiles.size() + " tool profiles.");
    }

    public Optional<ToolProfileDefinition> getProfile(String itemId) {
        return Optional.ofNullable(profiles.get(itemId));
    }
}
