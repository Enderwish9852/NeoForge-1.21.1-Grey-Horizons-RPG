package net.enderwish.Belliarium_Monstrarium_Subpack.datagen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ItemWeightProvider implements DataProvider {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final PackOutput output;

    public ItemWeightProvider(PackOutput output) {
        this.output = output;
    }

    private Map<String, Float> buildWeights() {
        Map<String, Float> w = new LinkedHashMap<>();
        w.put("minecraft:iron_ingot", 0.5f);
        w.put("minecraft:gold_ingot", 0.6f);
        w.put("minecraft:diamond", 0.05f);
        w.put("minecraft:iron_sword", 1.2f);
        w.put("minecraft:iron_pickaxe", 1.6f);
        w.put("minecraft:iron_axe", 1.7f);
        w.put("minecraft:diamond_sword", 1.3f);
        w.put("minecraft:bow", 1.0f);
        w.put("minecraft:shield", 3.0f);
        w.put("minecraft:iron_helmet", 1.8f);
        w.put("minecraft:iron_chestplate", 4.5f);
        w.put("minecraft:iron_leggings", 3.8f);
        w.put("minecraft:iron_boots", 1.5f);
        w.put("minecraft:bread", 0.15f);
        w.put("minecraft:cooked_beef", 0.2f);
        w.put("minecraft:water_bucket", 1.0f);
        w.put("minecraft:cobblestone", 1.0f);
        w.put("minecraft:anvil", 20.0f);
        return w;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        JsonObject json = new JsonObject();
        buildWeights().forEach(json::addProperty);
        Path path = output.getOutputFolder()
                .resolve("data/gh_belliarium_monstrarium/item_weights/vanilla_test_items.json");
        return DataProvider.saveStable(cache, GSON.toJsonTree(json), path);
    }

    @Override
    public String getName() {
        return "GH Belliarium Item Weights";
    }
}
