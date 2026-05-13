package net.enderwish.Atmospheric_Overhaul_Subpack.network;

import net.enderwish.Atmospheric_Overhaul_Subpack.AtmosphericOverhaulSubpack;
import net.enderwish.Atmospheric_Overhaul_Subpack.core.season.SeasonCalendar;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * SeasonSyncPacket
 *
 * Sent from server → client whenever the season, phase, weather,
 * or year changes. Stores the data in ClientSeasonState so client-side
 * code (HUD, BiomeMixin, FogMixin) always knows the current season.
 *
 * Registered in ModMessages.
 */
public record SeasonSyncPacket(
        int totalDays,
        int yearDay,
        SeasonCalendar.Season season,
        SeasonCalendar.Phase phase,
        String activeWeatherId,
        float activeIntensity,
        int year
) implements CustomPacketPayload {

    // ── Packet ID ─────────────────────────────────────────────────────────────

    public static final CustomPacketPayload.Type<SeasonSyncPacket> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(AtmosphericOverhaulSubpack.MOD_ID, "season_sync")
            );

    // ── Codec — how to encode/decode this packet over the network ─────────────

    public static final StreamCodec<FriendlyByteBuf, SeasonSyncPacket> STREAM_CODEC =
            StreamCodec.of(
                    SeasonSyncPacket::encode,
                    SeasonSyncPacket::decode
            );

    // ── Encode (server writes to buffer) ──────────────────────────────────────

    private static void encode(FriendlyByteBuf buf, SeasonSyncPacket packet) {
        buf.writeInt(packet.totalDays());
        buf.writeInt(packet.yearDay());
        buf.writeEnum(packet.season());
        buf.writeEnum(packet.phase());
        buf.writeUtf(packet.activeWeatherId());
        buf.writeFloat(packet.activeIntensity());
        buf.writeInt(packet.year());
    }

    // ── Decode (client reads from buffer) ─────────────────────────────────────

    private static SeasonSyncPacket decode(FriendlyByteBuf buf) {
        int totalDays        = buf.readInt();
        int yearDay          = buf.readInt();
        SeasonCalendar.Season season = buf.readEnum(SeasonCalendar.Season.class);
        SeasonCalendar.Phase  phase  = buf.readEnum(SeasonCalendar.Phase.class);
        String activeWeatherId       = buf.readUtf();
        float activeIntensity        = buf.readFloat();
        int year                     = buf.readInt();
        return new SeasonSyncPacket(totalDays, yearDay, season, phase,
                activeWeatherId, activeIntensity, year);
    }

    // ── Handle (runs on client after packet is received) ──────────────────────

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            // Store in ClientSeasonState — we build this next
            net.enderwish.Atmospheric_Overhaul_Subpack.client.ClientSeasonState.update(
                    totalDays,
                    yearDay,
                    season,
                    phase,
                    activeWeatherId,
                    activeIntensity,
                    year
            );
            Minecraft.getInstance().levelRenderer.allChanged();
        });
    }

    // ── Type ──────────────────────────────────────────────────────────────────

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}