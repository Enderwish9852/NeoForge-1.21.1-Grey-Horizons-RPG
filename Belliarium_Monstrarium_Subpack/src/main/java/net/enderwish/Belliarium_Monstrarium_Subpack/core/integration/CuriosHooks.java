package net.enderwish.Belliarium_Monstrarium_Subpack.core.integration;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * CuriosHooks
 *
 * NEW -- capture()/restore() for the Survivor's Diary rewind, so the survival
 * watch is rewound along with the rest of the inventory.
 *
 * OWNED_SLOT_IDS must list every Curios slot this mod defines
 * (data/gh_belliarium_monstrarium/curios/slots/). Add new slot ids here when you
 * add new slots, or they won't be rewound.
 *
 * Restoring writes straight into the slot handlers. Curios notices the change
 * on its own tick and handles syncing and equip callbacks, which is the
 * normal path other mods use.
 *
 * VERIFY IF COMPILE FAILS -- setStackInSlot on handler.getStacks() (relies on
 * IDynamicStackHandler extending IItemHandlerModifiable, confirmed only from
 * old Curios changelogs, not from 9.5.1 docs).
 */
public final class CuriosHooks {

    private static final List<String> OWNED_SLOT_IDS = List.of("wrist");

    private CuriosHooks() {}

    public static boolean hasWatchEquipped(Player player) {
        AtomicBoolean found = new AtomicBoolean(false);
        CuriosApi.getCuriosInventory(player).ifPresent(inventory ->
                inventory.getStacksHandler("wrist").ifPresent(handler -> {
                    for (int i = 0; i < handler.getStacks().getSlots(); i++) {
                        if (handler.getStacks().getStackInSlot(i).is(
                                net.enderwish.Belliarium_Monstrarium_Subpack.item.ModItems.SURVIVAL_WATCH.get())) {
                            found.set(true);
                            break;
                        }
                    }
                }));
        return found.get();
    }

    /** One list of {index, item} entries per owned slot id. */
    public static CompoundTag capture(Player player) {
        CompoundTag snapshot = new CompoundTag();
        HolderLookup.Provider registries = player.level().registryAccess();

        CuriosApi.getCuriosInventory(player).ifPresent(inventory -> {
            for (String slotId : OWNED_SLOT_IDS) {
                inventory.getStacksHandler(slotId).ifPresent(handler -> {
                    ListTag entries = new ListTag();
                    for (int i = 0; i < handler.getStacks().getSlots(); i++) {
                        ItemStack stack = handler.getStacks().getStackInSlot(i);
                        if (stack.isEmpty()) continue;

                        CompoundTag entry = new CompoundTag();
                        entry.putInt("index", i);
                        entry.put("item", stack.save(registries));
                        entries.add(entry);
                    }
                    snapshot.put(slotId, entries);
                });
            }
        });
        return snapshot;
    }

    /** Clears every owned slot, then puts back exactly what the snapshot held. */
    public static void restore(Player player, CompoundTag snapshot) {
        HolderLookup.Provider registries = player.level().registryAccess();

        CuriosApi.getCuriosInventory(player).ifPresent(inventory -> {
            for (String slotId : OWNED_SLOT_IDS) {
                inventory.getStacksHandler(slotId).ifPresent(handler -> {
                    var stacks = handler.getStacks();
                    for (int i = 0; i < stacks.getSlots(); i++) {
                        stacks.setStackInSlot(i, ItemStack.EMPTY);
                    }

                    ListTag entries = snapshot.getList(slotId, Tag.TAG_COMPOUND);
                    for (int e = 0; e < entries.size(); e++) {
                        CompoundTag entry = entries.getCompound(e);
                        int index = entry.getInt("index");
                        if (index < 0 || index >= stacks.getSlots() || !entry.contains("item")) continue;

                        ItemStack stack = ItemStack.parse(registries, entry.get("item")).orElse(ItemStack.EMPTY);
                        stacks.setStackInSlot(index, stack);
                    }
                });
            }
        });
    }
}
