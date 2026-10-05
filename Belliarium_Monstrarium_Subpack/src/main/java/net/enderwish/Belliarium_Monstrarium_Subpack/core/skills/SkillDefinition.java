package net.enderwish.Belliarium_Monstrarium_Subpack.core.skills;

import net.enderwish.Belliarium_Monstrarium_Subpack.core.gear.ItemRarity;

import java.util.List;

public record SkillDefinition(String skillId, ItemRarity rarity, List<String> pages, int learningTimeTicksOverride) {

    /** -1 means "derive from rarity" -- common=30s, legendary=10min, first-pass numbers. */
    public int getLearningTimeTicks() {
        if (learningTimeTicksOverride > 0) return learningTimeTicksOverride;
        return switch (rarity) {
            case COMMON -> 600;
            case UNCOMMON -> 1800;
            case RARE -> 4800;
            case LEGENDARY -> 12000;
        };
    }
}
