package net.enderwish.Belliarium_Monstrarium_Subpack.core.medical;

import com.mojang.serialization.Codec;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * FirstAidKitContents
 *
 * VERIFY IF COMPILE FAILS -- ItemStack.CODEC as the field name for a
 * full-fidelity (count + components) ItemStack codec is my best-confidence
 * read for 1.21.1; ItemStack.OPTIONAL_CODEC is the other likely candidate.
 */
public record FirstAidKitContents(List<ItemStack> items) {

    public static final int MAX_SLOTS = 6;
    public static final int ADRENALINE_SLOT_INDEX = 5; // only one Adrenaline ever allowed

    public static final Codec<FirstAidKitContents> CODEC =
            ItemStack.CODEC.listOf().xmap(FirstAidKitContents::new, FirstAidKitContents::items);

    public static FirstAidKitContents empty() {
        List<ItemStack> list = new ArrayList<>();
        for (int i = 0; i < MAX_SLOTS; i++) list.add(ItemStack.EMPTY);
        return new FirstAidKitContents(list);
    }

    public FirstAidKitContents withSlot(int index, ItemStack stack) {
        List<ItemStack> copy = new ArrayList<>(items);
        while (copy.size() < MAX_SLOTS) copy.add(ItemStack.EMPTY);
        copy.set(index, stack);
        return new FirstAidKitContents(copy);
    }

    public ItemStack getSlot(int index) {
        return index < items.size() ? items.get(index) : ItemStack.EMPTY;
    }
}
