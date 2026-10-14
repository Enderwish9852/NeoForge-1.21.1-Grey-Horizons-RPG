package net.enderwish.Belliarium_Monstrarium_Subpack.core.gear.backpack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.GsonHelper;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * BackpackRegistry
 *
 * Loads every backpack tier from data/gh_belliarium_monstrarium/backpacks/.
 * Same reload pattern as WeightRegistry. Registered SERVER-side only —
 * the client learns slot counts from BackpackContentsPacket, never from
 * this registry (a client reload would find zero data files).
 */
public class BackpackRegistry implements ResourceManagerReloadListener {

    public static final BackpackRegistry INSTANCE = new BackpackRegistry();
    private BackpackRegistry() {}

    private static final Gson GSON = new GsonBuilder().create();
    private static final String FOLDER = "backpacks";
    private static final String NAMESPACE = "gh_belliarium_monstrarium";

    private final Map<String, BackpackConfig> configs = new HashMap<>();

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        configs.clear();

        Map<ResourceLocation, Resource> resources = manager.listResources(FOLDER,
                path -> path.getNamespace().equals(NAMESPACE) && path.getPath().endsWith(".json"));

        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            String id = entry.getKey().getPath();
            id = id.substring(id.lastIndexOf('/') + 1, id.lastIndexOf('.'));
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement json = GsonHelper.fromJson(GSON, reader, JsonElement.class);
                JsonObject obj = json.getAsJsonObject();
                configs.put(id, new BackpackConfig(
                        id,
                        GsonHelper.getAsInt(obj, "slots", 6),
                        GsonHelper.getAsFloat(obj, "selfWeightKg", 1.0f),
                        GsonHelper.getAsFloat(obj, "carryCapBonusPct", 0f),
                        GsonHelper.getAsFloat(obj, "heavyLoadReliefPct", 0f)));
            } catch (IOException ignored) {}
        }
    }

    public Optional<BackpackConfig> get(String id) {
        return Optional.ofNullable(configs.get(id));
    }

    /** Config or a 6-slot fallback so a missing JSON can't crash the game. */
    public BackpackConfig getOrFallback(String id) {
        return get(id).orElse(new BackpackConfig(id, 6, 1.0f, 0f, 0f));
    }
}
