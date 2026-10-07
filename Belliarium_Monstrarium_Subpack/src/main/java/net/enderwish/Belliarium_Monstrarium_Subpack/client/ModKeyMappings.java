package net.enderwish.Belliarium_Monstrarium_Subpack.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/**
 * ModKeyMappings
 *
 * Default key is H ("heal") -- freely rebindable in vanilla's Controls
 * screen, change GLFW_KEY_H below if you want a different default.
 *
 * VERIFY IF COMPILE FAILS -- this exact KeyMapping constructor overload
 * (name, KeyConflictContext, InputConstants.Type, keyCode, category) is
 * my best-confidence read for 1.21.1, not confirmed against source pasted
 * in this conversation. The simpler 3-arg overload (name, keyCode,
 * category) is the fallback if this one doesn't compile.
 */
@EventBusSubscriber(modid = BelliariumMonstrariumSubpack.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModKeyMappings {

    public static final String CATEGORY = "key.categories." + BelliariumMonstrariumSubpack.MODID;

    public static final KeyMapping FAST_USE_FIRST_AID = new KeyMapping(
            "key." + BelliariumMonstrariumSubpack.MODID + ".fast_use_first_aid",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            CATEGORY
    );

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(FAST_USE_FIRST_AID);
    }
}
