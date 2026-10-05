package net.enderwish.Belliarium_Monstrarium_Subpack.client;

import net.enderwish.Belliarium_Monstrarium_Subpack.core.skills.KeyComboCodec;
import net.enderwish.Belliarium_Monstrarium_Subpack.network.ModMessages;
import net.enderwish.Belliarium_Monstrarium_Subpack.network.SkillBindKeyPacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * SkillKeybindCaptureScreen
 *
 * NEW -- compound binds (Ctrl+E etc.), per your spec. A modifier key held
 * alone doesn't finalize a bind -- it waits for a non-modifier key, then
 * packs whatever modifiers were held WITH it via KeyComboCodec.
 *
 * The vanilla-conflict check now only applies to PLAIN, no-modifier binds
 * -- vanilla's own KeyMapping has no concept of a compound bind to collide
 * with, so a Ctrl+E request bypasses that check entirely (this is also
 * exactly why compound binds solve your "most single keys are already
 * taken" problem).
 *
 * "Override" here never changes the player's actual control settings --
 * it just means this skill ALSO listens on that physical key, independent
 * of whatever vanilla action it already triggers.
 *
 * VERIFY IF COMPILE FAILS -- Options#keyMappings as the array of every
 * registered KeyMapping is the standard, long-stable field for this, not
 * confirmed against source pasted in this conversation.
 */
public class SkillKeybindCaptureScreen extends Screen {

    private final String skillId;

    public SkillKeybindCaptureScreen(String skillId) {
        super(Component.literal("Bind Skill"));
        this.skillId = skillId;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, "Learning complete! Press a key (or combo) to bind '" + skillId + "'.",
                this.width / 2, this.height / 2 - 10, 0xFFFFFFFF);
        graphics.drawCenteredString(this.font, "(Escape to bind later)",
                this.width / 2, this.height / 2 + 10, 0xFFAAAAAA);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }

        if (isModifierKey(keyCode)) return true; // wait for the real key

        boolean ctrl = Screen.hasControlDown();
        boolean shift = Screen.hasShiftDown();
        boolean alt = Screen.hasAltDown();
        int packed = KeyComboCodec.pack(keyCode, ctrl, shift, alt);

        if (!ctrl && !shift && !alt) {
            KeyMapping vanillaConflict = findVanillaConflict(keyCode);
            if (vanillaConflict != null) {
                Minecraft.getInstance().setScreen(new ConfirmScreen(
                        confirmed -> {
                            if (confirmed) sendBindRequest(packed);
                            else Minecraft.getInstance().setScreen(new SkillKeybindCaptureScreen(skillId));
                        },
                        Component.literal("Key Already Bound"),
                        Component.literal("This key is already used for '" + vanillaConflict.getName() + "'. Bind this skill to it anyway?")
                ));
                return true;
            }
        }

        sendBindRequest(packed);
        return true;
    }

    private static boolean isModifierKey(int keyCode) {
        return keyCode == GLFW.GLFW_KEY_LEFT_CONTROL || keyCode == GLFW.GLFW_KEY_RIGHT_CONTROL
                || keyCode == GLFW.GLFW_KEY_LEFT_SHIFT || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT
                || keyCode == GLFW.GLFW_KEY_LEFT_ALT || keyCode == GLFW.GLFW_KEY_RIGHT_ALT;
    }

    private KeyMapping findVanillaConflict(int keyCode) {
        for (KeyMapping mapping : Minecraft.getInstance().options.keyMappings) {
            if (mapping.getKey().getValue() == keyCode) return mapping;
        }
        return null;
    }

    private void sendBindRequest(int packedKey) {
        ModMessages.sendToServer(new SkillBindKeyPacket(skillId, packedKey, false));
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
