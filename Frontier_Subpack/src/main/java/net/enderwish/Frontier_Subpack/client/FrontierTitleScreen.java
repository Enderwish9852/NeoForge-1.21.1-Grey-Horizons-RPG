package net.enderwish.Frontier_Subpack.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import java.io.File;
import java.util.Random;

/**
 * FrontierTitleScreen
 *
 * Single-world design: exactly one "Adventure" save exists at a time.
 * Primary button dynamically reads "Start Adventure" (no save exists) or
 * "Continue Adventure" (one does), using the real WorldOpenFlows API
 * (createFreshLevel / openWorld) to bypass vanilla's CreateWorldScreen/
 * SelectWorldScreen entirely.
 *
 * SCOPING NOTE: TerraForma's custom chunk generator doesn't exist yet,
 * so "Start Adventure" currently generates a VANILLA-terrain world under
 * the fixed adventure name. Swap the dimensionGetter lambda below to
 * TerraForma's real generator once it exists — nothing else in this
 * flow needs to change.
 *
 * VERIFY IF COMPILE FAILS — these surrounding classes were NOT directly
 * confirmed against your decompiled sources (only WorldOpenFlows itself
 * was pasted and verified):
 *   - Minecraft.getLevelSource() — exact accessor name for LevelStorageSource
 *   - LevelSettings(...) constructor — exact parameter order/types
 *   - WorldOptions(...) constructor — exact parameter order/types
 *   - WorldDataConfiguration.DEFAULT — exact field name
 *   - WorldPresets.createNormalWorldDimensions(RegistryAccess) — exact
 *     method name/signature for getting vanilla's default dimension setup
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
        int startY = this.height / 2 - 20;

        String primaryLabel = adventureExists ? "Continue Adventure" : "Start Adventure";

        this.addRenderableWidget(Button.builder(
                Component.literal(primaryLabel),
                button -> onStartOrContinueAdventure()
        ).bounds(centerX - 100, startY, 200, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Settings"),
                button -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(new FrontierSettingsScreen(this));
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

    private void onStartOrContinueAdventure() {
        Minecraft mc = this.minecraft;
        if (mc == null) return;

        WorldOpenFlows flows = new WorldOpenFlows(mc, mc.getLevelSource());

        if (adventureExists) {
            flows.openWorld(ADVENTURE_WORLD_NAME, () -> mc.setScreen(this));
            return;
        }

        // DEFAULT: Survival, NOT hardcore. See question at end of response —
        // this is a real gameplay decision, not a technical detail, and I
        // don't want to silently bake in permadeath without your say-so.
        LevelSettings levelSettings = new LevelSettings(
                ADVENTURE_WORLD_NAME,
                GameType.SURVIVAL,
                false,
                Difficulty.NORMAL,
                false,
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
                registryAccess -> WorldPresets.createNormalWorldDimensions(registryAccess),
                this
        );
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
