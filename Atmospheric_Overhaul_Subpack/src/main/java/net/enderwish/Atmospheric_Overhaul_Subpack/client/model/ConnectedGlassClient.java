package net.enderwish.Atmospheric_Overhaul_Subpack.client.model;

import net.enderwish.Atmospheric_Overhaul_Subpack.AtmosphericOverhaulSubpack;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ConnectedGlassClient
 *
 * NEW -- swaps the baked minecraft:block/glass model for the connected
 * version after model baking. Both sprites (borderless face texture,
 * solid border-color edge strip) are pulled from the stitched block
 * atlas via the event's texture getter. The vanilla bordered texture
 * stays untouched for the item form and glass panes.
 */
@EventBusSubscriber(modid = AtmosphericOverhaulSubpack.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ConnectedGlassClient {

    private ConnectedGlassClient() {}

    private static final Logger LOGGER = LoggerFactory.getLogger("GHAtmospheric/ConnectedGlass");

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        TextureAtlasSprite borderless = ConnectedGlassModel.sprite(event, "block/glass_borderless");
        // the TRUE vanilla glass.png, extracted from the client jar -- the
        // border strips sample its opaque 1px ring => authentic vanilla
        // border color, not a hand-painted approximation
        TextureAtlasSprite edge = ConnectedGlassModel.sprite(event, "block/connected_glass_border");

        List<String> glassKeys = new ArrayList<>();
        for (Map.Entry<ModelResourceLocation, BakedModel> entry : event.getModels().entrySet()) {
            // toString() is "namespace:path#variant". The BLOCKSTATE model for
            // glass is "minecraft:block/glass#" (EMPTY variant). Other entries
            // can share the id part (e.g. an #inventory item-form variant), so
            // matching the id alone could wrap the wrong model first -- require
            // the empty variant explicitly and never break on the first hit.
            String s = entry.getKey().toString();
            int hash = s.indexOf('#');
            String id = hash < 0 ? s : s.substring(0, hash);
            if (id.contains("glass")) glassKeys.add(s);
            // Baked map keys are BLOCKSTATE ids: "minecraft:glass#" (empty
            // variant). The model FILE is block/glass, but no key is ever
            // "minecraft:block/glass" -- matching that string silently
            // wrapped nothing (confirmed via log: 0 wraps, minecraft:glass#
            // present in the baked map).
            if ("minecraft:glass".equals(id) && hash >= 0 && s.substring(hash + 1).isEmpty()) {
                entry.setValue(new ConnectedGlassModel(entry.getValue(), borderless, edge));
                LOGGER.info("Wrapped blockstate model '{}'", s);
            }
        }
        LOGGER.info("Baked models matching 'glass': {}", glassKeys);
    }
}
