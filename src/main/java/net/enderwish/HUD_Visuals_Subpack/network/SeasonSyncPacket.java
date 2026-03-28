package net.enderwish.HUD_Visuals_Subpack.network;

import net.enderwish.HUD_Visuals_Subpack.client.ClientSeasonHandler;
import net.enderwish.HUD_Visuals_Subpack.core.Season;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Syncs the current Season enum, day count, and weather status to the client.
 * Updated to fix the type mismatch: weather is now synced as a String.
 */
public record SeasonSyncPacket(Season season, int day, String weather) implements CustomPacketPayload {

    public static final Type<SeasonSyncPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("hud_visuals", "season_sync")
    );

    /**
     * The StreamCodec handles the serialization and deserialization of the packet.
     * Uses STRING_UTF8 for the weather to match the ClientSeasonHandler's expected type.
     */
    public static final StreamCodec<FriendlyByteBuf, SeasonSyncPacket> STREAM_CODEC = StreamCodec.composite(
            Season.STREAM_CODEC, SeasonSyncPacket::season,
            ByteBufCodecs.VAR_INT, SeasonSyncPacket::day,
            ByteBufCodecs.STRING_UTF8, SeasonSyncPacket::weather,
            SeasonSyncPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * This handles the packet logic when it arrives on the client side.
     */
    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientSeasonHandler.setSeason(this.season);
            ClientSeasonHandler.setDay(this.day);
            // Now matches the required String type in ClientSeasonHandler
            ClientSeasonHandler.setWeather(this.weather);
        });
    }
}