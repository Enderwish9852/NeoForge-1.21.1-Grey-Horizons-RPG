package net.enderwish.Belliarium_Monstrarium_Subpack.item;

import net.enderwish.Belliarium_Monstrarium_Subpack.core.ModDataComponents;
import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.item.gear.BackpackItem;
import net.enderwish.Belliarium_Monstrarium_Subpack.item.medical.*;
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

    // ── Backpacks (Curios "back" slot; tuning in data/backpacks/*.json) ──────
    public static final DeferredItem<Item> SCHOOL_BAG = ITEMS.register("school_bag",
            () -> new BackpackItem(new Item.Properties().stacksTo(1)
                    .component(ModDataComponents.BACKPACK_ID.get(), "school_bag")));
    public static final DeferredItem<Item> RUCKSACK = ITEMS.register("rucksack",
            () -> new BackpackItem(new Item.Properties().stacksTo(1)
                    .component(ModDataComponents.BACKPACK_ID.get(), "rucksack")));
    public static final DeferredItem<Item> HIKING_BAG = ITEMS.register("hiking_bag",
            () -> new BackpackItem(new Item.Properties().stacksTo(1)
                    .component(ModDataComponents.BACKPACK_ID.get(), "hiking_bag")));
    public static final DeferredItem<Item> SURVIVORS_BACKPACK = ITEMS.register("survivors_backpack",
            () -> new BackpackItem(new Item.Properties().stacksTo(1)
                    .component(ModDataComponents.BACKPACK_ID.get(), "survivors_backpack")));

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

    public static final DeferredItem<Item> FIRST_AID_KIT = ITEMS.register("first_aid_kit",
            () -> new FirstAidKitItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> BANDAGE = ITEMS.register("bandage",
            () -> new BandageItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> SUTURE_KIT = ITEMS.register("suture_kit",
            () -> new SutureKitItem(new Item.Properties().stacksTo(8)));
    public static final DeferredItem<Item> SPLINT = ITEMS.register("splint",
            () -> new SplintItem(new Item.Properties().stacksTo(8)));
    public static final DeferredItem<Item> PAINKILLER = ITEMS.register("painkiller",
            () -> new PainkillerItem(new Item.Properties().stacksTo(8)));
    public static final DeferredItem<Item> ADRENALINE = ITEMS.register("adrenaline",
            () -> new AdrenalineItem(new Item.Properties().stacksTo(4)));

    public static final DeferredItem<Item> SURVIVORS_DIARY = ITEMS.register("survivors_diary",
            () -> new net.enderwish.Belliarium_Monstrarium_Subpack.item.SurvivorsDiaryItem(
                    new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    // ── Legendary map (creative only; one per adventure) ────────────────────
    public static final DeferredItem<Item> LEGENDARY_MAP = ITEMS.register("legendary_map",
            () -> new MapItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
