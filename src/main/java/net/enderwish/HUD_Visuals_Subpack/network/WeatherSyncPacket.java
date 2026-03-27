package net.enderwish.HUD_Visuals_Subpack.network;

import net.enderwish.HUD_Visuals_Subpack.client.ClientWeatherHandler;
import net.enderwish.HUD_Visuals_Subpack.core.WeatherData;
import net.enderwish.HUD_Visuals_Subpack.core.WeatherType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Packet to sync weather state from Server to Client.
 * Updated for NeoForge 1.21.1 with handling logic.
 */
public record WeatherSyncPacket(WeatherData data) implements CustomPacketPayload {
    public static final Type<WeatherSyncPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("hud_visuals", "weather_sync"));

    public static final StreamCodec<FriendlyByteBuf, WeatherSyncPacket> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> {
                buf.writeEnum(value.data().type());
                buf.writeInt(value.data().ticksRemaining());
                buf.writeFloat(value.data().intensity());
            },
            buf -> new WeatherSyncPacket(new WeatherData(
                    buf.readEnum(WeatherType.class),
                    buf.readInt(),
                    buf.readFloat()
            ))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * This is the logic that runs when the client receives the packet.
     */
    public static void handle(final WeatherSyncPacket payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            // Update the ClientWeatherHandler with the received data
            ClientWeatherHandler.setWeatherFromServer(
                    payload.data().type(),
                    payload.data().intensity()
            );
        });
    }
}
