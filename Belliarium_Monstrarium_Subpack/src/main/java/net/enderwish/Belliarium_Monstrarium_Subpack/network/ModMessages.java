package net.enderwish.Belliarium_Monstrarium_Subpack.network;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * ModMessages
 *
 * NEW -- playToServer registrations for the skill system's client->server
 * packets, and sendToServer() -- this project only needed playToClient
 * before now.
 */
public class ModMessages {
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(BelliariumMonstrariumSubpack.MODID).versioned("1.0");

        registrar.playToClient(BodyHealthSyncPacket.TYPE, BodyHealthSyncPacket.STREAM_CODEC, BodyHealthSyncPacket::handle);
        registrar.playToClient(SurvivalSyncPacket.TYPE, SurvivalSyncPacket.STREAM_CODEC, SurvivalSyncPacket::handle);
        registrar.playToClient(CombatTimerSyncPacket.TYPE, CombatTimerSyncPacket.STREAM_CODEC, CombatTimerSyncPacket::handle);
        registrar.playToClient(DeathReportPacket.TYPE, DeathReportPacket.STREAM_CODEC, DeathReportPacket::handle);
        registrar.playToClient(KillFeedPacket.TYPE, KillFeedPacket.STREAM_CODEC, KillFeedPacket::handle);
        registrar.playToClient(ExpLogPacket.TYPE, ExpLogPacket.STREAM_CODEC, ExpLogPacket::handle);
        registrar.playToClient(SkillLearningCompletePacket.TYPE, SkillLearningCompletePacket.STREAM_CODEC, SkillLearningCompletePacket::handle);
        registrar.playToClient(SkillBindResultPacket.TYPE, SkillBindResultPacket.STREAM_CODEC, SkillBindResultPacket::handle);

        registrar.playToServer(SkillBookScreenStatePacket.TYPE, SkillBookScreenStatePacket.STREAM_CODEC, SkillBookScreenStatePacket::handle);
        registrar.playToServer(SkillBindKeyPacket.TYPE, SkillBindKeyPacket.STREAM_CODEC, SkillBindKeyPacket::handle);
    }

    public static void sendToPlayer(CustomPacketPayload packet, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, packet);
    }

    /**
     * VERIFY IF COMPILE FAILS -- PacketDistributor.sendToServer(...) is the
     * standard NeoForge client->server send call, not confirmed against
     * source pasted in this conversation.
     */
    public static void sendToServer(CustomPacketPayload packet) {
        PacketDistributor.sendToServer(packet);
    }
}
