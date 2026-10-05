package net.enderwish.Belliarium_Monstrarium_Subpack.core.skills;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashMap;
import java.util.Map;

public class LearnedSkillsCapability {

    private Map<String, Integer> skillKeyBinds;

    public LearnedSkillsCapability() { this(new HashMap<>()); }
    public LearnedSkillsCapability(Map<String, Integer> skillKeyBinds) {
        this.skillKeyBinds = skillKeyBinds;
    }

    public static final Codec<LearnedSkillsCapability> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.unboundedMap(Codec.STRING, Codec.INT)
                    .fieldOf("learned_skills")
                    .forGetter(LearnedSkillsCapability::getSkillKeyBinds)
    ).apply(inst, map -> new LearnedSkillsCapability(new HashMap<>(map))));

    public Map<String, Integer> getSkillKeyBinds() { return skillKeyBinds; }
    public void bind(String skillId, int keyCode) { skillKeyBinds.put(skillId, keyCode); }
    public String getSkillUsingKey(int keyCode) {
        return skillKeyBinds.entrySet().stream()
                .filter(e -> e.getValue() == keyCode)
                .map(Map.Entry::getKey)
                .findFirst().orElse(null);
    }
    public Integer getKeyFor(String skillId) { return skillKeyBinds.get(skillId); }
    public void removeAll() { skillKeyBinds.clear(); }
    public boolean removeOne(String skillId) { return skillKeyBinds.remove(skillId) != null; }
}
