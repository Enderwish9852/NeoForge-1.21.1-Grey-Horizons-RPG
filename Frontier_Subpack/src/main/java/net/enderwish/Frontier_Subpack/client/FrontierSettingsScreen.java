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
 * ASSUMPTION worth flagging: clicking either Hardcore or Dev Mode's
 * button switches to that mode directly (one click), turning the other
 * off automatically and greying its button out. I went with this
 * "radio button"-style single-click switch since it's the far more
 * common pattern for exclusive toggle pairs in game settings menus —
 * if you actually wanted a two-step "must turn the active one off
 * before the other becomes clickable" flow instead, tell me and it's
 * a small change.
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
        int startY = this.height / 2 - 60;

        boolean adventureEnabled = FrontierConfig.ADVENTURE_MODE_ENABLED.get();
        boolean hardcore = FrontierConfig.HARDCORE_MODE.get();
        boolean devMode = FrontierConfig.DEV_MODE.get();

        this.addRenderableWidget(Button.builder(
                Component.literal("Adventure Mode: " + (adventureEnabled ? "ON" : "OFF")),
                button -> {
                    FrontierConfig.ADVENTURE_MODE_ENABLED.set(!adventureEnabled);
                    FrontierConfig.ADVENTURE_MODE_ENABLED.save();
                    refresh();
                }
        ).bounds(centerX - 100, startY, 200, 20).build());

        Button hardcoreButton = Button.builder(
                Component.literal("Hardcore Mode: " + (hardcore ? "ON" : "OFF")),
                button -> {
                    FrontierConfig.HARDCORE_MODE.set(true);
                    FrontierConfig.HARDCORE_MODE.save();
                    FrontierConfig.DEV_MODE.set(false);
                    FrontierConfig.DEV_MODE.save();
                    refresh();
                }
        ).bounds(centerX - 100, startY + 24, 200, 20).build();
        hardcoreButton.active = !hardcore;
        this.addRenderableWidget(hardcoreButton);

        Button devModeButton = Button.builder(
                Component.literal("Dev Mode: " + (devMode ? "ON" : "OFF")),
                button -> {
                    FrontierConfig.DEV_MODE.set(true);
                    FrontierConfig.DEV_MODE.save();
                    FrontierConfig.HARDCORE_MODE.set(false);
                    FrontierConfig.HARDCORE_MODE.save();
                    refresh();
                }
        ).bounds(centerX - 100, startY + 48, 200, 20).build();
        devModeButton.active = !devMode;
        this.addRenderableWidget(devModeButton);

        this.addRenderableWidget(Button.builder(
                Component.literal("Video / Sound / Controls"),
                button -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(new OptionsScreen(this, this.minecraft.options));
                    }
                }
        ).bounds(centerX - 100, startY + 80, 200, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Back"),
                button -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(parent);
                    }
                }
        ).bounds(centerX - 100, startY + 104, 200, 20).build());
    }

    private void refresh() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(new FrontierSettingsScreen(this.parent));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xFF1C1A17);
        graphics.drawCenteredString(this.font, "SETTINGS",
                this.width / 2, this.height / 2 - 90, 0xFFC9B89A);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
