package net.enderwish.Belliarium_Monstrarium_Subpack.core.gear;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

/**
 * ArmorToughnessRegistry
 *
 * Exact same pattern as WeightRegistry -- item ID -> a flat float, from
 * data/gh_belliarium_monstrarium/armor_toughness/*.json, e.g.
 * { "minecraft:diamond_chestplate": 10.0, "minecraft:iron_chestplate": 4.0 }
 *
 * Deliberately a SEPARATE value from vanilla's own built-in armor toughness
 * attribute -- your formula is a full replacement of vanilla's own damage
 * math, not an adjustment to it (see the open question below about what
 * this means for vanilla's own armor attribute still being active).
 * Unregistered armor defaults to 0 toughness (pure durability-only
 * blocking).
 *
 * This is the DATA layer only -- the actual damage-interception handler
 * is intentionally not included yet.
 */
public class ArmorToughnessRegistry implements ResourceManagerReloadListener {

    public static final ArmorToughnessRegistry INSTANCE = new ArmorToughnessRegistry();
    private ArmorToughnessRegistry() {}

    private static final Gson GSON = new GsonBuilder().create();
    private static final String FOLDER = "armor_toughness";
    private static final String NAMESPACE = "gh_belliarium_monstrarium";

    private final Map<String, Float> toughness = new HashMap<>();

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        toughness.clear();

        Map<ResourceLocation, Resource> resources = manager.listResources(FOLDER,
                path -> path.getNamespace().equals(NAMESPACE) && path.getPath().endsWith(".json"));

        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement json = GsonHelper.fromJson(GSON, reader, JsonElement.class);
                if (!json.isJsonObject()) continue;
                json.getAsJsonObject().entrySet().forEach(e -> toughness.put(e.getKey(), e.getValue().getAsFloat()));
            } catch (IOException ignored) {}
        }
    }

    public float getToughness(ItemStack stack) {
        if (stack.isEmpty()) return 0f;
        String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return toughness.getOrDefault(id, 0f);
    }
}
