package net.enderwish.Belliarium_Monstrarium_Subpack.client;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.client.map.MapDrawTool;
import net.enderwish.Belliarium_Monstrarium_Subpack.client.map.MapRenderer;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.map.MapDraw;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.map.MapWorldData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;

/**
 * MapScreen
 *
 * Full-screen legendary map GUI. Phase 1 showed a placeholder; Phase 2 added
 * the map renderer with zoom, explored / unexplored areas, and a locator that
 * glides from world spawn to the player position when the map opens.
 *
 * Phase 3 adds the right-hand drawing toolbar: arrow, circle and text box
 * drawers. Click a tool icon to select it, click on the map to spawn a draw at
 * the cursor with default size, hold left click and drag to resize, then left
 * click again to apply. Drawings persist to world data.
 *
 * Controls:
 *   M or Escape - close
 *   Middle mouse wheel - zoom in / out
 *   Middle mouse drag - pan the map around
 *   Left click on toolbar - select a drawing tool
 *   Left click on map - place / apply a draw
 *   Left drag on map - resize the pending draw
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

    // ── Drawing state ─────────────────────────────────────────────────────

    private MapDrawTool selectedTool = MapDrawTool.NONE;
    private MapDraw pendingDraw = null; // draw being placed / resized

    private int toolbarX;
    private int toolbarY;
    private final int toolbarW = 44;
    private final int toolbarH = 100;

    // ── Panning state (middle mouse drag) ─────────────────────────────────

    private boolean panning = false;
    private int panLastX;
    private int panLastZ;

    public MapScreen() {
        super(Component.literal("Legendary Map"));
    }

    @Override
    protected void init() {
        // Right-hand toolbar buttons.
        int centerX = this.width / 2;
        int rightEdge = centerX + this.width / 2 - 20;
        toolbarX = rightEdge - toolbarW;
        toolbarY = 60;

        int bw = 32, bh = 32, gap = 8;
        int y = toolbarY;

        this.addRenderableWidget(Button.builder(Component.literal("▲"), b -> selectTool(MapDrawTool.ARROW))
                .bounds(toolbarX, y, bw, bh).build());
        y += bh + gap;

        this.addRenderableWidget(Button.builder(Component.literal("○"), b -> selectTool(MapDrawTool.CIRCLE))
                .bounds(toolbarX, y, bw, bh).build());
        y += bh + gap;

        this.addRenderableWidget(Button.builder(Component.literal("T"), b -> selectTool(MapDrawTool.TEXT))
                .bounds(toolbarX, y, bw, bh).build());
    }

    private void selectTool(MapDrawTool tool) {
        if (selectedTool == tool) {
            selectedTool = MapDrawTool.NONE; // toggle off
        } else {
            selectedTool = tool;
        }
        // Cancel any pending draw when switching tools.
        if (pendingDraw != null) {
            pendingDraw = null;
        }
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
        // Call super first so widgets render and the dark background overlay
        // gets applied; we then cover it with our parchment fill.
        super.render(graphics, mouseX, mouseY, partialTick);

        // Full-screen parchment background (covers the dark overlay from super.render()).
        graphics.fill(0, 0, this.width, this.height, PARCHMENT);
        graphics.fill(0, 0, this.width, 4, BORDER);
        graphics.fill(0, this.height - 4, this.width, 4, BORDER);
        graphics.fill(0, 0, 4, this.height, BORDER);
        graphics.fill(this.width - 4, 0, 4, this.height, BORDER);

        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level != null) {
            renderer.render(graphics, this.width, this.height, level);

            // Render all placed drawings.
            List<MapDraw> draws = MapWorldData.get(level).drawings();
            for (MapDraw draw : draws) {
                renderDraw(graphics, draw, false);
            }

            // Render the pending draw (being placed / resized).
            if (pendingDraw != null) {
                renderDraw(graphics, pendingDraw, true);
            }

            // Draw the locator dot at the player position.
            int lx = renderer.worldToScreenX((int) mc.player.getX());
            int lz = renderer.worldToScreenZ((int) mc.player.getZ());
            graphics.fill(lx - 2, lz - 2, lx + 2, lz + 2, 0xFFFF0000);
        }

        // Render the toolbar background.
        graphics.fill(toolbarX, toolbarY, toolbarX + toolbarW, toolbarY + toolbarH, 0xFF8B7D6B);
        graphics.fill(toolbarX + 1, toolbarY + 1, toolbarX + toolbarW - 1, toolbarY + toolbarH - 1, PARCHMENT);

        // Tool labels under each button.
        int buttonY = toolbarY;
        for (MapDrawTool tool : new MapDrawTool[]{MapDrawTool.ARROW, MapDrawTool.CIRCLE, MapDrawTool.TEXT}) {
            boolean active = selectedTool == tool;
            int btnColor = active ? tool.color : 0xFF5A4A3A;
            graphics.fill(toolbarX + 2, buttonY + 2, toolbarX + 30, buttonY + 30, btnColor);
            if (active) {
                graphics.fill(toolbarX + 2, buttonY + 2, toolbarX + 4, buttonY + 30, 0xFFFFFFFF); // active indicator
            }
            graphics.drawString(this.font, tool.label, toolbarX + 2, buttonY + 34, INK, false);
            buttonY += 40;
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

        // Pending draw hint.
        if (selectedTool != MapDrawTool.NONE && pendingDraw == null) {
            graphics.drawCenteredString(this.font,
                    "Click on the map to place " + selectedTool.label,
                    this.width / 2, this.height - 40, 0xFF888888);
        } else if (pendingDraw != null) {
            graphics.drawCenteredString(this.font,
                    "Drag to resize, click to apply, right-click to cancel",
                    this.width / 2, this.height - 40, 0xFF888888);
        }
    }

    private void renderDraw(GuiGraphics graphics, MapDraw draw, boolean pending) {
        int sx = renderer.worldToScreenX(draw.centerX());
        int sz = renderer.worldToScreenZ(draw.centerZ());
        int r = Math.max(2, (int) (draw.radius() * renderer.visibleRange() / 1000.0f));
        int alpha = pending ? 0x7F : 0xFF;
        int color = (draw.color() & 0xFFFFFF) | (alpha << 24);

        switch (draw.type()) {
            case ARROW -> {
                // Draw a simple arrow pointing up.
                graphics.fill(sx - 1, sz - r, sx + 1, sz + r, color);
                graphics.fill(sx - r, sz - r, sx + r, sz - r + 2, color); // arrowhead base
                graphics.fill(sx - 2, sz - r - 2, sx + 2, sz - r + 2, color); // arrowhead tip
            }
            case CIRCLE -> {
                // Draw a circle outline.
                for (int i = 0; i < 360; i += 10) {
                    double rad = Math.toRadians(i);
                    int x = sx + (int) (Math.cos(rad) * r);
                    int z = sz + (int) (Math.sin(rad) * r);
                    graphics.fill(x, z, x + 1, z + 1, color);
                }
            }
            case TEXT -> {
                graphics.drawString(this.font, draw.text() == null ? "?" : draw.text(),
                        sx - r, sz - r, draw.color(), false);
            }
        }
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
        if (keyCode == 256) { // Escape
            if (pendingDraw != null) {
                pendingDraw = null;
                return true;
            }
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null || button != 0) return super.mouseClicked(mouseX, mouseY, button);

        // Check toolbar clicks first.
        if (mouseX >= toolbarX && mouseX <= toolbarX + toolbarW
                && mouseY >= toolbarY && mouseY <= toolbarY + toolbarH) {
            int relY = (int) (mouseY - toolbarY);
            if (relY < 32) {
                selectTool(MapDrawTool.ARROW);
                return true;
            } else if (relY < 32 + 8 + 32) {
                selectTool(MapDrawTool.CIRCLE);
                return true;
            } else if (relY < 32 + 8 + 32 + 8 + 32) {
                selectTool(MapDrawTool.TEXT);
                return true;
            }
        }

        // Map clicks for drawing placement.
        if (selectedTool != MapDrawTool.NONE) {
            int worldX = renderer.screenToWorldX((int) mouseX);
            int worldZ = renderer.screenToWorldZ((int) mouseY);

            if (pendingDraw == null) {
                // Spawn a new draw at cursor.
                switch (selectedTool) {
                    case ARROW -> pendingDraw = MapDraw.arrow(worldX, worldZ, 20.0f);
                    case CIRCLE -> pendingDraw = MapDraw.circle(worldX, worldZ, 20.0f);
                    case TEXT -> pendingDraw = MapDraw.text(worldX, worldZ, 20.0f, null);
                }
                return true;
            } else {
                // Apply the pending draw.
                MapWorldData.get(level).addDrawing(pendingDraw);
                pendingDraw = null;
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        // End middle-mouse drag panning.
        if (button == 2 && panning) {
            panning = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        // Middle mouse drag pans the map.
        if (button == 2) {
            if (!panning) {
                panning = true;
                panLastX = renderer.screenToWorldX((int) mouseX);
                panLastZ = renderer.screenToWorldZ((int) mouseY);
            }
            int worldX = renderer.screenToWorldX((int) mouseX);
            int worldZ = renderer.screenToWorldZ((int) mouseY);
            int dx = panLastX - worldX;
            int dz = panLastZ - worldZ;
            renderer.pan(dx, dz);
            panLastX = worldX;
            panLastZ = worldZ;
            return true;
        }

        if (button != 0 || pendingDraw == null) return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);

        // Resize the pending draw based on drag distance.
        float dragDist = (float) Math.sqrt(dragX * dragX + dragY * dragY);
        float newRadius = Math.max(5.0f, pendingDraw.radius() + dragDist * 0.5f);
        pendingDraw = new MapDraw(
                pendingDraw.id(), pendingDraw.type(),
                pendingDraw.centerX(), pendingDraw.centerZ(),
                newRadius, pendingDraw.text(), pendingDraw.color()
        );
        return true;
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

    /** Save pending draw on close if one is being placed. */
    @Override
    public void onClose() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && pendingDraw != null) {
            // Auto-apply pending draw on close.
            MapWorldData.get(mc.level).addDrawing(pendingDraw);
        }
        super.onClose();
    }
}
