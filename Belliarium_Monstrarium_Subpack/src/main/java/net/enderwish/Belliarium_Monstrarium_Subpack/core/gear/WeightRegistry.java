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
 * WeightRegistry
 *
 * Same JSON-per-item-map pattern as CropRegistry/FoodRegistry in
 * Farming_Overhaul_Subpack -- weight per item ID in kg, loaded from
 * data/gh_belliarium_monstrarium/item_weights/*.json, e.g.:
 *   { "minecraft:iron_sword": 1.5, "minecraft:cobblestone": 1.0 }
 *
 * Unregistered items fall back to DEFAULT_WEIGHT_KG rather than 0 --
 * with "thousands of items" to eventually tag, an unweighted item should
 * still count for SOMETHING while its real weight is pending, not be free.
 */
public class WeightRegistry implements ResourceManagerReloadListener {

    public static final WeightRegistry INSTANCE = new WeightRegistry();
    private WeightRegistry() {}

    public static final float DEFAULT_WEIGHT_KG = 0.5f;

    private static final Gson GSON = new GsonBuilder().create();
    private static final String FOLDER = "item_weights";
    private static final String NAMESPACE = "gh_belliarium_monstrarium";

    private final Map<String, Float> weights = new HashMap<>();

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        weights.clear();

        Map<ResourceLocation, Resource> resources = manager.listResources(FOLDER,
                path -> path.getNamespace().equals(NAMESPACE) && path.getPath().endsWith(".json"));

        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement json = GsonHelper.fromJson(GSON, reader, JsonElement.class);
                if (!json.isJsonObject()) continue;
                json.getAsJsonObject().entrySet().forEach(e ->
                        weights.put(e.getKey(), e.getValue().getAsFloat()));
            } catch (IOException ignored) {}
        }
    }

    public float getWeightKg(ItemStack stack) {
        if (stack.isEmpty()) return 0f;
        String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return weights.getOrDefault(id, DEFAULT_WEIGHT_KG) * stack.getCount();
    }
}
