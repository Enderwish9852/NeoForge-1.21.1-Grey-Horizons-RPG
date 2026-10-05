package net.enderwish.Belliarium_Monstrarium_Subpack.network;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.client.SkillKeybindCaptureScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SkillBindResultPacket(boolean success, boolean conflict, String conflictingSkillName,
                                    String skillId, int keyCode) implements CustomPacketPayload {

    public static final Type<SkillBindResultPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BelliariumMonstrariumSubpack.MODID, "skill_bind_result"));

    public static final StreamCodec<FriendlyByteBuf, SkillBindResultPacket> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBoolean(p.success);
                buf.writeBoolean(p.conflict);
                buf.writeUtf(p.conflictingSkillName == null ? "" : p.conflictingSkillName);
                buf.writeUtf(p.skillId);
                buf.writeVarInt(p.keyCode);
            },
            buf -> {
                boolean success = buf.readBoolean();
                boolean conflict = buf.readBoolean();
                String conflictName = buf.readUtf();
                String skillId = buf.readUtf();
                int keyCode = buf.readVarInt();
                return new SkillBindResultPacket(success, conflict, conflictName.isEmpty() ? null : conflictName, skillId, keyCode);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            if (success) {
                mc.setScreen(null);
                return;
            }

            if (conflict) {
                mc.setScreen(new ConfirmScreen(
                        confirmed -> {
                            if (confirmed) {
                                ModMessages.sendToServer(new SkillBindKeyPacket(skillId, keyCode, true));
                                mc.setScreen(null);
                            } else {
                                mc.setScreen(new SkillKeybindCaptureScreen(skillId));
                            }
                        },
                        Component.literal("Key Already In Use"),
                        Component.literal("This key is already bound to your '" + conflictingSkillName + "' skill. Override it?")
                ));
            }
        });
    }
}
