package net.enderwish.Atmospheric_Overhaul_Subpack.network;

import net.enderwish.Atmospheric_Overhaul_Subpack.client.ClientSeasonState;
import net.enderwish.Atmospheric_Overhaul_Subpack.core.weather.WindDirection;
import net.enderwish.Atmospheric_Overhaul_Subpack.core.weather.WindState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * LocalPlayerEnvironmentSyncPacket
 *
 * Sent server -> ONE SPECIFIC PLAYER (not broadcast like SeasonSyncPacket/
 * WindSyncPacket) — felt wind and feels-like temperature are inherently
 * per-player, depending on that player's exact position and nearby geometry.
 *
 * Combines felt wind (direction + speed) and feels-like temperature into
 * ONE packet rather than two, since LocalPlayerEnvironmentHandler computes
 * both from the same per-player scan cycle.
 */
public record LocalPlayerEnvironmentSyncPacket(
        byte feltWindDirection,
        float feltWindSpeed,
        float feelsLikeTemp
) implements CustomPacketPayload {

    public static final Type<LocalPlayerEnvironmentSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    "gh_atmospheric", "local_player_environment_sync"));

    public static final StreamCodec<FriendlyByteBuf, LocalPlayerEnvironmentSyncPacket> CODEC =
            StreamCodec.composite(
                    net.minecraft.network.codec.ByteBufCodecs.BYTE,
                    LocalPlayerEnvironmentSyncPacket::feltWindDirection,
                    net.minecraft.network.codec.ByteBufCodecs.FLOAT,
                    LocalPlayerEnvironmentSyncPacket::feltWindSpeed,
                    net.minecraft.network.codec.ByteBufCodecs.FLOAT,
                    LocalPlayerEnvironmentSyncPacket::feelsLikeTemp,
                    LocalPlayerEnvironmentSyncPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void sendToPlayer(ServerPlayer player, WindDirection direction,
                                    float speed, float feelsLike) {
        PacketDistributor.sendToPlayer(player,
                new LocalPlayerEnvironmentSyncPacket((byte) direction.ordinal(), speed, feelsLike));
    }

    public static void handle(LocalPlayerEnvironmentSyncPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            WindDirection direction = WindState.directionFromByte(packet.feltWindDirection());
            ClientSeasonState.setLocalEnvironment(direction, packet.feltWindSpeed(), packet.feelsLikeTemp());
        });
    }
}
