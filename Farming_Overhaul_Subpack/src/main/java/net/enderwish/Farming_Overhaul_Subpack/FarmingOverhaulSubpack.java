package net.enderwish.Farming_Overhaul_Subpack;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.enderwish.Farming_Overhaul_Subpack.block.ModBlocks;
import net.enderwish.Farming_Overhaul_Subpack.client.ClayPotScreen;
import net.enderwish.Farming_Overhaul_Subpack.client.CuttingBoardScreen;
import net.enderwish.Farming_Overhaul_Subpack.client.SpoilageTooltipHandler;
import net.enderwish.Farming_Overhaul_Subpack.core.claypot.ClayPotRecipeRegistry;
import net.enderwish.Farming_Overhaul_Subpack.core.crop.CropRegistry;
import net.enderwish.Farming_Overhaul_Subpack.core.cutting_board.CuttingBoardRecipeRegistry;
import net.enderwish.Farming_Overhaul_Subpack.core.food.FoodRegistry;
import net.enderwish.Farming_Overhaul_Subpack.core.spoilage.ModDataComponents;
import net.enderwish.Farming_Overhaul_Subpack.core.spoilage.SpoilageHandler;
import net.enderwish.Farming_Overhaul_Subpack.core.tree.TreeGenerator;
import net.enderwish.Farming_Overhaul_Subpack.core.tree.TreeRegistry;
import net.enderwish.Farming_Overhaul_Subpack.event.*;
import net.enderwish.Farming_Overhaul_Subpack.init.ModBlockEntities;
import net.enderwish.Farming_Overhaul_Subpack.init.ModMenuTypes;
import net.enderwish.Farming_Overhaul_Subpack.item.ModItems;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(FarmingOverhaulSubpack.MODID)
public class FarmingOverhaulSubpack {
    public static final String MODID = "gh_farming_overhaul";

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // ── Tab 1 — Crops & Seeds ─────────────────────────────────────────────────
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB_CROPS =
            CREATIVE_MODE_TABS.register("tab_crops", () -> CreativeModeTab.builder()
                    .title(Component.translatable("creativetab.gh_farming_overhaul.crops"))
                    .icon(() -> new ItemStack(ModItems.TOMATO.get()))
                    .displayItems((parameters, output) -> {
                        // Vegetables
                        output.accept(ModItems.AUBERGINE.get());
                        output.accept(ModItems.BELL_PEPPER.get());
                        output.accept(ModItems.BROCCOLI.get());
                        output.accept(ModItems.CABBAGE.get());
                        output.accept(ModItems.CORN.get());
                        output.accept(ModItems.COURGETTE.get());
                        output.accept(ModItems.CUCUMBER.get());
                        output.accept(ModItems.GARLIC.get());
                        output.accept(ModItems.LETTUCE.get());
                        output.accept(ModItems.ONION.get());
                        output.accept(ModItems.SWEET_POTATO.get());
                        output.accept(ModItems.TOMATO.get());
                        // Fruits
                        output.accept(ModItems.BANANA.get());
                        output.accept(ModItems.FIG.get());
                        output.accept(ModItems.GUAVA.get());
                        output.accept(ModItems.KIWI.get());
                        output.accept(ModItems.MANGO.get());
                        output.accept(ModItems.ORANGE.get());
                        output.accept(ModItems.PAPAYA.get());
                        output.accept(ModItems.PEACH.get());
                        output.accept(ModItems.PEAR.get());
                        output.accept(ModItems.PERSIMMON.get());
                        output.accept(ModItems.PLUM.get());
                        output.accept(ModItems.POMEGRANATE.get());
                        // Vegetable Seeds
                        output.accept(ModItems.AUBERGINE_SEEDS.get());
                        output.accept(ModItems.BELL_PEPPER_SEEDS.get());
                        output.accept(ModItems.BROCCOLI_SEEDS.get());
                        output.accept(ModItems.CABBAGE_SEEDS.get());
                        output.accept(ModItems.CORN_SEEDS.get());
                        output.accept(ModItems.COURGETTE_SEEDS.get());
                        output.accept(ModItems.CUCUMBER_SEEDS.get());
                        output.accept(ModItems.LETTUCE_SEEDS.get());
                        output.accept(ModItems.TOMATO_SEEDS.get());
                        // Fruit Seeds
                        output.accept(ModItems.FIG_SEEDS.get());
                        output.accept(ModItems.GUAVA_SEEDS.get());
                        output.accept(ModItems.KIWI_SEEDS.get());
                        output.accept(ModItems.MANGO_SEEDS.get());
                        output.accept(ModItems.ORANGE_SEEDS.get());
                        output.accept(ModItems.PAPAYA_SEEDS.get());
                        output.accept(ModItems.PEACH_SEEDS.get());
                        output.accept(ModItems.PEAR_SEEDS.get());
                        output.accept(ModItems.PERSIMMON_SEEDS.get());
                        output.accept(ModItems.PLUM_SEEDS.get());
                        output.accept(ModItems.POMEGRANATE_SEEDS.get());
                    })
                    .build());

    // ── Tab 2 — Prep Ingredients ──────────────────────────────────────────────
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB_PREP =
            CREATIVE_MODE_TABS.register("tab_prep", () -> CreativeModeTab.builder()
                    .title(Component.translatable("creativetab.gh_farming_overhaul.prep"))
                    .icon(() -> new ItemStack(ModItems.CHOPPED_TOMATO.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.CHOPPED_TOMATO.get());
                        output.accept(ModItems.CHOPPED_BELL_PEPPER.get());
                        output.accept(ModItems.CHOPPED_BROCCOLI.get());
                        output.accept(ModItems.SHREDDED_CABBAGE.get());
                        output.accept(ModItems.CABBAGE_LEAF.get());
                        output.accept(ModItems.CORN_KERNELS.get());
                        output.accept(ModItems.SLICED_COURGETTE.get());
                        output.accept(ModItems.SLICED_CUCUMBER.get());
                        output.accept(ModItems.MINCED_GARLIC.get());
                        output.accept(ModItems.SHREDDED_LETTUCE.get());
                        output.accept(ModItems.DICED_ONION.get());
                        output.accept(ModItems.ONION_RINGS.get());
                        output.accept(ModItems.CUBED_SWEET_POTATO.get());
                        output.accept(ModItems.TOMATO_PASTE.get());
                        output.accept(ModItems.PEELED_POTATO.get());
                        output.accept(ModItems.PEELED_CUCUMBER.get());
                        output.accept(ModItems.PEELED_COURGETTE.get());
                        output.accept(ModItems.PEELED_SWEET_POTATO.get());
                        output.accept(ModItems.DICED_POTATO.get());
                        output.accept(ModItems.CHOPPED_CARROT.get());
                        output.accept(ModItems.PEELED_CARROT.get());
                        output.accept(ModItems.GRATED_CARROT.get());
                        output.accept(ModItems.CHOPPED_BEETROOT.get());
                        output.accept(ModItems.GRATED_COURGETTE.get());
                        output.accept(ModItems.SLICED_BANANA.get());
                        output.accept(ModItems.MASHED_BANANA.get());
                        output.accept(ModItems.HALVED_FIG.get());
                        output.accept(ModItems.CUBED_GUAVA.get());
                        output.accept(ModItems.SLICED_KIWI.get());
                        output.accept(ModItems.PEELED_KIWI.get());
                        output.accept(ModItems.CUBED_MANGO.get());
                        output.accept(ModItems.PEELED_MANGO.get());
                        output.accept(ModItems.ORANGE_SLICES.get());
                        output.accept(ModItems.ORANGE_ZEST.get());
                        output.accept(ModItems.CUBED_PAPAYA.get());
                        output.accept(ModItems.SLICED_PEACH.get());
                        output.accept(ModItems.PEELED_PEACH.get());
                        output.accept(ModItems.SLICED_PEAR.get());
                        output.accept(ModItems.PEELED_PEAR.get());
                        output.accept(ModItems.SLICED_PERSIMMON.get());
                        output.accept(ModItems.HALVED_PLUM.get());
                        output.accept(ModItems.APPLE_SLICES.get());
                        output.accept(ModItems.PEELED_APPLE.get());
                        output.accept(ModItems.GRATED_APPLE.get());
                        output.accept(ModItems.MELON_CHUNKS.get());
                        output.accept(ModItems.CUBED_BEEF.get());
                        output.accept(ModItems.MINCED_BEEF.get());
                        output.accept(ModItems.BEEF_STRIPS.get());
                        output.accept(ModItems.CUBED_PORK.get());
                        output.accept(ModItems.MINCED_PORK.get());
                        output.accept(ModItems.PORK_STRIPS.get());
                        output.accept(ModItems.DICED_CHICKEN.get());
                        output.accept(ModItems.CHICKEN_STRIPS.get());
                        output.accept(ModItems.CUBED_MUTTON.get());
                        output.accept(ModItems.MINCED_MUTTON.get());
                        output.accept(ModItems.DICED_RABBIT.get());
                        output.accept(ModItems.FISH_FILLET.get());
                        output.accept(ModItems.FISH_CHUNKS.get());
                        output.accept(ModItems.SALMON_FILLET.get());
                        output.accept(ModItems.SALMON_CHUNKS.get());
                        output.accept(ModItems.BREAD_CRUMBS.get());
                        output.accept(ModItems.EGG_WASH.get());
                    })
                    .build());

    // ── Tab 3 — Meals ────────────────────────────────────────────────────────
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB_MEALS =
            CREATIVE_MODE_TABS.register("tab_meals", () -> CreativeModeTab.builder()
                    .title(Component.translatable("creativetab.gh_farming_overhaul.meals"))
                    .icon(() -> new ItemStack(net.minecraft.world.item.Items.BOWL))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.GARDEN_SALAD.get());
                        output.accept(ModItems.COLESLAW.get());
                    })
                    .build());

    // ── Tab 4 — Equipment ────────────────────────────────────────────────────
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB_EQUIPMENT =
            CREATIVE_MODE_TABS.register("tab_equipment", () -> CreativeModeTab.builder()
                    .title(Component.translatable("creativetab.gh_farming_overhaul.equipment"))
                    .icon(() -> new ItemStack(ModItems.KNIFE.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.WET_CLAY_POT.get());
                        output.accept(ModItems.CLAY_POT.get());
                        output.accept(ModItems.CUTTING_BOARD.get());
                        output.accept(ModItems.KNIFE.get());
                        output.accept(ModItems.CLEAVER.get());
                        output.accept(ModItems.BUNDLE.get());
                        output.accept(ModItems.WOODEN_BUCKET.get());
                        output.accept(ModItems.WOODEN_BUCKET_OF_FERTILIZER.get());
                        output.accept(ModItems.BUCKET_OF_FERTILIZER.get());
                        output.accept(ModItems.GH_COMPOSTER.get());
                        output.accept(ModItems.FERTILIZED_FARMLAND.get());
                    })
                    .build());

    // ── Tab 5 — Misc ─────────────────────────────────────────────────────────
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB_MISC =
            CREATIVE_MODE_TABS.register("tab_misc", () -> CreativeModeTab.builder()
                    .title(Component.translatable("creativetab.gh_farming_overhaul.misc"))
                    .icon(() -> new ItemStack(ModItems.ROTTEN_SCRAP.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.ROTTEN_SCRAP.get());
                        output.accept(ModItems.POTATO_PEEL.get());
                        output.accept(ModItems.CARROT_PEEL.get());
                        output.accept(ModItems.CUCUMBER_PEEL.get());
                        output.accept(ModItems.COURGETTE_PEEL.get());
                        output.accept(ModItems.SWEET_POTATO_PEEL.get());
                        output.accept(ModItems.BANANA_PEEL.get());
                        output.accept(ModItems.KIWI_PEEL.get());
                        output.accept(ModItems.MANGO_PEEL.get());
                        output.accept(ModItems.ORANGE_PEEL.get());
                        output.accept(ModItems.PEACH_PEEL.get());
                        output.accept(ModItems.PEAR_PEEL.get());
                        output.accept(ModItems.APPLE_PEEL.get());
                        output.accept(ModItems.FERTILIZED_FARMLAND.get());
                    })
                    .build());

    // ── Tab 6 — Nature ───────────────────────────────────────────────────────
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB_NATURE =
            CREATIVE_MODE_TABS.register("tab_nature", () -> CreativeModeTab.builder()
                    .title(Component.translatable(
                            "creativetab.gh_farming_overhaul.nature"))
                    .icon(() -> new ItemStack(ModItems.OAK_LOG.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.OAK_LOG.get());
                        output.accept(ModItems.OAK_LEAVES.get());
                    })
                    .build());

    public FarmingOverhaulSubpack(IEventBus modEventBus, ModContainer container) {
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModDataComponents.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModMenuTypes.register(modEventBus);

        modEventBus.addListener(FarmingOverhaulSubpack::onClientReloadListeners);
        modEventBus.addListener(FarmingOverhaulSubpack::onRegisterScreens);

        NeoForge.EVENT_BUS.addListener(FarmingOverhaulSubpack::onAddReloadListeners);
        NeoForge.EVENT_BUS.register(SpoilageHandler.class);
        NeoForge.EVENT_BUS.register(SpoilageTooltipHandler.class);
        NeoForge.EVENT_BUS.register(CropHarvestHandler.class);
        NeoForge.EVENT_BUS.register(FoodPickupHandler.class);
        NeoForge.EVENT_BUS.register(CraftingSpoilageHandler.class);
        NeoForge.EVENT_BUS.register(CookingSpoilageHandler.class);
        NeoForge.EVENT_BUS.register(FruitTreeSeasonHandler.class);
        NeoForge.EVENT_BUS.addListener(FarmingOverhaulSubpack::onRegisterCommands);
    }

    // ── Test command — /spawntree <species> ───────────────────────────────────
    // TODO: Remove after tree system is fully tested

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("spawntree")
                        .then(Commands.argument("species", StringArgumentType.word())
                                .executes(ctx -> spawnTree(ctx, true))  // default = with leaves
                                .then(Commands.literal("noleaves")
                                        .executes(ctx -> spawnTree(ctx, false)))));  // no leaves
    }

    private static int spawnTree(
            CommandContext<CommandSourceStack> ctx,
            boolean withLeaves) throws CommandSyntaxException {

        String species = StringArgumentType.getString(ctx, "species");
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        BlockPos pos = player.blockPosition();

        boolean result = TreeGenerator.generate(
                player.serverLevel(), pos, species,
                player.serverLevel().getRandom(),
                true, withLeaves);

        ctx.getSource().sendSuccess(() ->
                        Component.literal(result
                                ? "Spawned " + species + " tree at " + pos
                                + (withLeaves ? "" : " (no leaves)")
                                : "Failed — check species name"),
                false);
        return 1;
    }

    private static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.CLAY_POT.get(), ClayPotScreen::new);
        event.register(ModMenuTypes.CUTTING_BOARD.get(), CuttingBoardScreen::new);
    }

    private static void onClientReloadListeners(
            RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(CropRegistry.INSTANCE);
        event.registerReloadListener(FoodRegistry.INSTANCE);
        event.registerReloadListener(ClayPotRecipeRegistry.INSTANCE);
        event.registerReloadListener(CuttingBoardRecipeRegistry.INSTANCE);
        event.registerReloadListener(TreeRegistry.INSTANCE);
    }

    private static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(CropRegistry.INSTANCE);
        event.addListener(FoodRegistry.INSTANCE);
        event.addListener(ClayPotRecipeRegistry.INSTANCE);
        event.addListener(CuttingBoardRecipeRegistry.INSTANCE);
        event.addListener(TreeRegistry.INSTANCE);
    }
}