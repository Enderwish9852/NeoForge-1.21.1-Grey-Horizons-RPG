package net.enderwish.Frontier_Subpack.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;

/**
 * FrontierTitleScreen
 *
 * PLACEHOLDER custom title screen — replaces vanilla's TitleScreen when
 * Adventure Mode is enabled. Structurally matches the mockup's button
 * layout (Start Adventure / Settings / Quit — no Singleplayer/Multiplayer)
 * but uses plain vanilla Button widgets for now. Custom art/styling comes
 * later once real assets exist.
 *
 * "Start Adventure" is currently a no-op stub — the instant world-gen /
 * continue-existing-Adventure-world flow hasn't been built yet.
 */
public class FrontierTitleScreen extends Screen {

    public FrontierTitleScreen() {
        super(Component.literal("Grey Horizons"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = this.height / 2 - 20;

        // TODO: check for an existing Adventure world and swap this label
        // to "Continue Adventure" — not wired up yet.
        this.addRenderableWidget(Button.builder(
                Component.literal("Start Adventure"),
                button -> {
                    // TODO: instant world-gen / continue-existing-save flow
                }
        ).bounds(centerX - 100, startY, 200, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Settings"),
                button -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(new OptionsScreen(this, this.minecraft.options));
                    }
                }
        ).bounds(centerX - 100, startY + 24, 200, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Quit"),
                button -> {
                    if (this.minecraft != null) {
                        this.minecraft.stop();
                    }
                }
        ).bounds(centerX - 100, startY + 48, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xFF1C1A17);
        graphics.drawCenteredString(this.font, "GREY HORIZONS",
                this.width / 2, this.height / 2 - 60, 0xFFC9B89A);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
