package net.enderwish.Belliarium_Monstrarium_Subpack.core.skills;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.gear.ItemRarity;
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
 * SkillRegistry
 *
 * One JSON file per skill, same pattern as ToolProfileRegistry, under
 * data/gh_belliarium_monstrarium/skills/<id>.json:
 *   {
 *     "skill": "swift_strike",
 *     "rarity": "COMMON",
 *     "pages": ["placeholder flavor text..."],
 *     "learning_time_ticks": -1
 *   }
 * Intentionally empty for now -- no actual skill files ship with this
 * batch, per "the actual skills... will come next." This just defines the
 * shape the rest of the framework reads.
 */
public class SkillRegistry implements ResourceManagerReloadListener {

    public static final SkillRegistry INSTANCE = new SkillRegistry();
    private SkillRegistry() {}

    private static final Gson GSON = new GsonBuilder().create();
    private static final String FOLDER = "skills";
    private static final String NAMESPACE = "gh_belliarium_monstrarium";

    private final Map<String, SkillDefinition> skills = new HashMap<>();

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        skills.clear();

        Map<ResourceLocation, Resource> resources = manager.listResources(FOLDER,
                path -> path.getNamespace().equals(NAMESPACE) && path.getPath().endsWith(".json"));

        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonObject json = GsonHelper.fromJson(GSON, reader, JsonElement.class).getAsJsonObject();
                String skillId = GsonHelper.getAsString(json, "skill");
                ItemRarity rarity = ItemRarity.valueOf(GsonHelper.getAsString(json, "rarity").toUpperCase());

                List<String> pages = new ArrayList<>();
                JsonArray pageArray = json.getAsJsonArray("pages");
                if (pageArray != null) {
                    for (JsonElement el : pageArray) pages.add(el.getAsString());
                }

                int timeOverride = GsonHelper.getAsInt(json, "learning_time_ticks", -1);
                skills.put(skillId, new SkillDefinition(skillId, rarity, pages, timeOverride));
            } catch (IOException | RuntimeException e) {
                System.err.println("[GHBelliarium] Failed to parse skill " + entry.getKey() + ": " + e.getMessage());
            }
        }

        System.out.println("[GHBelliarium] SkillRegistry loaded " + skills.size() + " skills.");
    }

    public Optional<SkillDefinition> getSkill(String skillId) {
        return Optional.ofNullable(skills.get(skillId));
    }
}
