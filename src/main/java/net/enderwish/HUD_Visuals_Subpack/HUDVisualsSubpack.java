package net.enderwish.HUD_Visuals_Subpack;

import net.enderwish.HUD_Visuals_Subpack.client.gui.SportsWatchHUD;
import net.enderwish.HUD_Visuals_Subpack.common.items.SportsWatchItem;
import net.enderwish.HUD_Visuals_Subpack.core.LimbDamageEventHandler;
import net.enderwish.HUD_Visuals_Subpack.core.ModAttachments;
import net.enderwish.HUD_Visuals_Subpack.event.HealthRegenEvents;
import net.enderwish.HUD_Visuals_Subpack.network.ModMessages;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

@Mod(HUDVisualsSubpack.MOD_ID)
public class HUDVisualsSubpack {
    public static final String MOD_ID = "gh_hud_visuals";

    // Registries
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    // Register the Sports Watch using the custom class
    public static final DeferredItem<Item> SPORTS_WATCH = ITEMS.register("sports_watch",
            () -> new SportsWatchItem(new Item.Properties()));

    // Creative Tab setup
    public static final Supplier<CreativeModeTab> SPORTS_TAB = CREATIVE_TABS.register("sports_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("creativetab.sports_tab"))
                    .icon(() -> new ItemStack(SPORTS_WATCH.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(SPORTS_WATCH.get());
                    })
                    .build());

    public HUDVisualsSubpack(IEventBus modEventBus) {
        // Registering registries to the bus
        ITEMS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);

        // Capability registration
        ModAttachments.register(modEventBus);

        // Network registration
        modEventBus.addListener(this::registerNetworking);

        // Global Event Bus registrations
        NeoForge.EVENT_BUS.register(new HealthRegenEvents());
        NeoForge.EVENT_BUS.register(LimbDamageEventHandler.class);

        // Client-side only registrations
        if (FMLEnvironment.dist.isClient()) {
            modEventBus.addListener(this::onRegisterGuiLayers);
        }
    }

    private void registerNetworking(final RegisterPayloadHandlersEvent event) {
        ModMessages.register(event);
    }

    private void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        // Registers your custom Watch HUD above the hotbar
        event.registerAbove(
                VanillaGuiLayers.HOTBAR,
                ResourceLocation.fromNamespaceAndPath(MOD_ID, "sports_watch"),
                (graphics, delta) -> SportsWatchHUD.SPORTS_WATCH_ELEMENT.render(graphics, delta)
        );

        // Hide vanilla HUD elements to make room for the watch display
        event.replaceLayer(VanillaGuiLayers.PLAYER_HEALTH, (gui, delta) -> {});
        event.replaceLayer(VanillaGuiLayers.FOOD_LEVEL, (gui, delta) -> {});
        event.replaceLayer(VanillaGuiLayers.ARMOR_LEVEL, (gui, delta) -> {});
        event.replaceLayer(VanillaGuiLayers.EXPERIENCE_BAR, (gui, delta) -> {});
        event.replaceLayer(VanillaGuiLayers.AIR_LEVEL, (gui, delta) -> {});
    }
}