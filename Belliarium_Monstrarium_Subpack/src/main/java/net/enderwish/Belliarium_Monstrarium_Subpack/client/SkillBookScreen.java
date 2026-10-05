package net.enderwish.Belliarium_Monstrarium_Subpack.client;

import net.enderwish.Belliarium_Monstrarium_Subpack.core.skills.SkillDefinition;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.skills.SkillRegistry;
import net.enderwish.Belliarium_Monstrarium_Subpack.network.ModMessages;
import net.enderwish.Belliarium_Monstrarium_Subpack.network.SkillBookScreenStatePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.InteractionHand;

import java.util.List;
import java.util.Optional;

public class SkillBookScreen extends Screen {

    private static final int HEARTBEAT_INTERVAL_TICKS = 20;

    private final String skillId;
    private final InteractionHand hand;
    private final Optional<SkillDefinition> definition;
    private int currentPage = 0;
    private int ticksOpen = 0;

    public SkillBookScreen(String skillId, InteractionHand hand) {
        super(Component.literal("Skill Book"));
        this.skillId = skillId;
        this.hand = hand;
        this.definition = SkillRegistry.INSTANCE.getSkill(skillId);
    }

    @Override
    protected void init() {
        sendHeartbeat();

        int pageCount = definition.map(d -> d.pages().size()).orElse(0);
        if (pageCount > 1) {
            this.addRenderableWidget(Button.builder(Component.literal("<"), b -> { if (currentPage > 0) currentPage--; })
                    .bounds(this.width / 2 - 100, this.height - 30, 20, 20).build());
            this.addRenderableWidget(Button.builder(Component.literal(">"), b -> { if (currentPage < pageCount - 1) currentPage++; })
                    .bounds(this.width / 2 + 80, this.height - 30, 20, 20).build());
        }

        this.addRenderableWidget(Button.builder(Component.literal("Close"), b -> onClose())
                .bounds(this.width / 2 - 50, this.height - 30, 100, 20).build());
    }

    @Override
    public void tick() {
        super.tick();
        ticksOpen++;
        if (ticksOpen % HEARTBEAT_INTERVAL_TICKS == 0) sendHeartbeat();
    }

    private void sendHeartbeat() {
        ModMessages.sendToServer(new SkillBookScreenStatePacket(true, skillId, hand.ordinal()));
    }

    @Override
    public void onClose() {
        ModMessages.sendToServer(new SkillBookScreenStatePacket(false, skillId, hand.ordinal()));
        super.onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);

        if (definition.isEmpty() || definition.get().pages().isEmpty()) {
            graphics.drawCenteredString(this.font, "This skill has no content yet.",
                    this.width / 2, this.height / 2, 0xFFFFFFFF);
            return;
        }

        List<String> pages = definition.get().pages();
        String page = pages.get(Math.min(currentPage, pages.size() - 1));

        List<FormattedCharSequence> lines = this.font.split(Component.literal(page), 200);
        int y = 40;
        for (FormattedCharSequence line : lines) {
            graphics.drawString(this.font, line, this.width / 2 - 100, y, 0xFFFFFFFF);
            y += this.font.lineHeight + 2;
        }

        graphics.drawCenteredString(this.font, "Page " + (currentPage + 1) + " / " + pages.size(),
                this.width / 2, this.height - 55, 0xFFAAAAAA);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
