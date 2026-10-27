package net.enderwish.Belliarium_Monstrarium_Subpack.client;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.client.map.MapRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

/**
 * MapScreen
 *
 * Full-screen legendary map GUI. Phase 1 showed a placeholder; Phase 2 adds
 * the map renderer with zoom, explored / unexplored areas, and a locator
 * that glides from world spawn to the player position when the map opens.
 *
 * Controls:
 *   M or Escape - close
 *   Middle mouse wheel - zoom in / out
 *
 * The map is client-side only -- no container, no server menu.
 */
public class MapScreen extends Screen {

    private static final int PARCHMENT = 0xFFE6D8B4;
    private static final int BORDER = 0xFF6B5330;
    private static final int INK = 0xFF3A2A14;

    private final MapRenderer renderer = new MapRenderer();
    private int locatorX;
    private int locatorZ;
    private int targetX;
    private int targetZ;
    private float glideProgress = 0.0f;
    private boolean initialized = false;

    public MapScreen() {
        super(Component.literal("Legendary Map"));
    }

    @Override
    protected void init() {
        // No widgets yet -- Phase 3 adds the drawing toolbar.
    }

    @Override
    public void tick() {
        super.tick();

        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) return;

        if (!initialized) {
            // Start at world spawn, then glide to the player position.
            net.minecraft.core.BlockPos spawn = level.getSharedSpawnPos();
            renderer.setCenter(spawn.getX(), spawn.getZ());
            locatorX = spawn.getX();
            locatorZ = spawn.getZ();
            targetX = (int) mc.player.getX();
            targetZ = (int) mc.player.getZ();
            glideProgress = 0.0f;
            initialized = true;
        }

        // Glide the locator from spawn to the player over ~60 ticks.
        if (glideProgress < 1.0f) {
            glideProgress += 1.0f / 60.0f;
            if (glideProgress > 1.0f) glideProgress = 1.0f;
            float t = glideProgress;
            locatorX = (int) (locatorX + (targetX - locatorX) * t);
            locatorZ = (int) (locatorZ + (targetZ - locatorZ) * t);
            renderer.setCenter(locatorX, locatorZ);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Full-screen parchment background.
        graphics.fill(0, 0, this.width, this.height, PARCHMENT);
        graphics.fill(0, 0, this.width, 4, BORDER);
        graphics.fill(0, this.height - 4, this.width, 4, BORDER);
        graphics.fill(0, 0, 4, this.height, BORDER);
        graphics.fill(this.width - 4, 0, 4, this.height, BORDER);

        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level != null) {
            renderer.render(graphics, this.width, this.height, level);

            // Draw the locator dot at the player position.
            int lx = renderer.worldToScreenX((int) mc.player.getX());
            int lz = renderer.worldToScreenZ((int) mc.player.getZ());
            graphics.fill(lx - 2, lz - 2, lx + 2, lz + 2, 0xFFFF0000);
        }

        // Title and zoom hint.
        graphics.drawCenteredString(this.font, this.title.getString(),
                this.width / 2, 16, INK);
        graphics.drawCenteredString(this.font,
                "Zoom: " + renderer.getZoomLevel() + "  (middle mouse wheel)",
                this.width / 2, this.height - 20, INK);
        graphics.drawCenteredString(this.font,
                "Press M or Escape to close",
                this.width / 2, this.height - 8, 0xFF888888);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ModKeyMappings.OPEN_MAP.matches(keyCode, scanCode)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY < 0) {
            renderer.zoomIn();
        } else if (scrollY > 0) {
            renderer.zoomOut();
        }
        return true;
    }
}
