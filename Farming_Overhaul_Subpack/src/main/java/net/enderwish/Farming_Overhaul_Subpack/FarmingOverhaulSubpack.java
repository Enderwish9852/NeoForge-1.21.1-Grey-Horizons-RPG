package net.enderwish.Farming_Overhaul_Subpack;

import com.mojang.logging.LogUtils;
import net.enderwish.Farming_Overhaul_Subpack.event.SeasonGrowthListener;
import net.enderwish.HUD_Visuals_Subpack.item.ModItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * Main class for the Farming Overhaul Subpack.
 * Developed for the Grey Horizons RPG.
 */
@Mod(FarmingOverhaulSubpack.MODID)
public class FarmingOverhaulSubpack {
    // This ID must match your mods.toml
    public static final String MODID = "gh_farming_overhaul";
    private static final Logger LOGGER = LogUtils.getLogger();

    public FarmingOverhaulSubpack(IEventBus modEventBus) {
        LOGGER.info("GH-FARMING: Initializing Farming Overhaul Subpack...");

        // 1. Register Hardcore Items (Fruits/Veg/Grains)
        ModItems.ITEMS.register(modEventBus);

        // 2. Register the Season/Growth Hooks [cite: 12]
        // This connects to the HUD_Visuals_Subpack climate data
        NeoForge.EVENT_BUS.register(SeasonGrowthListener.class);
    }
}