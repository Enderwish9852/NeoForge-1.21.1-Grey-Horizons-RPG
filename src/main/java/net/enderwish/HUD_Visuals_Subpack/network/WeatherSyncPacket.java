package net.enderwish.HUD_Visuals_Subpack.network;

import net.enderwish.HUD_Visuals_Subpack.client.ClientWeatherHandler;
import net.enderwish.HUD_Visuals_Subpack.core.WeatherData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Packet to sync weather state from Server to Client.
 * Leverages the updated WeatherData record which now includes tempOffset.
 */
public record WeatherSyncPacket(WeatherData data) implements CustomPacketPayload {

    public static final Type<WeatherSyncPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("hud_visuals", "weather_sync")
    );

    /**
     * Efficiently handles serialization by leveraging the existing
     * WeatherData.STREAM_CODEC (which now handles type, ticks, intensity, and tempOffset).
     */
    public static final StreamCodec<FriendlyByteBuf, WeatherSyncPacket> STREAM_CODEC = StreamCodec.composite(
            WeatherData.STREAM_CODEC.cast(), WeatherSyncPacket::data,
            WeatherSyncPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Logic executed on the client-side upon receiving the packet.
     * Updated to pass all components including duration and temperature offset.
     */
    public static void handle(final WeatherSyncPacket payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            WeatherData weather = payload.data();

            // Update the ClientWeatherHandler with the full set of received data
            ClientWeatherHandler.setWeatherFromServer(
                    weather.type(),
                    weather.ticksRemaining(),
                    weather.intensity(),
                    weather.tempOffset()
            );
        });
    }
}