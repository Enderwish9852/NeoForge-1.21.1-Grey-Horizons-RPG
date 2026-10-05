package net.enderwish.Belliarium_Monstrarium_Subpack.network;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.client.DeathReportClientState;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.combat.CombatLogEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * DeathReportPacket
 *
 * Sent once, right as the deferred kill's real LivingDeathEvent goes through
 * (see BodyDamageHandler.sendDeathReport) -- carries everything the client-side
 * DeathReportScreen needs in one shot, since the screen itself has no server
 * access. combatDeath=false means "log" is empty and "headline" is already the
 * full simple cause-of-death line (fall/starve/etc.) -- the screen skips the
 * body diagram/scoreboard phase entirely in that case.
 */
public record DeathReportPacket(
        boolean combatDeath,
        String headline,
        float headPct, float torsoPct, float leftArmPct, float rightArmPct,
        float leftLegPct, float rightLegPct, float leftFootPct, float rightFootPct,
        List<CombatLogEntry> log
) implements CustomPacketPayload {

    public static final Type<DeathReportPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BelliariumMonstrariumSubpack.MODID, "death_report"));

    public static final StreamCodec<FriendlyByteBuf, DeathReportPacket> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBoolean(p.combatDeath);
                buf.writeUtf(p.headline);
                buf.writeFloat(p.headPct); buf.writeFloat(p.torsoPct);
                buf.writeFloat(p.leftArmPct); buf.writeFloat(p.rightArmPct);
                buf.writeFloat(p.leftLegPct); buf.writeFloat(p.rightLegPct);
                buf.writeFloat(p.leftFootPct); buf.writeFloat(p.rightFootPct);
                buf.writeVarInt(p.log.size());
                for (CombatLogEntry entry : p.log) {
                    buf.writeVarInt(entry.secondsIntoBattle());
                    buf.writeUtf(entry.sourceName());
                    buf.writeDouble(entry.distanceBlocks());
                    buf.writeFloat(entry.amount());
                    buf.writeUtf(entry.bodyPart());
                }
            },
            buf -> {
                boolean combatDeath = buf.readBoolean();
                String headline = buf.readUtf();
                float head = buf.readFloat(), torso = buf.readFloat();
                float lArm = buf.readFloat(), rArm = buf.readFloat();
                float lLeg = buf.readFloat(), rLeg = buf.readFloat();
                float lFoot = buf.readFloat(), rFoot = buf.readFloat();
                int count = buf.readVarInt();
                List<CombatLogEntry> log = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    int seconds = buf.readVarInt();
                    String source = buf.readUtf();
                    double distance = buf.readDouble();
                    float amount = buf.readFloat();
                    String bodyPart = buf.readUtf();
                    log.add(new CombatLogEntry(seconds, source, distance, amount, bodyPart));
                }
                return new DeathReportPacket(combatDeath, headline, head, torso, lArm, rArm, lLeg, rLeg, lFoot, rFoot, log);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().player == null) return;
            DeathReportClientState.setPending(this);
        });
    }
}
