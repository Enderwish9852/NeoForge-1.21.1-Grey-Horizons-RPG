package net.enderwish.Belliarium_Monstrarium_Subpack.client;

import net.enderwish.Belliarium_Monstrarium_Subpack.core.ModAttachments;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.body.BodyHealthCapability;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.body.BodyPart;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.body.BodyPartTier;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.medical.TargetedMedItem;
import net.enderwish.Belliarium_Monstrarium_Subpack.network.MedApplyPacket;
import net.enderwish.Belliarium_Monstrarium_Subpack.network.ModMessages;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * MedTargetScreen
 *
 * Generalized from the Bandage-only version, now shared by Bandage/Suture
 * Kit/Splint. medStack is always supplied by the caller (never guessed by
 * the screen itself) -- the item's own use() passes its held stack
 * directly; OpenMedTargetFromKitPacket carries the kit-slot stack across
 * the network when triggered from inside the kit.
 *
 * Regions the held item CAN'T treat right now (wrong tier, or Splint
 * aimed at Head/Torso) render dimmed red and refuse the click, with a
 * tooltip explaining exactly why -- this replaces your original
 * "hovering shows suggested med" idea with something more direct: it
 * names the tier mismatch instead of guessing a suggestion.
 */
public class MedTargetScreen extends Screen {

    private final int handOrdinal;
    private final int kitSlotIndex;
    private final ItemStack medStack;

    public MedTargetScreen(int handOrdinal, int kitSlotIndex, ItemStack medStack) {
        super(Component.literal("Apply Treatment"));
        this.handOrdinal = handOrdinal;
        this.kitSlotIndex = kitSlotIndex;
        this.medStack = medStack;
    }

    private int originX() { return this.width / 2 - 20; }
    private int originY() { return this.height / 2 - 40; }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);

        int ox = originX(), oy = originY();
        BodyPart hovered = hitTest(mouseX, mouseY, ox, oy);

        for (BodyPart part : BodyPart.values()) {
            boolean valid = isValidTarget(part);
            boolean isHovered = part == hovered;
            int color = !valid ? 0x88440000 : (isHovered ? 0xFFFFFFFF : 0x88444444);
            int[] box = boxFor(ox, oy, part);
            graphics.fill(box[0], box[1], box[0] + box[2], box[1] + box[3], color);
        }

        if (hovered != null) {
            Component tooltip = isValidTarget(hovered)
                    ? Component.literal(hovered.getDisplayName())
                    : Component.literal(hovered.getDisplayName() + " -- " + reasonInvalid(hovered));
            graphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }

    private boolean isValidTarget(BodyPart part) {
        if (!(medStack.getItem() instanceof TargetedMedItem med)) return false;
        if (Minecraft.getInstance().player == null) return false;
        BodyHealthCapability cap = Minecraft.getInstance().player.getData(ModAttachments.BODY_HEALTH);
        return BodyPartTier.from(part.getPct(cap)) == med.treatsTier() && med.canTreat(part);
    }

    private String reasonInvalid(BodyPart part) {
        if (!(medStack.getItem() instanceof TargetedMedItem med)) return "not a treatment item";
        if (Minecraft.getInstance().player == null) return "";
        BodyHealthCapability cap = Minecraft.getInstance().player.getData(ModAttachments.BODY_HEALTH);
        if (!med.canTreat(part)) return "can't be used here";
        return switch (BodyPartTier.from(part.getPct(cap))) {
            case GREEN -> "doesn't need treatment";
            case YELLOW -> "needs a Bandage";
            case RED -> "needs a Suture Kit";
            case GREY -> "needs a Splint";
        };
    }

    private BodyPart hitTest(double mx, double my, int ox, int oy) {
        for (BodyPart part : BodyPart.values()) {
            int[] box = boxFor(ox, oy, part);
            if (mx >= box[0] && mx <= box[0] + box[2] && my >= box[1] && my <= box[1] + box[3]) return part;
        }
        return null;
    }

    /** {x, y, w, h} -- same body-diagram layout DeathReportScreen/BodyHealthHUD already use. */
    private int[] boxFor(int ox, int oy, BodyPart part) {
        return switch (part) {
            case HEAD       -> new int[]{ox + 12, oy, 10, 10};
            case TORSO      -> new int[]{ox + 8, oy + 11, 18, 22};
            case LEFT_ARM   -> new int[]{ox, oy + 11, 7, 20};
            case RIGHT_ARM  -> new int[]{ox + 27, oy + 11, 7, 20};
            case LEFT_LEG   -> new int[]{ox + 8, oy + 34, 8, 18};
            case RIGHT_LEG  -> new int[]{ox + 17, oy + 34, 8, 18};
            case LEFT_FOOT  -> new int[]{ox + 8, oy + 53, 8, 6};
            case RIGHT_FOOT -> new int[]{ox + 17, oy + 53, 8, 6};
        };
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        BodyPart hit = hitTest(mouseX, mouseY, originX(), originY());
        if (hit != null && isValidTarget(hit)) {
            ModMessages.sendToServer(new MedApplyPacket(handOrdinal, kitSlotIndex, hit.getDisplayName()));
            onClose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
