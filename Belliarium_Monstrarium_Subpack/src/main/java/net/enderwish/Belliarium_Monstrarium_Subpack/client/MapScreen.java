package net.enderwish.Belliarium_Monstrarium_Subpack.client;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * MapScreen
 *
 * Full-screen legendary map GUI. Phase 1 shows a parchment background with a
 * title; Phase 2 adds the actual map rendering with zoom and explored /
 * unexplored areas; Phase 3 adds the right-hand drawing toolbar.
 *
 * The map is client-side only -- no container, no server menu.
 */
public class MapScreen extends Screen {

    private static final int PARCHMENT = 0xFFE6D8B4;
    private static final int BORDER = 0xFF6B5330;
    private static final int INK = 0xFF3A2A14;

    public MapScreen() {
        super(Component.literal("Legendary Map"));
    }

    @Override
    protected void init() {
        // No widgets yet -- Phase 3 adds the drawing toolbar.
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Full-screen parchment background.
        graphics.fill(0, 0, this.width, this.height, PARCHMENT);
        graphics.fill(0, 0, this.width, 4, BORDER);
        graphics.fill(0, this.height - 4, this.width, 4, BORDER);
        graphics.fill(0, 0, 4, this.height, BORDER);
        graphics.fill(this.width - 4, 0, 4, this.height, BORDER);

        // Title.
        graphics.drawCenteredString(this.font, this.title.getString(),
                this.width / 2, 16, INK);

        // Placeholder hint (replaced by map rendering in Phase 2).
        graphics.drawCenteredString(this.font,
                "Map rendering comes in Phase 2",
                this.width / 2, this.height / 2, INK);
        graphics.drawCenteredString(this.font,
                "Press M again or Escape to close",
                this.width / 2, this.height / 2 + 12, 0xFF888888);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // M again closes the map.
        if (ModKeyMappings.OPEN_MAP.matches(keyCode, scanCode)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
