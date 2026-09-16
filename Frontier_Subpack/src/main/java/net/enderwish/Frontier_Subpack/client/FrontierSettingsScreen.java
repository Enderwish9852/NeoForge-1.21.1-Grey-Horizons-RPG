package net.enderwish.Frontier_Subpack.client;

import net.enderwish.Frontier_Subpack.FrontierConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;

/**
 * FrontierSettingsScreen
 *
 * Deliberately minimal for now — only the Adventure Mode toggle exists
 * as a real setting. Video/Sound/Controls routes through to vanilla's
 * actual OptionsScreen rather than us reimplementing those sliders.
 *
 * VERIFY IF COMPILE FAILS — FrontierConfig.ADVENTURE_MODE_ENABLED.set(...)
 * assumes NeoForge's ModConfigSpec.ConfigValue supports runtime mutation
 * this way from inside a screen. This is a different API surface than
 * just DEFINING a config value (which is all we'd done before this),
 * and I haven't verified the exact runtime-set/persist method against
 * your actual NeoForge sources.
 */
public class FrontierSettingsScreen extends Screen {

    private final Screen parent;

    public FrontierSettingsScreen(Screen parent) {
        super(Component.literal("Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = this.height / 2 - 20;

        boolean adventureEnabled = FrontierConfig.ADVENTURE_MODE_ENABLED.get();

        this.addRenderableWidget(Button.builder(
                Component.literal("Adventure Mode: " + (adventureEnabled ? "ON" : "OFF")),
                button -> {
                    FrontierConfig.ADVENTURE_MODE_ENABLED.set(!adventureEnabled);
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(new FrontierSettingsScreen(this.parent));
                    }
                }
        ).bounds(centerX - 100, startY, 200, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Video / Sound / Controls"),
                button -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(new OptionsScreen(this, this.minecraft.options));
                    }
                }
        ).bounds(centerX - 100, startY + 24, 200, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Back"),
                button -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(parent);
                    }
                }
        ).bounds(centerX - 100, startY + 48, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xFF1C1A17);
        graphics.drawCenteredString(this.font, "SETTINGS",
                this.width / 2, this.height / 2 - 60, 0xFFC9B89A);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
