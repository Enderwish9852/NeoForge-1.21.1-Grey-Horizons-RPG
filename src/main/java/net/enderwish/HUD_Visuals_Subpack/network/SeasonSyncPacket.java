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
 * Syncs the current Season enum and the day count to the client.
 * Updated to correctly interface with ClientSeasonHandler for NeoForge 1.21.1.
 */
public record SeasonSyncPacket(Season season, int day) implements CustomPacketPayload {

    public static final Type<SeasonSyncPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("hud_visuals", "season_sync")
    );

    /**
     * The StreamCodec handles the serialization and deserialization of the packet.
     * Uses the custom Season.STREAM_CODEC for the enum and VAR_INT for the day.
     */
    public static final StreamCodec<FriendlyByteBuf, SeasonSyncPacket> STREAM_CODEC = StreamCodec.composite(
            Season.STREAM_CODEC, SeasonSyncPacket::season,
            ByteBufCodecs.VAR_INT, SeasonSyncPacket::day,
            SeasonSyncPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * This handles the packet logic when it arrives on the client side.
     * It updates the ClientSeasonHandler so the HUD can render the correct information.
     */
    public void handle(IPayloadContext context) {
        // Enqueue work ensures this runs on the main game thread
        context.enqueueWork(() -> {
            ClientSeasonHandler.setSeason(this.season);
            ClientSeasonHandler.setDay(this.day);
        });
    }
}