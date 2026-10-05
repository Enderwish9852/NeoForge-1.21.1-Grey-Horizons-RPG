package net.enderwish.Belliarium_Monstrarium_Subpack.core.gear;

import java.util.List;

/** Traits are freeform per item/weapon type, per your spec -- not a fixed master list. */
public record ToolProfileDefinition(ItemRarity rarity, List<ToolTrait> traits) {
    public record ToolTrait(String name, String description) {}
}
