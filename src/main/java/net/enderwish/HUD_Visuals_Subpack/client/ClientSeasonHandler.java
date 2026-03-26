package net.enderwish.HUD_Visuals_Subpack.client;

import net.enderwish.HUD_Visuals_Subpack.core.Season;
import net.enderwish.HUD_Visuals_Subpack.network.SeasonSyncPacket;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Handles the logic when a SeasonSyncPacket is received on the client side.
 */
public class ClientSeasonHandler {

    private static Season clientSeason = Season.SPRING;

    /**
     * Called by the networking system.
     */
    public static void handleData(final SeasonSyncPacket data, final IPayloadContext context) {
        // Enqueue work to the main client thread to avoid race conditions
        context.enqueueWork(() -> {
            clientSeason = data.season();
            // Optional: You could trigger a level renderer refresh here if needed
            // Minecraft.getInstance().levelRenderer.allChanged();
        });
    }

    public static Season getClientSeason() {
        return clientSeason;
    }
}