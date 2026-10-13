package net.enderwish.Belliarium_Monstrarium_Subpack.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/** Black "Waking up..." screen shown while the server rewinds the world; fades out when the server says it's done. */
public class WakeUpScreen extends Screen {

    private static final long FADE_IN_MS = 600;
    private static final long FADE_OUT_MS = 1600;

    private final long openedAt = System.currentTimeMillis();
    private long finishedAt = -1;

    public WakeUpScreen() { super(Component.literal("Waking up")); }

    /** Called by DiaryWakeFinishedPacket. */
    public static void finish() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof WakeUpScreen screen) {
            screen.finishedAt = System.currentTimeMillis();
        } else {
            mc.setScreen(null);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() { return false; }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void tick() {
        if (finishedAt >= 0 && System.currentTimeMillis() - finishedAt >= FADE_OUT_MS && this.minecraft != null) {
            this.minecraft.setScreen(null);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        long now = System.currentTimeMillis();
        float alpha = finishedAt >= 0
                ? 1f - Mth.clamp((now - finishedAt) / (float) FADE_OUT_MS, 0f, 1f)
                : Mth.clamp((now - openedAt) / (float) FADE_IN_MS, 0f, 1f);

        graphics.fill(0, 0, this.width, this.height, ((int) (alpha * 255) << 24));

        if (finishedAt < 0 && now - openedAt > FADE_IN_MS) {
            int dots = (int) ((now / 450) % 4);
            String text = "Waking up" + ".".repeat(dots);
            graphics.drawCenteredString(this.font, text, this.width / 2, this.height / 2 - 4, 0xFFB0B0B0);
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
