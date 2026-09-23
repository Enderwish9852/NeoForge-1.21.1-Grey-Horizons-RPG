package net.enderwish.Frontier_Subpack.client;

import net.enderwish.Frontier_Subpack.FrontierConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;

/**
 * FrontierSettingsScreen
 *
 * NEW this round: "Exotic Start" toggle under World Creation, independent
 * of Hardcore/Dev Mode (no mutual exclusion -- a fresh Adventure world
 * can combine Exotic Start with either). This is ONLY the config
 * plumbing: it stores the preference correctly, but nothing reads it yet
 * to actually force the spawn point into Volcanic Lowlands. That needs
 * confirming whichever vanilla/NeoForge API controls initial world-spawn
 * selection first -- see this reply's questions. FrontierTitleScreen.
 * onStartOrContinueAdventure() is the natural place that wiring lands
 * once confirmed.
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
        int startY = this.height / 2 - 72;

        boolean adventureEnabled = FrontierConfig.ADVENTURE_MODE_ENABLED.get();
        boolean hardcore = FrontierConfig.HARDCORE_MODE.get();
        boolean devMode = FrontierConfig.DEV_MODE.get();
        boolean exoticStart = FrontierConfig.EXOTIC_START.get();

        this.addRenderableWidget(Button.builder(
                Component.literal("Adventure Mode: " + (adventureEnabled ? "ON" : "OFF")),
                button -> {
                    boolean newValue = !adventureEnabled;
                    FrontierConfig.ADVENTURE_MODE_ENABLED.set(newValue);
                    FrontierConfig.ADVENTURE_MODE_ENABLED.save();

                    if (this.minecraft != null) {
                        if (newValue) {
                            this.minecraft.setScreen(new FrontierTitleScreen());
                        } else {
                            this.minecraft.setScreen(new TitleScreen());
                        }
                    }
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
                Component.literal("Exotic Start: " + (exoticStart ? "ON" : "OFF")),
                button -> {
                    FrontierConfig.EXOTIC_START.set(!exoticStart);
                    FrontierConfig.EXOTIC_START.save();
                    refresh();
                }
        ).bounds(centerX - 100, startY + 72, 200, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Video / Sound / Controls"),
                button -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(new OptionsScreen(this, this.minecraft.options));
                    }
                }
        ).bounds(centerX - 100, startY + 104, 200, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Back"),
                button -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(parent);
                    }
                }
        ).bounds(centerX - 100, startY + 128, 200, 20).build());
    }

    private void refresh() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(new FrontierSettingsScreen(this.parent));
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xFF1C1A17);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, "SETTINGS",
                this.width / 2, this.height / 2 - 102, 0xFFC9B89A);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
