package net.enderwish.HUD_Visuals_Subpack.network;

import net.enderwish.HUD_Visuals_Subpack.core.Season;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 1.21.1 Custom Packet Payload for syncing seasons.
 */
public record SeasonSyncPacket(Season season) implements CustomPacketPayload {

    public static final Type<SeasonSyncPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("hud_visuals", "season_sync"));

    // StreamCodec handles writing/reading from the network buffer
    public static final StreamCodec<FriendlyByteBuf, SeasonSyncPacket> STREAM_CODEC = StreamCodec.composite(
            Season.STREAM_CODEC,
            SeasonSyncPacket::season,
            SeasonSyncPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}