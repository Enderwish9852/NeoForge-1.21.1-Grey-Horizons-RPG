package net.enderwish.HUD_Visuals_Subpack.network;

import net.enderwish.HUD_Visuals_Subpack.HUDVisualsSubpack;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModMessages {

    /**
     * Registers the networking channel and payloads.
     */
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(HUDVisualsSubpack.MOD_ID)
                .versioned("1.0");

        // Server -> Client (S2C)
        registrar.playToClient(
                net.enderwish.HUD_Visual_Subpack.network.LimbSyncPacket.TYPE,
                net.enderwish.HUD_Visual_Subpack.network.LimbSyncPacket.STREAM_CODEC,
                net.enderwish.HUD_Visual_Subpack.network.LimbSyncPacket::handle
        );


        registrar.playToClient(
                WristSyncPacket.TYPE,
                WristSyncPacket.STREAM_CODEC,
                WristSyncPacket::handle
        );

    }

    /**
     * Helper to send a packet to a specific player.
     * Uses the NeoForge static helper method to avoid resolution issues with nested PLAYER types.
     */
    public static void sendToPlayer(CustomPacketPayload packet, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, packet);
    }

    /**
     * Helper to send a packet to everyone.
     */
    public static void sendToAll(CustomPacketPayload packet) {
        PacketDistributor.sendToAllPlayers(packet);
    }
}