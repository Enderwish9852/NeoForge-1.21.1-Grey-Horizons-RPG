package net.enderwish.Belliarium_Monstrarium_Subpack.core.medical;

import com.mojang.serialization.Codec;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public record FirstAidKitContents(List<ItemStack> items) {

    public static final int MAX_SLOTS = 6;
    public static final int ADRENALINE_SLOT_INDEX = 5;

    public static final Codec<FirstAidKitContents> CODEC =
            ItemStack.OPTIONAL_CODEC.listOf().xmap(FirstAidKitContents::new, FirstAidKitContents::items);

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FirstAidKitContents other)) return false;
        if (items.size() != other.items.size()) return false;
        for (int i = 0; i < items.size(); i++) {
            if (!ItemStack.matches(items.get(i), other.items.get(i))) return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int h = 0;
        for (ItemStack s : items) {
            h = h * 31 + s.getItem().hashCode();
            h = h * 31 + s.getCount();
        }
        return h;
    }
}
