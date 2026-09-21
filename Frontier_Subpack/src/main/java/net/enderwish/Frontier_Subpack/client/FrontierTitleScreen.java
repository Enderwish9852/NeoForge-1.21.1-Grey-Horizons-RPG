package net.enderwish.Frontier_Subpack.client;

import net.enderwish.Frontier_Subpack.FrontierConfig;
import net.enderwish.TerraForma_Subpack.api.WorldAPI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Random;

/**
 * FrontierTitleScreen
 *
 * World creation now goes through WorldAPI.createWorldDimensions(...)
 * instead of importing GHChunkGenerator/GHBiomeSource directly —
 * matches the project's own established rule that every cross-subpack
 * import goes through the corresponding API facade, never the
 * internals directly.
 */
public class FrontierTitleScreen extends Screen {

    public static final String ADVENTURE_WORLD_NAME = "GreyHorizonsAdventure";

    private final boolean adventureExists;

    public FrontierTitleScreen() {
        super(Component.literal("Grey Horizons"));
        this.adventureExists = adventureWorldExists();
    }

    private static boolean adventureWorldExists() {
        File savesDir = new File(Minecraft.getInstance().gameDirectory, "saves");
        File worldDir = new File(savesDir, ADVENTURE_WORLD_NAME);
        return worldDir.isDirectory();
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = this.height / 2 - (adventureExists ? 32 : 20);

        String primaryLabel = adventureExists ? "Continue Adventure" : "Start Adventure";

        this.addRenderableWidget(Button.builder(
                Component.literal(primaryLabel),
                button -> onStartOrContinueAdventure()
        ).bounds(centerX - 100, startY, 200, 20).build());

        int nextY = startY + 24;

        if (adventureExists) {
            this.addRenderableWidget(Button.builder(
                    Component.literal("Restart Adventure"),
                    button -> onRestartAdventure()
            ).bounds(centerX - 100, nextY, 200, 20).build());
            nextY += 24;
        }

        this.addRenderableWidget(Button.builder(
                Component.literal("Settings"),
                button -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(new FrontierSettingsScreen(this));
                    }
                }
        ).bounds(centerX - 100, nextY, 200, 20).build());
        nextY += 24;

        this.addRenderableWidget(Button.builder(
                Component.literal("Quit"),
                button -> {
                    if (this.minecraft != null) {
                        this.minecraft.stop();
                    }
                }
        ).bounds(centerX - 100, nextY, 200, 20).build());
    }

    private void onStartOrContinueAdventure() {
        Minecraft mc = this.minecraft;
        if (mc == null) return;

        WorldOpenFlows flows = new WorldOpenFlows(mc, mc.getLevelSource());

        if (adventureExists) {
            flows.openWorld(ADVENTURE_WORLD_NAME, () -> mc.setScreen(this));
            return;
        }

        boolean hardcore = FrontierConfig.HARDCORE_MODE.get();

        GameType gameType = hardcore ? GameType.SURVIVAL : GameType.CREATIVE;
        Difficulty difficulty = hardcore ? Difficulty.HARD : Difficulty.NORMAL;
        boolean allowCommands = !hardcore;

        LevelSettings levelSettings = new LevelSettings(
                ADVENTURE_WORLD_NAME,
                gameType,
                hardcore,
                difficulty,
                allowCommands,
                new GameRules(),
                WorldDataConfiguration.DEFAULT
        );

        WorldOptions worldOptions = new WorldOptions(
                new Random().nextLong(),
                true,
                false
        );

        flows.createFreshLevel(
                ADVENTURE_WORLD_NAME,
                levelSettings,
                worldOptions,
                registryAccess -> WorldAPI.createWorldDimensions(registryAccess),
                this
        );
    }

    private void onRestartAdventure() {
        Minecraft mc = this.minecraft;
        if (mc == null) return;

        mc.setScreen(new ConfirmScreen(
                confirmed -> {
                    if (confirmed) {
                        deleteAdventureWorld();
                        mc.setScreen(new FrontierTitleScreen());
                    } else {
                        mc.setScreen(this);
                    }
                },
                Component.literal("Restart Adventure?"),
                Component.literal("This will permanently delete your current Adventure world. This cannot be undone.")
        ));
    }

    private void deleteAdventureWorld() {
        File savesDir = new File(Minecraft.getInstance().gameDirectory, "saves");
        File worldDir = new File(savesDir, ADVENTURE_WORLD_NAME);
        deleteRecursively(worldDir.toPath());
    }

    private static void deleteRecursively(Path path) {
        if (!Files.exists(path)) return;
        try (var stream = Files.walk(path)) {
            stream.sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException ignored) {}
                    });
        } catch (IOException ignored) {}
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xFF1C1A17);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, "GREY HORIZONS",
                this.width / 2, this.height / 2 - 90, 0xFFC9B89A);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
