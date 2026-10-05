package net.enderwish.Belliarium_Monstrarium_Subpack.client;

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
 * "Override" for a vanilla conflict does NOT change the player's control
 * settings -- it just means this skill ALSO listens on that same physical
 * key, independent of whatever vanilla action it already triggers. That's
 * the normal, expected meaning of a mod sharing a key with vanilla.
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
        graphics.drawCenteredString(this.font, "Learning complete! Press a key to bind '" + skillId + "'.",
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

        KeyMapping vanillaConflict = findVanillaConflict(keyCode);
        if (vanillaConflict != null) {
            Minecraft.getInstance().setScreen(new ConfirmScreen(
                    confirmed -> {
                        if (confirmed) sendBindRequest(keyCode);
                        else Minecraft.getInstance().setScreen(new SkillKeybindCaptureScreen(skillId));
                    },
                    Component.literal("Key Already Bound"),
                    Component.literal("This key is already used for '" + vanillaConflict.getName() + "'. Bind this skill to it anyway?")
            ));
            return true;
        }

        sendBindRequest(keyCode);
        return true;
    }

    private KeyMapping findVanillaConflict(int keyCode) {
        for (KeyMapping mapping : Minecraft.getInstance().options.keyMappings) {
            if (mapping.getKey().getValue() == keyCode) return mapping;
        }
        return null;
    }

    private void sendBindRequest(int keyCode) {
        ModMessages.sendToServer(new SkillBindKeyPacket(skillId, keyCode, false));
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
