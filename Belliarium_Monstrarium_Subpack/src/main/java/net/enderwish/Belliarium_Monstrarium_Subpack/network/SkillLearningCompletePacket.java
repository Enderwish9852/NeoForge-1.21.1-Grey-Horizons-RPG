package net.enderwish.Belliarium_Monstrarium_Subpack.network;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.client.SkillKeybindCaptureScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SkillLearningCompletePacket(String skillId) implements CustomPacketPayload {

    public static final Type<SkillLearningCompletePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BelliariumMonstrariumSubpack.MODID, "skill_learning_complete"));

    public static final StreamCodec<FriendlyByteBuf, SkillLearningCompletePacket> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> buf.writeUtf(p.skillId),
            buf -> new SkillLearningCompletePacket(buf.readUtf())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().player == null) return;
            Minecraft.getInstance().setScreen(new SkillKeybindCaptureScreen(skillId));
        });
    }
}
