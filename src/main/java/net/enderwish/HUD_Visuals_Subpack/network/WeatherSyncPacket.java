package net.enderwish.HUD_Visuals_Subpack.network;

import io.netty.buffer.ByteBuf;
import net.enderwish.HUD_Visuals_Subpack.HUDVisualsSubpack;
import net.enderwish.HUD_Visuals_Subpack.client.ClientWeatherHandler;
import net.enderwish.HUD_Visuals_Subpack.core.WeatherData;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Modern NeoForge 1.21.1 Packet for syncing weather.
 */
public record WeatherSyncPacket(WeatherData data) implements CustomPacketPayload {

    // Use your MOD_ID constant to prevent "magic string" errors
    public static final Type<WeatherSyncPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(HUDVisualsSubpack.MOD_ID, "weather_sync")
    );

    /**
     * Using ByteBuf instead of FriendlyByteBuf is the modern standard for StreamCodecs
     * unless you specifically need Vanilla-friendly buffer methods.
     */
    public static final StreamCodec<ByteBuf, WeatherSyncPacket> STREAM_CODEC = StreamCodec.composite(
            WeatherData.STREAM_CODEC, WeatherSyncPacket::data,
            WeatherSyncPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Payload handling logic.
     */
    public void handle(IPayloadContext context) {
        // EnqueueWork is CRITICAL to ensure we are back on the Main Client Thread
        // before touching things like Rendering or ClientHandlers.
        context.enqueueWork(() -> {
            ClientWeatherHandler.setWeatherFromServer(
                    data.type(),
                    data.ticksRemaining(),
                    data.intensity(),
                    data.tempOffset()
            );
        });
    }
}