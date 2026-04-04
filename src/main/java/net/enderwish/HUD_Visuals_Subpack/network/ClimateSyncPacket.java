package net.enderwish.HUD_Visuals_Subpack.network;

import net.enderwish.HUD_Visuals_Subpack.HUDVisualsSubpack;
import net.enderwish.HUD_Visuals_Subpack.api.ClimateData;
import net.enderwish.HUD_Visuals_Subpack.api.WeatherRegistry;
import net.enderwish.HUD_Visuals_Subpack.api.WeatherType;
import net.enderwish.HUD_Visuals_Subpack.client.ClientClimateCache;
import net.enderwish.HUD_Visuals_Subpack.client.ClientColorHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Modern 1.21.1 Custom Packet for syncing Climate to the Client.
 * Updated to use WeatherRegistry tags for reliable rendering.
 */
public record ClimateSyncPacket(ClimateData data) implements CustomPacketPayload {

    public static final Type<ClimateSyncPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(HUDVisualsSubpack.MOD_ID, "climate_sync")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ClimateSyncPacket> STREAM_CODEC = StreamCodec.composite(
            ClimateData.STREAM_CODEC,
            ClimateSyncPacket::data,
            ClimateSyncPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Handles the climate data on the client side.
     */
    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            // Safety check for client-side execution
            if (context.flow().isClientbound()) {
                ClimateData oldData = ClientClimateCache.get();
                ClimateData newData = this.data;

                // 1. Update the cache for the Sports Watch and Overlays
                ClientClimateCache.setInstance(newData);

                // 2. VANILLA WEATHER SYNC: Fixes the sky rendering
                ClientLevel level = Minecraft.getInstance().level;
                if (level != null) {
                    WeatherType type = WeatherRegistry.getById(newData.weather());
                    float intensity = newData.intensity();

                    // Check for Storm/Precipitation tags
                    if (WeatherRegistry.is(type, WeatherRegistry.IS_STORM)) {
                        level.setRainLevel(intensity);

                        // Check specifically for thunder to trigger lightning/darker skies
                        if (WeatherRegistry.is(type, WeatherRegistry.IS_THUNDER)) {
                            level.setThunderLevel(intensity);
                        } else {
                            level.setThunderLevel(0.0f);
                        }
                    } else {
                        // Clear sky for non-stormy weathers
                        level.setRainLevel(0.0f);
                        level.setThunderLevel(0.0f);
                    }
                }

                // 3. VISUAL REFRESH: Trigger chunk re-renders for Seasons/Thaw
                // thawStateChanged checks if the tempOffset flipped from negative (ice/snow) to zero (clear)
                boolean seasonChanged = oldData == null || oldData.season() != newData.season();
                boolean thawStateChanged = oldData != null && (oldData.tempOffset() < 0 != newData.tempOffset() < 0);

                if (seasonChanged || thawStateChanged) {
                    ClientColorHandler.refreshVisuals();
                }
            }
        });
    }
}