package net.enderwish.Belliarium_Monstrarium_Subpack;

import net.enderwish.Belliarium_Monstrarium_Subpack.client.HudRegistrationHandler;
import net.enderwish.Belliarium_Monstrarium_Subpack.command.SkillCommand;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.gear.ArmorToughnessRegistry;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.ModAttachments;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.ModDataComponents;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.skills.SkillRegistry;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.gear.ToolProfileRegistry;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.gear.WeightRegistry;
import net.enderwish.Belliarium_Monstrarium_Subpack.item.ModItems;
import net.enderwish.Belliarium_Monstrarium_Subpack.network.ModMessages;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod(BelliariumMonstrariumSubpack.MODID)
public class BelliariumMonstrariumSubpack {

    public static final String MODID = "gh_belliarium_monstrarium";

    public BelliariumMonstrariumSubpack(IEventBus modEventBus, ModContainer container) {
        ModAttachments.register(modEventBus);
        ModDataComponents.register(modEventBus);
        ModItems.register(modEventBus);

        modEventBus.addListener(this::registerNetworking);
        modEventBus.addListener(HudRegistrationHandler::onRegisterGuiLayers);

        NeoForge.EVENT_BUS.addListener((AddReloadListenerEvent e) -> {
            e.addListener(WeightRegistry.INSTANCE);
            e.addListener(ToolProfileRegistry.INSTANCE);
            e.addListener(ArmorToughnessRegistry.INSTANCE);
            e.addListener(SkillRegistry.INSTANCE);
        });

        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> SkillCommand.register(e.getDispatcher()));
    }

    private void registerNetworking(RegisterPayloadHandlersEvent event) {
        ModMessages.register(event);
    }
}
