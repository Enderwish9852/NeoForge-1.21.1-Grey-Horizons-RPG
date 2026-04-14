package net.enderwish.HUD_Visuals_Subpack;

import net.enderwish.HUD_Visuals_Subpack.client.gui.SportsWatchHUD;
import net.enderwish.HUD_Visuals_Subpack.client.render.WeatherOverlayRenderer;
import net.enderwish.HUD_Visuals_Subpack.command.SeasonCommand;
import net.enderwish.HUD_Visuals_Subpack.command.WeatherCommand;
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
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

@Mod(HUDVisualsSubpack.MOD_ID)
public class HUDVisualsSubpack {
    public static final String MOD_ID = "gh_hud_visuals";

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    public static final DeferredItem<Item> SPORTS_WATCH = ITEMS.register("sports_watch",
            () -> new SportsWatchItem(new Item.Properties().stacksTo(1)));

    public static final Supplier<CreativeModeTab> GREY_HORIZONS_TAB = CREATIVE_TABS.register("hud_visuals_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("creativetab.hud_visuals"))
                    .icon(() -> new ItemStack(SPORTS_WATCH.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(SPORTS_WATCH.get());
                    })
                    .build());

    public HUDVisualsSubpack(IEventBus modEventBus) {
        // Register Deferred Registers
        ITEMS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
        ModAttachments.register(modEventBus);

        // Mod Bus Listeners (Lifecycle & Setup)
        modEventBus.addListener(this::registerNetworking);
        modEventBus.addListener(this::onRegisterGuiLayers);

        // Global Bus Listeners (Gameplay & Commands)
        NeoForge.EVENT_BUS.register(new HealthRegenEvents());
        NeoForge.EVENT_BUS.register(LimbDamageEventHandler.class);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
    }

    private void registerNetworking(final RegisterPayloadHandlersEvent event) {
        ModMessages.register(event);
    }

    /**
     * Commands are registered on the NeoForge EVENT_BUS.
     */
    private void onRegisterCommands(RegisterCommandsEvent event) {
        WeatherCommand.register(event.getDispatcher());
        SeasonCommand.register(event.getDispatcher());
    }

    /**
     * GUI Layers are registered on the MOD EVENT_BUS.
     */
    private void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        // 1. Sports Watch Layer
        event.registerAbove(
                VanillaGuiLayers.HOTBAR,
                ResourceLocation.fromNamespaceAndPath(MOD_ID, "sports_watch"),
                (graphics, delta) -> SportsWatchHUD.SPORTS_WATCH_ELEMENT.render(graphics, delta)
        );

        // 2. Weather Full-Screen Overlay Layer
        event.registerAbove(
                VanillaGuiLayers.CAMERA_OVERLAYS,
                ResourceLocation.fromNamespaceAndPath(MOD_ID, "weather_screen_overlay"),
                (graphics, delta) -> WeatherOverlayRenderer.render(graphics, delta)
        );

        // Vanilla UI Overrides
        // Note: Using replaceLayer with a null/empty consumer hides them
        event.replaceLayer(VanillaGuiLayers.PLAYER_HEALTH, (graphics, delta) -> {});
        event.replaceLayer(VanillaGuiLayers.FOOD_LEVEL, (graphics, delta) -> {});
        event.replaceLayer(VanillaGuiLayers.ARMOR_LEVEL, (graphics, delta) -> {});
        event.replaceLayer(VanillaGuiLayers.EXPERIENCE_BAR, (graphics, delta) -> {});
        event.replaceLayer(VanillaGuiLayers.AIR_LEVEL, (graphics, delta) -> {});
    }
}