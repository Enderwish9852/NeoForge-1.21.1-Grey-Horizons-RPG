package net.enderwish.Belliarium_Monstrarium_Subpack.item;

import net.enderwish.Belliarium_Monstrarium_Subpack.core.ModDataComponents;
import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * ModItems
 *
 * CHANGED -- "every skill needs their own book naming the skill" replaces
 * the single generic SKILL_BOOK with 5 named items, one per common skill.
 * Same SkillBookItem class for all of them -- behavior is entirely driven
 * by the SKILL_ID component, so no new Item subclass was needed, just a
 * default component value baked in at registration via
 * Item.Properties#component (the same standard 1.20.5+ mechanism vanilla
 * itself uses for default durability/food values on its own items).
 */
public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BelliariumMonstrariumSubpack.MODID);

    public static final DeferredItem<Item> SURVIVAL_WATCH = ITEMS.register("survival_watch",
            () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final DeferredItem<Item> SKILL_BOOK_DASH = ITEMS.register("skill_book_dash",
            () -> new SkillBookItem(new Item.Properties().stacksTo(1)
                    .component(ModDataComponents.SKILL_ID.get(), "dash")));

    public static final DeferredItem<Item> SKILL_BOOK_SLIDE = ITEMS.register("skill_book_slide",
            () -> new SkillBookItem(new Item.Properties().stacksTo(1)
                    .component(ModDataComponents.SKILL_ID.get(), "slide")));

    public static final DeferredItem<Item> SKILL_BOOK_ROLL = ITEMS.register("skill_book_roll",
            () -> new SkillBookItem(new Item.Properties().stacksTo(1)
                    .component(ModDataComponents.SKILL_ID.get(), "roll")));

    public static final DeferredItem<Item> SKILL_BOOK_KICK = ITEMS.register("skill_book_kick",
            () -> new SkillBookItem(new Item.Properties().stacksTo(1)
                    .component(ModDataComponents.SKILL_ID.get(), "kick")));

    public static final DeferredItem<Item> SKILL_BOOK_PARRY = ITEMS.register("skill_book_parry",
            () -> new SkillBookItem(new Item.Properties().stacksTo(1)
                    .component(ModDataComponents.SKILL_ID.get(), "parry")));

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
